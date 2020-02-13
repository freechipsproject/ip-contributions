package chisel.lib.dclib
import chisel3._
import chisel3.util._

/**
  * Provides timing closure on valid, ready and bits interfaces by
  * using DCInput and DCOutput back to back.  Effectively a 2-entry
  * FIFO.
  */
object DCFull {
  def apply[D <: Data](x : DecoupledIO[D]) : DecoupledIO[D] = {
    val tin = Module(new DCInput(x.bits.cloneType))
    val tout = Module(new DCOutput(x.bits.cloneType))
    tin.io.enq <> x
    tin.io.deq <> tout.io.enq
    tout.io.deq
  }
}
