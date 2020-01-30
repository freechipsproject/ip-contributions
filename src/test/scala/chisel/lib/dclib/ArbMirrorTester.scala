package chisel.lib.dclib

import chisel3._
import chisel3.util._
import chisel3.iotesters.PeekPokeTester

class ArbMirrorTestbench(ways: Int) extends Module {
  val io = IO(new Bundle {
    val src_pat = Input(UInt(16.W))
    val dst_pat = Input(UInt(16.W))
    val color_error = Output(Bool())
    val seq_error = Output(Bool())
  })

  val arb = Module(new DCArbiter(new ColorToken(ways,16), ways, false))
  val mir = Module(new DCMirror(new ColorToken(ways,16), ways))
  val i_color_error = Wire(Vec(ways, Bool()))
  val i_seq_error = Wire(Vec(ways, Bool()))

  for (i <- 0 until ways) {
    val src = Module(new ColorSource(ways, 16))
    val dst = Module(new ColorSink(ways, 16))

    src.io.pattern := io.src_pat
    dst.io.pattern := io.dst_pat

    src.io.color := 0.U
    dst.io.color := 0.U

    src.io.enable := true.B
    dst.io.enable := true.B

    src.io.p <> arb.io.c(i)
    mir.io.p(i) <> dst.io.c
    i_color_error(i) := dst.io.color_error
    i_seq_error(i) := dst.io.seq_error
  }

  io.color_error := Cat(i_color_error).orR()
  io.seq_error := Cat(i_seq_error).orR()

  arb.io.p <> mir.io.c

  // hook up arbiter outputs as virtual circuits, to check grant behavior
  // is correct
  mir.io.dst := arb.io.grant
}

class ArbMirrorTester(tb: ArbMirrorTestbench) extends PeekPokeTester(tb) {
  poke(tb.io.src_pat, 0xFFFF.U)
  poke(tb.io.dst_pat, 0xFFFF.U)

  step(100)

  // try a couple other flow control patterns
  poke(tb.io.src_pat, 0xF000.U)
  poke(tb.io.dst_pat, 0xC0A0.U)

  step(50)
  poke(tb.io.src_pat, 0xAA55.U)
  poke(tb.io.dst_pat, 0xF00F.U)

  // errors are sticky, only need to check at the end
  expect(tb.io.color_error, false.B)
  expect(tb.io.seq_error, false.B)
}
