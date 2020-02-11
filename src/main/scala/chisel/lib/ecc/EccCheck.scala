package chisel.lib.ecc

import chisel3._
import chisel3.util._

class EccCheck[D <: Data](data: D) extends Module {
  val eccBits = calcCodeBits(data.getWidth)

  val io = IO(new Bundle {
    val dataIn = Input(data.cloneType)
    val eccIn = Input(UInt(eccBits.W))
    val dataOut = Output(data.cloneType)
    val errorSyndrome = Output(UInt(eccBits.W))
  })

  //val bitValue = Wire(Vec(eccBits, Bool()))
  val outWidth = io.dataIn.getWidth + eccBits
  //val errorSyndrome = Wire(UInt(log2Ceil(outWidth).W))
  val errorSynVec = Wire(Vec(log2Ceil(outWidth), Bool()))
  val vecIn = Wire(Vec(outWidth, Bool()))
  val correctedOut = Wire(Vec(outWidth, Bool()))
  val reverseMap = calcBitMapping(data.getWidth, reversed=true)
  val outDataVec = Wire(Vec(data.getWidth, Bool()))

  // assign input bits to their correct location in the combined input/ecc vector
  for (i <- 0 until io.dataIn.getWidth) {
    //println("vecin(%d)".format(reverseMap(i)))
    vecIn(reverseMap(i)) := io.dataIn.asUInt()(i)
  }
  // assign eccBits to their location in the combined vector
  for (i <- 0 until eccBits) {
    //println("vecin(%d)".format((1 << i) - 1))
    vecIn((1 << i)-1) := io.eccIn(i)
  }

  // compute the error syndrome location
  for (i <- 0 until eccBits) {
    val bitSelect : Seq[UInt] = for (j <- buildSeq(i, outWidth)) yield vecIn(j)
    errorSynVec(i) := bitSelect.reduce(_ ^ _)
  }
  io.errorSyndrome := Cat(errorSynVec.reverse)

  // correct the bit error
  correctedOut := vecIn
  when (io.errorSyndrome =/= 0.U) {
    correctedOut(io.errorSyndrome-1.U) := ~vecIn(io.errorSyndrome-1.U)
  }

  // construct corrected output data
  for (i <- 0 until data.getWidth) {
    outDataVec(i) := correctedOut(reverseMap(i))
  }
  io.dataOut := Cat(outDataVec.reverse).asTypeOf(data.cloneType)
}
