// Author: Martin Schoeberl (martin@jopdesign.com)
// License: this code is released into the public domain, see README.md and http://unlicense.org/

package chisel.lib.fifo

import chisel3._
import chisel3.util._

/**
  * FIFO with read and write pointer using dedicated registers as memory.
  */
class RegFifo[T <: Data](gen: T, depth: Int) extends Fifo(gen: T, depth: Int) {

  def counter(depth: Int, incr: Bool): (UInt, UInt) = {
    val cntReg = RegInit(0.U(log2Ceil(depth).W))
    val nextVal = Mux(cntReg === (depth - 1).U, 0.U, cntReg + 1.U)
    when(incr) {
      cntReg := nextVal
    }
    (cntReg, nextVal)
  }

  // the register based memory
  val memReg = Reg(Vec(depth, gen))

  val incrRead = WireDefault(false.B)
  val incrWrite = WireDefault(false.B)
  val (readPtr, nextRead) = counter(depth, incrRead)
  val (writePtr, nextWrite) = counter(depth, incrWrite)

  val emptyReg = RegInit(true.B)
  val fullReg = RegInit(false.B)

  val op = io.enq.valid ## io.deq.ready

  switch (op) {
    is ("b00".U) { }
    is ("b01".U) { // read
      when (!emptyReg) {
        fullReg := false.B
        emptyReg := nextRead === writePtr
        incrRead := true.B
      }
    }
    is ("b10".U) { // write
      when (!fullReg) {
        memReg(writePtr) := io.enq.bits
        emptyReg := false.B
        fullReg := nextWrite === readPtr
        incrWrite := true.B
      }
    }
    is ("b11".U) { // write and read
      when (!fullReg) {
        memReg(writePtr) := io.enq.bits
        emptyReg := false.B
        when (emptyReg) {
          fullReg := nextWrite === readPtr
        } .otherwise {
          fullReg := nextWrite === nextRead
        }
        incrWrite := true.B
      }
      when (!emptyReg) {
        fullReg := false.B
        when (fullReg) {
          emptyReg := nextRead === writePtr
        } .otherwise {
          emptyReg := nextRead === nextWrite
        }
        incrRead := true.B
      }
    }
  }

  io.deq.bits := memReg(readPtr)
  io.enq.ready := !fullReg
  io.deq.valid := !emptyReg

  // some assertions
  val fullNr = Mux(fullReg, depth.U, 0.U)
  val number = writePtr - readPtr + fullNr
  assert(number >= 0.U)
  assert(number < (depth+1).U)

  assert(!(emptyReg && fullReg))

  when(readPtr =/= writePtr) {
    assert(emptyReg === false.B)
    assert(fullReg === false.B)
  }

  when (fullReg) {
    assert(readPtr === writePtr)
  }

  when (emptyReg) {
    assert(readPtr === writePtr)
  }
}
