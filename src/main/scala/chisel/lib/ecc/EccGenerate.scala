// See README.md for license details.
package chisel.lib.ecc

import chisel3._
import chisel3.util.Cat

class EccGenerate[D <: Data](data: D, doubleBit : Boolean = true) extends Module {
  val eccBits = calcCodeBits(data.getWidth)

  val io = IO(new Bundle {
    val in = Input(data.cloneType)
    val out = Output(UInt(eccBits.W))
    val par = if (doubleBit) Some(Output(Bool())) else None
  })

  val bitValue = Wire(Vec(eccBits, Bool()))
  val outWidth = io.in.getWidth + eccBits
  val bitMapping = calcBitMapping(data.getWidth, false)

  for (i <- 0 until eccBits) {
    val bitSelect : Seq[UInt] = for (j <- buildSeq(i, outWidth)) yield io.in.asUInt()(bitMapping(j))
    bitValue(i) := bitSelect.reduce(_ ^ _)
  }
  io.out := Cat(bitValue.reverse)
  if (io.par.nonEmpty) {
    io.par.get := io.in.asUInt().xorR() ^ io.out.xorR()
  }
}
