package chisel.lib.dclib

import chisel3._
import chisel3.util._

/**
  * This module declares a multi-input decoupled operator.  This is an example using showing how to use
  * [[DCInput]] and [[DCOutput]] modules to create a module with registered-output timing.
  *
  * @param data Data type to operate on
  * @param n    The number of inputs for the operator
  * @param op   Function with the required operator
  */
class DCReduce[D <: Data](data: D, n: Int, op: (D, D) => D) extends Module {
  val io = IO(new Bundle {
    val a = Vec(n, Flipped(Decoupled(data.cloneType)))
    val z = Decoupled(data.cloneType)
  })
  require(n >= 2)
  val aInt = for (n <- 0 until n) yield DCInput(io.a(n))
  val zInt = Wire(Decoupled(data.cloneType))
  val zDcout = DCOutput(zInt)

  val all_valid = aInt.map(_.valid).reduce(_ & _)
  zInt.bits := aInt.map(_.bits).reduce(op)
  when(all_valid & zInt.ready) {
    zInt.valid := true.B
    for (n <- 0 until n) {
      aInt(n).ready := true.B
    }
  }.otherwise {
    zInt.valid := false.B
    for (n <- 0 until n) {
      aInt(n).ready := false.B
    }
  }
  io.z <> zDcout
}

object CreateDcReduce extends App {
  def xor(a: UInt, b: UInt): UInt = a ^ b

  (new chisel3.stage.ChiselStage).execute(
    Array("--target-dir", "generated"),
    Seq(chisel3.stage.ChiselGeneratorAnnotation(() => new DCReduce(UInt(8.W), n = 6, op = xor)))
  )
}
