package chisel.lib.dclib

import chisel3._
import chisel3.util._

/**
  * This module declares a multi-input decoupled operator.  This is an example using showing how to use
  * [[DCInput]] and [[DCOutput]] modules to create a module with registered-output timing.
  *
  * @param data   Data type to operate on
  * @param n      The number of inputs for the operator
  * @param op     Function with the required operator
  */
class DCReduce[D <: Data](data: D, n: Int, op: (D, D) => D) extends Module {
  val io = IO(new Bundle {
    val a = Vec(n, Flipped(Decoupled(data.cloneType)))
    val z = Decoupled(data.cloneType)
  })
  require (n >= 2)
  val a_int = for (n <- 0 until n) yield DCInput(io.a(n))
  val z_int = Wire(Decoupled(data.cloneType))
  val z_dcout = DCOutput(z_int)

  val all_valid = a_int.map(_.valid).reduce(_ & _)
  z_int.bits := a_int.map(_.bits).reduce(op)
  when (all_valid & z_int.ready) {
    z_int.valid := true.B
    for (n <- 0 until n) {
      a_int(n).ready := true.B
    }
  }.otherwise {
    z_int.valid := false.B
    for (n <- 0 until n) {
      a_int(n).ready := false.B
    }
  }
  io.z <> z_dcout
}

object CreateDcReduce extends App {
  def xor(a: UInt, b: UInt) : UInt = a ^ b
  (new chisel3.stage.ChiselStage).execute(Array("--target-dir", "generated"), 
                                          Seq(chisel3.stage.ChiselGeneratorAnnotation(() => new DCReduce(UInt(8.W), n=6, op=xor))))
}
