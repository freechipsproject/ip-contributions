package chisel.lib.dclib

import chisel3._
import chisel3.util._
import chisel3.iotesters.PeekPokeTester

class ArbMirrorTestbench(ways: Int) extends Module {
  val io = IO(new Bundle {
    val srcPat = Input(UInt(16.W))
    val dstPat = Input(UInt(16.W))
    val colorError = Output(Bool())
    val seqError = Output(Bool())
  })

  val arb = Module(new DCArbiter(new ColorToken(ways,16), ways, false))
  val mir = Module(new DCMirror(new ColorToken(ways,16), ways))
  val i_color_error = Wire(Vec(ways, Bool()))
  val i_seq_error = Wire(Vec(ways, Bool()))

  for (i <- 0 until ways) {
    val src = Module(new ColorSource(ways, 16))
    val dst = Module(new ColorSink(ways, 16))

    src.io.pattern := io.srcPat
    dst.io.pattern := io.dstPat

    src.io.color := 0.U
    dst.io.color := 0.U

    src.io.enable := true.B
    dst.io.enable := true.B

    src.io.p <> arb.io.c(i)
    mir.io.p(i) <> dst.io.c
    i_color_error(i) := dst.io.colorError
    i_seq_error(i) := dst.io.seqError
  }

  io.colorError := Cat(i_color_error).orR()
  io.seqError := Cat(i_seq_error).orR()

  arb.io.p <> mir.io.c

  // hook up arbiter outputs as virtual circuits, to check grant behavior
  // is correct
  mir.io.dst := arb.io.grant
}
