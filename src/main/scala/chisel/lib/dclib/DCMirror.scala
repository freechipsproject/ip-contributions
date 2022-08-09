package chisel.lib.dclib

import chisel3._
import chisel3.util._

/**
  * Sends tokens to multiple output destinations, as selected by bit
  * vector "dst".  dst must have at least one bit set for correct
  * operation.
  *
  * @param data Payload data type
  * @param n    Number of output destinations
  */
class DCMirror[D <: Data](data: D, n: Int) extends Module {
  val io = IO(new Bundle {
    val dst = Input(UInt(n.W))
    val c = Flipped(new DecoupledIO(data.cloneType))
    val p = Vec(n, new DecoupledIO(data.cloneType))
  })

  override def desiredName: String = "DCMirror_" + data.toString + "_N" + n.toString

  val pData = Reg(data.cloneType)
  val pValid = RegInit(0.asUInt(n.W))
  val pReady = Cat(io.p.map(_.ready).reverse)
  val nxtAccept = (pValid === 0.U) || ((pValid =/= 0.U) && ((pValid & pReady) === pValid))

  when(nxtAccept) {
    pValid := Fill(n, io.c.valid) & io.dst
    pData := io.c.bits
  }.otherwise {
    pValid := pValid & ~pReady
  }
  io.c.ready := nxtAccept

  for (i <- 0 until n) {
    io.p(i).bits := pData
    io.p(i).valid := pValid(i)
  }
}
