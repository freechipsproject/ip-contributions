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

  val p_data = Reg(data.cloneType)
  val p_valid = RegInit(0.asUInt(n.W))
  val p_ready = Cat(io.p.map(_.ready).reverse)
  val nxt_accept = (p_valid === 0.U) || ((p_valid =/= 0.U) && ((p_valid & p_ready) === p_valid))

  when (nxt_accept) {
    p_valid := Fill(n, io.c.valid) & io.dst
    p_data := io.c.bits
  }.otherwise {
    p_valid := p_valid & ~p_ready
  }
  io.c.ready := nxt_accept

  for (i <- 0 until n) {
    io.p(i).bits := p_data
    io.p(i).valid := p_valid(i)
  }
}
