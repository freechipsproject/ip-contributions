package chisel.lib.ecc

import chisel3._
import chisel3.util.Cat

class EccGenerate[D <: Data](data: D) extends Module {
  val eccBits = calcCodeBits(data.getWidth)

  val io = IO(new Bundle {
    val in = Input(data.cloneType)
    val out = Output(UInt(eccBits.W))
  })

  val bitValue = Wire(Vec(eccBits, Bool()))
  val outWidth = io.in.getWidth + eccBits
  val bitMapping = calcBitMapping(outWidth, false)

  for (i <- 0 to eccBits) {
    val bitSelect : Seq[UInt] = for (j <- buildSeq(i, outWidth)) yield io.in.asUInt()(bitMapping(j))
    bitValue(i) := bitSelect.reduce(_ ^ _)
  }
  io.out := Cat(bitValue.reverse)
}
