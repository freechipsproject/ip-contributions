package chisel.lib.dclib

import chisel3._
import chisel3.util._

/**
 * Creates a new signal class for sending credit-based flow control.
 *
 * This is useful for building systems which are insensitive to variable latency
 * delays.
 */
class CreditIO[D <: Data](data: D) extends Bundle {
  val valid = Output(Bool())
  val credit = Input(Bool())
  val bits = Output(data)
}

class DCCreditSender[D <: Data](data: D, val maxCredit: Int) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(Decoupled(data.cloneType))
    val deq = new CreditIO(data.cloneType)
    val curCredit = Output(UInt(log2Ceil(maxCredit).W))
  })
  require(maxCredit >= 1)
  override def desiredName: String = "DCCreditSender_" + data.toString

  val icredit = RegNext(io.deq.credit)
  val curCredit = RegInit(init=maxCredit.U)
  when (icredit && !io.enq.fire) {
    curCredit := curCredit + 1.U
  }.elsewhen(!icredit && io.enq.fire) {
    curCredit := curCredit - 1.U
  }
  io.enq.ready := curCredit > 0.U
  val dataOut = RegEnable(next=io.enq.bits, enable=io.enq.fire)
  val validOut = RegNext(next=io.enq.fire, init=false.B)
  io.deq.valid := validOut
  io.deq.bits := dataOut
  io.curCredit := curCredit
}

class DCCreditReceiver[D <: Data](data: D, val maxCredit: Int) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(new CreditIO(data.cloneType))
    val deq = new DecoupledIO(data.cloneType)
    val fifoCount = Output(UInt(log2Ceil(maxCredit+1).W))
  })
  require(maxCredit >= 1)
  override def desiredName: String = "DCCreditReceiver_" + data.toString

  val ivalid = RegNext(io.enq.valid)
  val idata = RegNext(io.enq.bits)
  val outFifo = Module(new Queue(data.cloneType, maxCredit))
  val nextCredit = WireDefault(0.B)

  outFifo.io.enq.bits := idata

  // bypass the FIFO when empty
  when (!outFifo.io.deq.valid && (outFifo.io.count === 0.U)) {
    when (io.deq.ready) {
      outFifo.io.enq.valid := false.B
      nextCredit := ivalid
    }.otherwise {
      outFifo.io.enq.valid := ivalid
    }
    outFifo.io.deq.ready := false.B
    io.deq.valid := ivalid
    io.deq.bits := idata
  }.otherwise {
    outFifo.io.enq.valid := ivalid
    outFifo.io.enq.bits := idata
    io.deq <> outFifo.io.deq
    nextCredit := outFifo.io.deq.fire
  }
  io.fifoCount := outFifo.io.count
  val ocredit = RegNext(next=nextCredit, init=false.B)
  io.enq.credit := ocredit
}
