/*
 *
 * Serial Peripheral Interface (SPI) interface
 *
 * Author: Kevin Joly (kevin.joly@armadeus.com)
 *
 */
package chisel.lib.spi

import chisel3._
import chisel3.experimental.Analog
import chisel3.stage.ChiselStage
import chisel3.util._

class Master(frequency: Int, clkfreq: Int, bsize: Int) extends Module {
  val io = IO(new Bundle {
    val cpol = Input(Bool())
    val cpha = Input(Bool())
    val msbfirst = Input(Bool())
    val mosi = Output(Bool())
    val miso = Input(Bool())
    val sclk = Output(Bool())
    val din = Decoupled(UInt(bsize.W))
    val dout = Flipped(Decoupled(UInt(bsize.W)))
    val busy = Output(Bool())
  })

  object State extends ChiselEnum {
    val sIdle, sHalfCycle, sLoad, sShift = Value
  }

  val state = RegInit(State.sIdle)

  val clockPrescaler = (frequency + clkfreq / 2) / clkfreq / 2 - 1
  val CLKPRE = clockPrescaler.asUInt(clockPrescaler.W)

  val bits = RegInit(0.U((bsize - 1).W))
  val regout = RegInit(0.U(bsize.W))
  val regin = RegInit(0.U(bsize.W))
  val cnt = RegInit(0.U(clockPrescaler.W))

  val cpolReg = RegInit(false.B)
  val cphaReg = RegInit(false.B)
  val msbfirstReg = RegInit(false.B)

  switch(state) {
    is(State.sIdle) {

      cpolReg := io.cpol
      cphaReg := io.cpha
      msbfirstReg := io.msbfirst

      when(io.dout.valid) {
        regout := io.dout.bits
        bits := bsize.asUInt - 1.U
        cnt := CLKPRE
        when(cphaReg) {
          state := State.sHalfCycle
        }.otherwise {
          state := State.sLoad
        }
      }
      regin := 0.U
    }
    is(State.sHalfCycle) {
      when(cnt > 0.U) {
        cnt := cnt - 1.U
      }.otherwise {
        cnt := CLKPRE
        state := State.sLoad
      }
    }
    is(State.sLoad) {
      when(cnt > 0.U) {
        cnt := cnt - 1.U
      }.elsewhen(bits > 0.U || io.din.ready) {
        cnt := CLKPRE
        state := State.sShift
        when(msbfirstReg) {
          regin := Cat(regin(bsize - 2, 0), io.miso)
        }.otherwise {
          regin := Cat(io.miso, regin(bsize - 1, 1))
        }
      }
    }
    is(State.sShift) {
      when(cnt > 0.U) {
        cnt := cnt - 1.U
      }.otherwise {
        when(bits > 0.U) {
          cnt := CLKPRE
          bits := bits - 1.U
          state := State.sLoad
        }.otherwise {
          state := State.sIdle
        }
      }
    }
  }

  io.dout.ready := state === State.sIdle

  io.din.bits := regin
  val lastBitShifted = Wire(Bool())
  lastBitShifted := (state === State.sShift) && (bits === 0.U)
  io.din.valid := lastBitShifted && !RegNext(lastBitShifted)

  when(state === State.sLoad) {
    io.sclk := cpolReg ^ cphaReg
  }.elsewhen(state === State.sShift) {
    io.sclk := !(cpolReg ^ cphaReg)
  }.otherwise {
    io.sclk := cpolReg
  }

  when(state === State.sIdle) {
    io.mosi := false.B
  }.elsewhen(msbfirstReg) {
    io.mosi := regout(bits)
  }.otherwise {
    io.mosi := regout(bsize.U - 1.U - bits)
  }

  io.busy := state =/= State.sIdle
}

object Master extends App {
  emitVerilog(new Master(100000000, 10000000, 8), Array("--target-dir", "generated"))
}
