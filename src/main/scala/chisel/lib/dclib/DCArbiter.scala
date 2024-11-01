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
    val rearb = if (locking) Some(Input(UInt(inputs.W))) else None
  })

  override def desiredName: String = "DCArbiter_" + data.toString

  val justGranted = RegInit(1.asUInt(inputs.W))
  val toBeGranted = Wire(UInt(inputs.W))
  val nxtRRLocked = Wire(Bool())
  val ioCValid = Wire(UInt(inputs.W))

  for (i <- 0 until inputs) {
    io.c(i).ready := toBeGranted(i) && io.p.ready
  }
  io.grant := toBeGranted

  def nxt_grant(cur_grant: UInt, cur_req: UInt, cur_accept: Bool): UInt = {
    val mskReq = Wire(UInt(inputs.W))
    val tmpGrant = Wire(UInt(inputs.W))
    val tmpGrant2 = Wire(UInt(inputs.W))
    val rv = Wire(UInt(inputs.W))

    mskReq := cur_req & ~((cur_grant - 1.U) | cur_grant)
    tmpGrant := mskReq & (~mskReq + 1.U)
    tmpGrant2 := cur_req & (~cur_req + 1.U)

    when(cur_accept) {
      when(mskReq =/= 0.U) {
        rv := tmpGrant
      }.otherwise {
        rv := tmpGrant2
      }
    }.elsewhen(cur_req =/= 0.U) {
      when(mskReq =/= 0.U) {
        rv := Cat(tmpGrant(0), tmpGrant(inputs - 1, 1))
      }.otherwise {
        rv := Cat(tmpGrant2(0), tmpGrant2(inputs - 1, 1))
      }
    }.otherwise {
      rv := cur_grant
    }
    rv
  }

  ioCValid := Cat(io.c.map(_.valid).reverse)

  io.p.valid := ioCValid.orR
  toBeGranted := justGranted

  if (locking) {
    val rr_locked = RegInit(false.B)

    when((ioCValid & justGranted).orR && !rr_locked) {
      nxtRRLocked := true.B
    }.elsewhen((ioCValid & justGranted & io.rearb.get).orR) {
      nxtRRLocked := false.B
    }.otherwise {
      nxtRRLocked := rr_locked
    }

    when(nxtRRLocked && (ioCValid & justGranted).orR) {
      toBeGranted := justGranted
    }.otherwise {
      when(io.p.ready) {
        toBeGranted := Cat(justGranted(0), justGranted(inputs - 1, 1))
      }.otherwise {
        toBeGranted := justGranted
      }
    }
  } else {
    nxtRRLocked := false.B
    toBeGranted := nxt_grant(justGranted, ioCValid, io.p.ready)
  }

  when(toBeGranted =/= 0.U) {
    justGranted := toBeGranted
  }

  io.p.bits := io.c(0).bits
  for (i <- 0 until inputs) {
    when(toBeGranted(i)) {
      io.p.bits := io.c(i).bits
    }
  }
}
