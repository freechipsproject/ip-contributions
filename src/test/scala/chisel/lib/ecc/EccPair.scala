package chisel.lib.ecc
import chisel3._
import chisel3.util._

class EccPair(width : Int) extends Module {
  val eccBits = calcCodeBits(width)
  val outWidth = width + eccBits

  val io = IO(new Bundle {
    val dataIn = Input(UInt(width.W))
    val dataOut = Output(UInt(width.W))
    val injectError = Input(Bool())
    val errorLocation = Input(UInt(log2Ceil(width).W))
    val syndromeOut = Output(UInt(eccBits.W))
  })

  def getWidthParam : Int = { width }

  val intermediate = Wire(UInt(width.W))
  val eccGen = Module(new EccGenerate(io.dataIn.cloneType))
  val eccCheck = Module(new EccCheck(io.dataIn.cloneType))

  eccGen.io.in := io.dataIn

  when (io.injectError) {
    intermediate := io.dataIn ^ (1.U << io.errorLocation)
  }.otherwise {
    intermediate := io.dataIn
  }

  eccCheck.io.dataIn := intermediate
  eccCheck.io.eccIn := eccGen.io.out
  io.dataOut := eccCheck.io.dataOut
}
