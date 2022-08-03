package chisel.lib.dclib

import chisel3._
import chisel3.util._

/**
  * Closes output timing on an input of type D
  * deq.valid and deq.bits will be registered, enq.ready will be combinatorial
  */
class DCOutput[D <: Data](data: D) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(new DecoupledIO(data.cloneType))
    val deq = new DecoupledIO(data.cloneType)
  })
  override def desiredName: String = "DCOutput_" + data.toString

  val r_valid = RegInit(false.B)

  io.enq.ready := io.deq.ready || !r_valid
  r_valid := io.enq.fire || (r_valid && !io.deq.ready)
  io.deq.bits := RegEnable(next=io.enq.bits, enable=io.enq.fire)
  io.deq.valid := r_valid
}

// Helper function for functional inference
object DCOutput {
  def apply[D <: Data](x : DecoupledIO[D]) : DecoupledIO[D] = {
    val tout = Module(new DCOutput(x.bits.cloneType))
    tout.io.enq <> x
    tout.io.deq
  }
}
