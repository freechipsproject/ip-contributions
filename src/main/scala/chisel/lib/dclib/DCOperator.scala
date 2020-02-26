package chisel.lib.dclib

import chisel3._
import chisel3.util._

/**
  * This module declares a multi-input decoupled operator.  This is an example using showing how to use
  * [[DCInput]] and [[DCOutput]] modules to create a module with registered-output timing.
  *
  * @param n      The number of inputs for the operator
  * @param width  The bit-width of inputs and outputs
  * @param op     Function with the required operator
  */
class DCOperator(n: Int, width: Int, op: (UInt, UInt) => UInt) extends Module {
  val io = IO(new Bundle {
    val a = Vec(n, Flipped(Decoupled(UInt(width.W))))
    val z = Decoupled(UInt(width.W))
  })
  require (n >= 2)
  val a_int = for (n <- 0 until n) yield DCInput(io.a(n))
  val z_dcout = Module(new DCOutput(UInt(width.W)))

  val all_valid = a_int.map(_.valid).reduce(_ & _)
  z_dcout.io.enq.bits := a_int.map(_.bits).reduce(_ + _)
  when (all_valid & z_dcout.io.enq.ready) {
    z_dcout.io.enq.valid := true.B
    for (n <- 0 until n) {
      a_int(n).ready := true.B
    }
  }.otherwise {
    z_dcout.io.enq.valid := false.B
    for (n <- 0 until n) {
      a_int(n).ready := false.B
    }
  }
  io.z <> z_dcout.io.deq
}

object CreateDcOperator extends App {
  def xor(a: UInt, b: UInt) : UInt = a ^ b
  chisel3.Driver.execute(Array("--target-dir", "generated"), () => new DCOperator(6, 8, xor))
}