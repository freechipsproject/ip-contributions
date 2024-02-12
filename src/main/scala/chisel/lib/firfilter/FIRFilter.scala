/*
 *
 * A fixed point FIR filter module.
 *
 * Author: Kevin Joly (kevin.joly@armadeus.com)
 *
 */

package chisel.lib.firfilter

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._

/*
 * FIR filter module
 *
 * Apply filter on input samples passed by ready/valid handshake. Coefficients
 * are to be set prior to push any input sample.
 *
 * All the computations are done in fixed point. Output width is inputWidth +
 * coefWidth + log2Ceil(coefNum).
 *
 */
class FIRFilter(
  inputWidth:       Int,
  coefWidth:        Int,
  coefDecimalWidth: Int,
  coefNum:          Int)
    extends Module {

  val outputWidth = inputWidth + coefWidth + log2Ceil(coefNum)

  val io = IO(new Bundle {
    /*
     * Input samples
     */
    val input = Flipped(Decoupled(SInt(inputWidth.W)))
    /*
     * Filter's coefficients b[0], b[1], ...
     */
    val coef = Input(Vec(coefNum, SInt(coefWidth.W)))
    /*
     * Filtered samples. Fixed point format is:
     * (inputWidth+coefWidth).coefDecimalWidth
     * Thus, output should be right shifted to the right of 'coefDecimalWidth' bits.
     */
    val output = Decoupled(SInt(outputWidth.W))
  })

  assert(coefWidth >= coefDecimalWidth)

  val coefIdx = RegInit(0.U(coefNum.W))

  object FIRFilterState extends ChiselEnum {
    val Idle, Compute, Valid, LeftOver = Value
  }

  val state = RegInit(FIRFilterState.Idle)

  switch(state) {
    is(FIRFilterState.Idle) {
      when(io.input.valid) {
        state := FIRFilterState.Compute
      }
    }
    is(FIRFilterState.Compute) {
      when(coefIdx === (coefNum - 1).U) {
        state := FIRFilterState.LeftOver
      }
    }
    is(FIRFilterState.LeftOver) {
      state := FIRFilterState.Valid
    }
    is(FIRFilterState.Valid) {
      when(io.output.ready) {
        state := FIRFilterState.Idle
      }
    }
  }

  when((state === FIRFilterState.Idle) && io.input.valid) {
    coefIdx := 1.U
  }.elsewhen(state === FIRFilterState.Compute) {
    when(coefIdx === (coefNum - 1).U) {
      coefIdx := 0.U
    }.otherwise {
      coefIdx := coefIdx + 1.U
    }
  }.otherwise {
    coefIdx := 0.U
  }

  val inputReg = RegInit(0.S(inputWidth.W))
  val inputMem = Mem(coefNum - 1, SInt(inputWidth.W))
  val inputMemAddr = RegInit(0.U(math.max(log2Ceil(coefNum - 1), 1).W))
  val inputMemOut = Wire(SInt(inputWidth.W))
  val inputRdWr = inputMem(inputMemAddr)

  inputMemOut := DontCare

  when(state === FIRFilterState.LeftOver) {
    inputRdWr := inputReg
  }.elsewhen((state === FIRFilterState.Idle) && io.input.valid) {
    inputReg := io.input.bits // Delayed write
    inputMemOut := inputRdWr
  }.otherwise {
    inputMemOut := inputRdWr
  }

  when((state === FIRFilterState.Compute) && (coefIdx < (coefNum - 1).U)) {
    when(inputMemAddr === (coefNum - 2).U) {
      inputMemAddr := 0.U
    }.otherwise {
      inputMemAddr := inputMemAddr + 1.U
    }
  }

  val inputSum = RegInit(0.S(outputWidth.W))

  val multNumOut = Wire(SInt((inputWidth + coefWidth).W))
  val multNumOutReg = RegInit(0.S((inputWidth + coefWidth).W))
  val multNumIn = Wire(SInt(inputWidth.W))

  when((state === FIRFilterState.Idle) && io.input.valid) {
    multNumOutReg := multNumOut
    inputSum := 0.S
  }.elsewhen(state === FIRFilterState.Compute) {
    when(coefIdx < coefNum.U) {
      multNumOutReg := multNumOut
      inputSum := inputSum +& multNumOutReg
    }
  }.elsewhen(state === FIRFilterState.LeftOver) {
    inputSum := inputSum +& multNumOutReg
  }

  when(state === FIRFilterState.Idle) {
    multNumIn := io.input.bits
  }.otherwise {
    multNumIn := inputMemOut
  }

  multNumOut := multNumIn * io.coef(coefIdx)

  io.input.ready := state === FIRFilterState.Idle
  io.output.valid := state === FIRFilterState.Valid
  io.output.bits := inputSum
}
