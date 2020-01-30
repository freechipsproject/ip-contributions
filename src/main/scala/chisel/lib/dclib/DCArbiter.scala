package chisel.lib.dclib

import chisel3._
import chisel3.util._

/**
 * Round-robin arbiter
 *
 * Accepts number of inputs and arbitrates between them on a per-cycle basis.
 *
 * @param data    Data type of item to be arbitrated
 * @param inputs  Number of inputs to arbiter
 * @param locking Creates a locking arbiter with a rearb input
 */
class DCArbiter[D <: Data](data: D, inputs: Int, locking: Boolean) extends Module {
  val io = IO(new Bundle {
    val c = Vec(inputs, Flipped(Decoupled(data.cloneType)))
    val p = Decoupled(data.cloneType)
    val grant = Output(UInt(inputs.W))
    val rearb = if(locking) Some(Input(UInt(inputs.W))) else None
  })
  override def desiredName: String = "DCArbiter_" + data.toString

  val just_granted = RegInit(1.asUInt(inputs.W))
  val to_be_granted = Wire(UInt(inputs.W))
  val nxt_rr_locked = Wire(Bool())
  val io_c_valid = Wire(UInt(inputs.W))

  for (i <- 0 until inputs) {
    io.c(i).ready := to_be_granted(i) && io.p.ready
  }
  io.grant := to_be_granted

  def nxt_grant(cur_grant: UInt, cur_req: UInt, cur_accept: Bool): UInt = {
    val msk_req = Wire(UInt(inputs.W))
    val tmp_grant = Wire(UInt(inputs.W))
    val tmp_grant2 = Wire(UInt(inputs.W))
    val rv = Wire(UInt(inputs.W))

    msk_req := cur_req & ~((cur_grant - 1.U) | cur_grant)
    tmp_grant := msk_req & (~msk_req + 1.U)
    tmp_grant2 := cur_req & (~cur_req + 1.U)


    when(cur_accept) {
      when(msk_req =/= 0.U) {
        rv := tmp_grant
      }.otherwise {
        rv := tmp_grant2
      }
    }.elsewhen(cur_req =/= 0.U) {
      when(msk_req =/= 0.U) {
        rv := Cat(tmp_grant(0), tmp_grant(inputs - 1, 1))
      }.otherwise {
        rv := Cat(tmp_grant2(0), tmp_grant2(inputs - 1, 1))
      }
    }.otherwise {
      rv := cur_grant
    }
    rv
  }

  io_c_valid := Cat(io.c.map(_.valid).reverse)

  io.p.valid := io_c_valid.orR()
  to_be_granted := just_granted

  if (locking) {
    val rr_locked = RegInit(false.B)

    when ((io_c_valid & just_granted).orR() && !rr_locked) {
      nxt_rr_locked := true.B
    }.elsewhen ((io_c_valid & just_granted & io.rearb.get).orR()) {
      nxt_rr_locked := false.B
    }.otherwise {
      nxt_rr_locked := rr_locked
    }

    when (nxt_rr_locked && (io_c_valid & just_granted).orR()) {
      to_be_granted := just_granted
    }.otherwise {
      when (io.p.ready) {
        to_be_granted := Cat(just_granted(0), just_granted(inputs-1,1))
      }.otherwise {
        to_be_granted := just_granted
      }
    }
  } else {
    nxt_rr_locked := false.B
    to_be_granted := nxt_grant(just_granted, io_c_valid, io.p.ready)
  }

  when (to_be_granted =/= 0.U) {
    just_granted := to_be_granted
  }

  io.p.bits := io.c(0).bits
  for (i <- 0 until inputs) {
    when (to_be_granted(i)) {
      io.p.bits := io.c(i).bits
    }
  }
}

