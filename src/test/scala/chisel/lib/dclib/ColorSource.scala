package chisel.lib.dclib

import chisel3._
import chisel3.util._

class PktToken(asz: Int, cycsz: Int = 16) extends Bundle {
  val src = UInt(asz.W)
  val dst = UInt(asz.W)
  val cycle = UInt(cycsz.W)
}

class ColorToken(colors: Int, dsz: Int) extends Bundle {
  val color = UInt(log2Ceil(colors).W)
  val seqnum = UInt(dsz.W)

}

class ColorSource(colors: Int, dsz: Int) extends Module {
  val io = IO(new Bundle {
    val p = Decoupled(new ColorToken(colors, dsz))
    val enable = Input(Bool())
    val pattern = Input(UInt(16.W))
    val color = Input(UInt(log2Ceil(colors).W))
  })

  val seqnum = RegInit(0.asUInt(dsz.W))
  val strobe = RegInit(0.asUInt(4.W))

  when(io.p.fire()) {
    seqnum := seqnum + 1.U
  }

  io.p.valid := io.pattern(strobe)

  // advance the strobe whenever we are not providing data or when it
  // is accepted
  when(io.p.ready || !io.pattern(strobe)) {
    strobe := strobe + 1.U
  }
  io.p.bits.color := io.color
  io.p.bits.seqnum := seqnum
}

/**
  * Receive an incoming stream of color tokens, assert error whenever
  * the tokens don't match
  */
class ColorSink(colors: Int, dsz: Int) extends Module {
  val io = IO(new Bundle {
    val c = Flipped(Decoupled(new ColorToken(colors, dsz)))
    val enable = Input(Bool())
    val pattern = Input(UInt(16.W))
    val color = Input(UInt(log2Ceil(colors).W))
    val seqError = Output(Bool())
    val colorError = Output(Bool())
    val okCount = Output(UInt(32.W))
  })

  val seqnum = RegInit(0.asUInt(dsz.W))
  val strobe = RegInit(0.asUInt(4.W))
  val seq_error = RegInit(false.B)
  val color_error = RegInit(false.B)
  val okCount = RegInit(0.U(32.W))

  when(io.c.fire()) {
    seqnum := seqnum + 1.U
    when(io.c.bits.seqnum =/= seqnum) {
      seq_error := true.B
    }
    when(io.c.bits.color =/= io.color) {
      color_error := true.B
    }
    when((io.c.bits.seqnum === seqnum) && (io.c.bits.color === io.color)) {
      okCount := okCount + 1.U
    }
  }

  io.c.ready := io.pattern(strobe)
  io.okCount := okCount

  // advance the strobe whenever we accept a word or whenever
  // we are stalling
  when(io.c.valid || !io.pattern(strobe)) {
    strobe := strobe + 1.U
  }

  io.seqError := seq_error
  io.colorError := color_error
}

class LFSR16(init: Int = 1) extends Module {
  val io = IO(new Bundle {
    val inc = Input(Bool())
    val out = Output(UInt(16.W))
  })
  val res = RegInit(init.U(16.W))
  when(io.inc) {
    val nxt_res = Cat(res(0) ^ res(2) ^ res(3) ^ res(5), res(15, 1))
    res := nxt_res
  }
  io.out := res
}

class PktTokenSource(asz: Int, cycsz: Int = 16, id: Int = 0) extends Module {
  val io = IO(new Bundle {
    val p = Decoupled(new PktToken(asz, cycsz))
    val enable = Input(Bool())
    val pattern = Input(UInt(16.W))
    val src = Input(UInt(asz.W))
    val dst = Input(UInt(asz.W))
    val cum_delay = Output(UInt(32.W))
  })

  val cycle = RegInit(0.U(cycsz.W))
  val strobe = RegInit(0.U(4.W))
  val timestamp = Reg(UInt(cycsz.W))
  val ohold = Module(new DCOutput(new PktToken(asz, cycsz)))
  val cum_delay = RegInit(0.U(32.W))

  cycle := cycle + 1.U
  io.cum_delay := cum_delay

  when(io.p.fire() || !io.pattern(strobe)) {
    strobe := strobe + 1.U
  }
  ohold.io.enq.valid := io.pattern(strobe)
  ohold.io.enq.bits.src := io.src
  ohold.io.enq.bits.dst := io.dst
  ohold.io.enq.bits.cycle := cycle

  io.p <> ohold.io.deq

  when(io.p.valid && !io.p.ready) {
    cum_delay := cum_delay + 1.U
  }
}

class PktTokenSink(asz: Int, cycsz: Int = 16, id: Int = 0) extends Module {
  val io = IO(new Bundle {
    val c = Flipped(Decoupled(new PktToken(asz, cycsz)))
    val enable = Input(Bool())
    val pattern = Input(UInt(16.W))
    val dest = Input(UInt(asz.W))
    val cum_latency = Output(UInt(32.W))
    val pkt_count = Output(UInt(32.W))
    val addr_error = Output(Bool())
  })

  val strobe = RegInit(0.asUInt(4.W))
  val c_hold = DCOutput(io.c)
  val cycle = RegInit(0.asUInt(16.W))
  val cum_latency = RegInit(0.asUInt(32.W))
  val pkt_count = RegInit(0.asUInt(32.W))
  val addr_error = RegInit(false.B)

  cycle := cycle + 1.U

  c_hold.ready := io.pattern(strobe)
  when(c_hold.fire() || !io.pattern(strobe)) {
    strobe := strobe + 1.U
  }

  when(c_hold.fire()) {
    cum_latency := cum_latency + (cycle - c_hold.bits.cycle)
    pkt_count := pkt_count + 1.U
    when(c_hold.bits.dst =/= id.U) {
      addr_error := true.B
    }
  }
  io.cum_latency := cum_latency
  io.pkt_count := pkt_count
  io.addr_error := addr_error
}
