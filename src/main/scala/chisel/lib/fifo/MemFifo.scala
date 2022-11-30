// Author: Martin Schoeberl (martin@jopdesign.com)
// License: this code is released into the public domain, see README.md and http://unlicense.org/

package chisel.lib.fifo

import chisel3._
import chisel3.util._

/**
  * FIFO with memory and read and write pointers.
  * Extra shadow register to handle the one cycle latency of the synchronous memory.
  */
class MemFifo[T <: Data](gen: T, depth: Int) extends Fifo(gen: T, depth: Int) {

  def counter(depth: Int, incr: Bool): (UInt, UInt) = {
    val cntReg = RegInit(0.U(log2Ceil(depth).W))
    val nextVal = Mux(cntReg === (depth - 1).U, 0.U, cntReg + 1.U)
    when(incr) {
      cntReg := nextVal
    }
    (cntReg, nextVal)
  }

  val mem = SyncReadMem(depth, gen, SyncReadMem.WriteFirst)

  val incrRead = WireInit(false.B)
  val incrWrite = WireInit(false.B)
  val (readPtr, nextRead) = counter(depth, incrRead)
  val (writePtr, nextWrite) = counter(depth, incrWrite)

  val emptyReg = RegInit(true.B)
  val fullReg = RegInit(false.B)

  val outputReg = Reg(gen)
  val outputValidReg = RegInit(false.B)
  val read = WireDefault(false.B)

  io.deq.valid := outputValidReg
  io.enq.ready := !fullReg

  val doWrite = WireDefault(false.B)
  val data = Wire(gen)
  data := mem.read(readPtr)
  io.deq.bits := data
  when(doWrite) {
    mem.write(writePtr, io.enq.bits)
  }

  val readCond =
    !outputValidReg && ((readPtr =/= writePtr) || fullReg) // should add optimization when downstream is ready for pipielining
  when(readCond) {
    read := true.B
    incrRead := true.B
    outputReg := data
    outputValidReg := true.B
    emptyReg := nextRead === writePtr
    fullReg := false.B // no concurrent read when full (at the moment)
  }
  when(io.deq.fire) {
    outputValidReg := false.B
  }
  io.deq.bits := outputReg

  when(io.enq.fire) {
    emptyReg := false.B
    fullReg := (nextWrite === readPtr) & !read
    incrWrite := true.B
    doWrite := true.B
  }

  // some assertions
  val fullNr = Mux(fullReg, depth.U, 0.U)
  val number = writePtr - readPtr + fullNr
  assert(number >= 0.U)
  assert(number < (depth + 1).U)

  assert(!(emptyReg && fullReg))

  when(readPtr =/= writePtr) {
    assert(emptyReg === false.B)
    assert(fullReg === false.B)
  }

  when(fullReg) {
    assert(readPtr === writePtr)
  }

  when(emptyReg) {
    assert(readPtr === writePtr)
  }
}
