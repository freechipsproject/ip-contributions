package chisel.lib.dclib

import chisel3._
import chisel3.util.DecoupledIO

/**
  * Creates a ready/valid holding register, will not accept new data
  * until current data word is unloaded.
  *
  * This block has no combinational paths through it, although it can
  * accept data at a maximum of every other cycle.
  *
  * @param data The data type for the payload
  */
class DCHold[D <: Data](data: D) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(new DecoupledIO(data.cloneType))
    val deq = new DecoupledIO(data.cloneType)
  })
  override def desiredName: String = "DCHold_" + data.toString

  val p_valid = RegInit(init = 0.U)
  val p_data = Reg(data.cloneType)

  when (io.enq.valid && !p_valid) {
    p_valid := io.enq.valid
    p_data := io.enq.bits
  }.elsewhen((p_valid & io.deq.ready) === 1.U) {
    p_valid := 0.U
  }
  io.deq.valid := p_valid
  io.deq.bits := p_data
  io.enq.ready := ~p_valid
}

// Helper function for functional inference
object DCHold {
  def apply[D <: Data](x : DecoupledIO[D]) : DecoupledIO[D] = {
    val tout = Module(new DCHold(x.bits.cloneType))
    tout.io.enq <> x
    tout.io.deq
  }
}
