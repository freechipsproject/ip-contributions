/*
 *
 * A fixed point IIR filter module.
 *
 * Author: Kevin Joly (kevin.joly@armadeus.com)
 *
 */

package chisel.lib.iirfilter

import chisel3._
import chisel3.util._

/*
 * IIR filter module
 *
 * Apply filter on input samples passed by ready/valid handshake. Numerators
 * and denominators are to be set prior to push any input sample.
 *
 * All the computations are done in fixed point. The user should manage the
 * input decimal width by himself. Output width should be sufficient in order
 * to not overflow (i.e. in case of overshoot). A minimum output width of
 * inputWidht+coefWidth+log2Ceil(numeratorNum + denominatorNum) + 1 is
 * requested.
 *
 */
class IIRFilter(
  inputWidth:       Int,
  coefWidth:        Int,
  coefDecimalWidth: Int,
  outputWidth:      Int,
  numeratorNum:     Int,
  denominatorNum:   Int)
    extends Module {
  val io = IO(new Bundle {
    /*
     * Input samples
     */
    val input = Flipped(Decoupled(SInt(inputWidth.W)))
    /*
     * Numerator's coefficients b[0], b[1], ...
     */
    val num = Input(Vec(numeratorNum, SInt(coefWidth.W)))
    /*
     * The first coefficient of the denominator should be omitted and should be a[0] == 1.
     * a[1], a[2], ...
     */
    val den = Input(Vec(denominatorNum, SInt(coefWidth.W)))
    /*
     * Filtered samples. Fixed point format is:
     * (outputWidth-coefDecimalWidth).coefDecimalWidth
     * Thus, output should be right shifted to the right of 'coefDecimalWidth' bits.
     */
    val output = Decoupled(SInt(outputWidth.W))
  })

  assert(coefWidth >= coefDecimalWidth)

  val minOutputWidth = inputWidth + coefWidth + log2Ceil(numeratorNum + denominatorNum) + 1
  assert(outputWidth >= minOutputWidth)

  val coefNum = RegInit(0.U(log2Ceil(math.max(numeratorNum, denominatorNum)).W))

  object IIRFilterState extends ChiselEnum {
    val Idle, ComputeNum, ComputeDen, Valid, StoreLast = Value
  }

  val state = RegInit(IIRFilterState.Idle)

  switch(state) {
    is(IIRFilterState.Idle) {
      when(io.input.valid) {
        state := IIRFilterState.ComputeNum
      }
    }
    is(IIRFilterState.ComputeNum) {
      when(coefNum === (numeratorNum - 1).U) {
        state := IIRFilterState.ComputeDen
      }
    }
    is(IIRFilterState.ComputeDen) {
      when(coefNum === (denominatorNum - 1).U) {
        state := IIRFilterState.StoreLast
      }
    }
    is(IIRFilterState.StoreLast) {
      state := IIRFilterState.Valid
    }
    is(IIRFilterState.Valid) {
      when(io.output.ready) {
        state := IIRFilterState.Idle
      }
    }
  }

  when((state === IIRFilterState.Idle) && io.input.valid) {
    coefNum := 1.U
  }.elsewhen(state === IIRFilterState.ComputeNum) {
    when(coefNum === (numeratorNum - 1).U) {
      coefNum := 0.U
    }.otherwise {
      coefNum := coefNum + 1.U
    }
  }.elsewhen(state === IIRFilterState.ComputeDen) {
    when(coefNum === (denominatorNum - 1).U) {
      coefNum := 0.U
    }.otherwise {
      coefNum := coefNum + 1.U
    }
  }.otherwise {
    coefNum := 0.U
  }

  val inputReg = RegInit(0.S(inputWidth.W))
  val inputMem = Mem(numeratorNum - 1, SInt(inputWidth.W))
  val inputMemAddr = RegInit(0.U(math.max(log2Ceil(numeratorNum - 1), 1).W))
  val inputMemOut = Wire(SInt(inputWidth.W))
  val inputRdWr = inputMem(inputMemAddr)

  inputMemOut := DontCare

  when(state === IIRFilterState.StoreLast) {
    inputRdWr := inputReg
  }.elsewhen((state === IIRFilterState.Idle) && io.input.valid) {
    inputReg := io.input.bits // Delayed write
    inputMemOut := inputRdWr
  }.otherwise {
    inputMemOut := inputRdWr
  }

  when((state === IIRFilterState.ComputeNum) && (coefNum < (numeratorNum - 1).U)) {
    when(inputMemAddr === (numeratorNum - 2).U) {
      inputMemAddr := 0.U
    }.otherwise {
      inputMemAddr := inputMemAddr + 1.U
    }
  }

  val outputMem = Mem(denominatorNum, SInt(outputWidth.W))
  val outputMemAddr = RegInit(0.U(math.max(log2Ceil(denominatorNum), 1).W))
  val outputMemOut = Wire(SInt(outputWidth.W))
  val outputRdWr = outputMem(outputMemAddr)

  outputMemOut := DontCare

  when((state === IIRFilterState.Valid) && (RegNext(state) === IIRFilterState.StoreLast)) {
    outputRdWr := io.output.bits
  }.otherwise {
    outputMemOut := outputRdWr
  }

  when((state === IIRFilterState.ComputeDen) && (coefNum < (denominatorNum - 1).U)) {
    when(outputMemAddr === (denominatorNum - 1).U) {
      outputMemAddr := 0.U
    }.otherwise {
      outputMemAddr := outputMemAddr + 1.U
    }
  }

  val inputSum = RegInit(0.S((inputWidth + coefWidth + log2Ceil(numeratorNum)).W))
  val outputSum = RegInit(0.S((outputWidth + coefWidth + log2Ceil(denominatorNum)).W))

  val multOut = Wire(SInt((outputWidth + coefWidth).W))
  val multOutReg = RegInit(0.S((outputWidth + coefWidth).W))
  val multIn = Wire(SInt(outputWidth.W))
  val multCoef = Wire(SInt(coefWidth.W))

  when((state === IIRFilterState.Idle) && io.input.valid) {
    multOutReg := multOut
    outputSum := 0.S
    inputSum := 0.S
  }.elsewhen(state === IIRFilterState.ComputeNum) {
    multOutReg := multOut
    inputSum := inputSum +& multOutReg
  }.elsewhen(state === IIRFilterState.ComputeDen) {
    multOutReg := multOut

    when(coefNum === 0.U) {
      // Store numerator's last value
      inputSum := inputSum +& multOutReg
    }.otherwise {
      outputSum := outputSum +& multOutReg
    }
  }.elsewhen(state === IIRFilterState.StoreLast) {
    outputSum := outputSum +& multOutReg
  }

  when(state === IIRFilterState.ComputeNum) {
    multIn := inputMemOut
  }.elsewhen(state === IIRFilterState.ComputeDen) {
    multIn := outputMemOut
  }.otherwise {
    multIn := io.input.bits
  }

  when(state === IIRFilterState.ComputeDen) {
    multCoef := io.den(coefNum)
  }.otherwise {
    multCoef := io.num(coefNum)
  }

  multOut := multIn * multCoef

  io.input.ready := state === IIRFilterState.Idle
  io.output.valid := state === IIRFilterState.Valid
  io.output.bits := inputSum -& (outputSum >> coefDecimalWidth)
}
