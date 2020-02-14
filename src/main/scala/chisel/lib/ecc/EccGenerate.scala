// See README.md for license details.
package chisel.lib.ecc

import chisel3._
import chisel3.util.Cat

class EccGenerate[D <: Data](data: D, doubleBit : Boolean = true) extends Module {
  val eccBits = calcCodeBits(data.getWidth)

  val io = IO(new Bundle {
    val dataIn = Input(data.cloneType)
    val eccOut = Output(UInt(eccBits.W))
    val parOut = if (doubleBit) Some(Output(Bool())) else None
  })

  val bitValue = Wire(Vec(eccBits, Bool()))
  val outWidth = io.dataIn.getWidth + eccBits
  val bitMapping = calcBitMapping(data.getWidth, false)

  for (i <- 0 until eccBits) {
    val bitSelect : Seq[UInt] = for (j <- buildSeq(i, outWidth)) yield io.dataIn.asUInt()(bitMapping(j))
    bitValue(i) := bitSelect.reduce(_ ^ _)
  }
  io.eccOut := Cat(bitValue.reverse)
  if (io.parOut.nonEmpty) {
    io.parOut.get := io.dataIn.asUInt().xorR() ^ io.eccOut.xorR()
  }
}
