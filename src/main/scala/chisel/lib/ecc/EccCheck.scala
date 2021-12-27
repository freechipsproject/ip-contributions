// See README.md for license details.
package chisel.lib.ecc

import chisel3._
import chisel3.util._

class withEcc[D <: Data](dat: D) extends Bundle {
  val data = dat.cloneType
  val ecc = UInt(calcCodeBits(dat.getWidth).W)
  val par = Bool()
  override def cloneType: this.type = {
    new withEcc(dat).asInstanceOf[this.type]
  }
}

class EccCheck[D <: Data](data: D, doubleBit: Boolean = true) extends Module {
  val eccBits = calcCodeBits(data.getWidth)

  val io = IO(new Bundle {
    val dataIn = Input(data.cloneType)
    val eccIn = Input(UInt(eccBits.W))
    val dataOut = Output(data.cloneType)
    val errorSyndrome = Output(UInt(eccBits.W))
    val parIn = if (doubleBit) Some(Input(Bool())) else None
    val doubleBitError = if (doubleBit) Some(Output(Bool())) else None
  })

  val outWidth = io.dataIn.getWidth + eccBits
  val errorSynVec = Wire(Vec(log2Ceil(outWidth), Bool()))
  val vecIn = Wire(Vec(outWidth, Bool()))
  val correctedOut = Wire(Vec(outWidth, Bool()))
  val reverseMap = calcBitMapping(data.getWidth, reversed = true)
  val outDataVec = Wire(Vec(data.getWidth, Bool()))

  // assign input bits to their correct location in the combined input/ecc vector
  for (i <- 0 until io.dataIn.getWidth) {
    vecIn(reverseMap(i)) := io.dataIn.asUInt()(i)
  }
  // assign eccBits to their location in the combined vector
  for (i <- 0 until eccBits) {
    vecIn((1 << i) - 1) := io.eccIn(i)
  }

  // compute the error syndrome location
  for (i <- 0 until eccBits) {
    val bitSelect: Seq[UInt] = for (j <- buildSeq(i, outWidth)) yield vecIn(j)
    errorSynVec(i) := bitSelect.reduce(_ ^ _)
  }
  io.errorSyndrome := Cat(errorSynVec.reverse) ^ io.eccIn

  // correct the bit error
  correctedOut := vecIn
  when(io.errorSyndrome =/= 0.U) {
    correctedOut(io.errorSyndrome - 1.U) := ~vecIn(io.errorSyndrome - 1.U)
  }

  // construct corrected output data
  for (i <- 0 until data.getWidth) {
    outDataVec(i) := correctedOut(reverseMap(i))
  }
  io.dataOut := Cat(outDataVec.reverse).asTypeOf(data.cloneType)
  if (io.doubleBitError.isDefined) {
    val computedParity = Wire(Bool())
    computedParity := io.dataIn.asUInt().xorR() ^ io.eccIn.xorR()
    io.doubleBitError.get := (io.errorSyndrome =/= 0.U) && (computedParity === io.parIn.get)
  }
}

// Helper function for functional inference
object EccCheck {
  def apply[D <: Data](x: withEcc[D]): withEcc[D] = {
    val withEccOut = Wire(new withEcc[D](x.data))
    val eccChecker = Module(new EccCheck(x.data.cloneType, true))
    eccChecker.io.dataIn := x.data
    eccChecker.io.eccIn := x.ecc
    eccChecker.io.parIn.get := x.par
    withEccOut.data := eccChecker.io.dataOut
    withEccOut.ecc := eccChecker.io.errorSyndrome
    withEccOut.par := eccChecker.io.doubleBitError.get
    withEccOut
  }
}
