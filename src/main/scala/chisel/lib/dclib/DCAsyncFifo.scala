package chisel.lib.dclib

import chisel3._
import chisel3.util._

/**
  * Asynchronous FIFO with Decoupled interfaces
  *
  * Implements a clock-domain crossing FIFO with synchronized pointers.  The FIFO must be a natural power of 2
  * in depth, and should be at least 8 words deep for full performance.  Internally uses a 1-bit larger pointer
  * value to track empty vs. full.
  *
  * @param data       Data type for FIFO
  * @param depth      Depth of FIFO, must be power of 2
  * @param doubleSync Generator function to create a double-synchronized version of input
  */
class DCAsyncFifo[D <: Data](data: D, depth: Int, doubleSync: (UInt) => UInt = defaultDoubleSync) extends RawModule {
  val io = IO(new Bundle {
    val enq_clock = Input(Clock())
    val enq_reset = Input(Reset())
    val deq_clock = Input(Clock())
    val deq_reset = Input(Reset())
    val enq = Flipped(new DecoupledIO(data.cloneType))
    val deq = new DecoupledIO(data.cloneType)
  })
  // Async FIFO must be power of two for pointer sync to work correctly
  val asz = log2Ceil(depth)
  require(depth == 1 << asz)

  val mem = withClockAndReset(io.enq_clock, io.enq_reset) {
    Reg(Vec(depth, data))
  }
  val wrptr_enq = withClockAndReset(io.enq_clock, io.enq_reset) {
    RegInit(init = 0.U((asz + 1).W))
  }
  val wrptr_grey_enq = bin2grey(wrptr_enq)
  val wrptr_grey_deq = withClockAndReset(io.deq_clock, io.deq_reset) {
    doubleSync(wrptr_grey_enq)
  }
  val rdptr_deq = withClockAndReset(io.deq_clock, io.deq_reset) {
    RegInit(init = 0.U((asz + 1).W))
  }
  val rdptr_grey_deq = bin2grey(rdptr_deq)
  val rdptr_grey_enq = withClockAndReset(io.enq_clock, io.enq_reset) {
    doubleSync(rdptr_grey_deq)
  }

  val full_enq = wrptr_grey_enq(asz - 1, 0) === rdptr_grey_enq(asz - 1, 0) & (wrptr_grey_enq(asz) =/= rdptr_grey_enq(asz))
  val empty_deq = wrptr_grey_deq === rdptr_grey_deq
  io.enq.ready := !full_enq

  when(io.enq.fire) {
    wrptr_enq := wrptr_enq + 1.U
    withClockAndReset(io.enq_clock, io.enq_reset) {
      mem(wrptr_enq(asz - 1, 0)) := io.enq.bits
    }
  }

  io.deq.valid := !empty_deq
  when(io.deq.fire) {
    rdptr_deq := rdptr_deq + 1.U
  }
  io.deq.bits := mem(rdptr_deq(asz - 1, 0))
}

