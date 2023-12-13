/*
 * A very simple test collection for IIRFilter module.
 *
 * See README.md for license details.
 */

package chisel.lib.iirfilter

import chisel3._
import chisel3.experimental.VecLiterals._
import chisel3.util.log2Ceil
import chiseltest._

import org.scalatest.flatspec.AnyFlatSpec

class IIRFilterNumeratorTest extends AnyFlatSpec with ChiselScalatestTester {
  "IIRFilter numerator" should "work" in {

    val inputWidth = 4
    val coefWidth = 3
    val coefDecimalWidth = 0
    val num = Seq(2, 1, 0, 3)
    val den = Seq(0, 0)
    val outputWidth = inputWidth + coefWidth + log2Ceil(num.length + den.length) + 1

    test(
      new IIRFilter(
        inputWidth = inputWidth,
        coefWidth = coefWidth,
        coefDecimalWidth = coefDecimalWidth,
        outputWidth = outputWidth,
        numeratorNum = num.length,
        denominatorNum = den.length
      )
    ) { dut =>
      dut.io.num.poke(Vec.Lit(num.map(_.S(coefWidth.W)): _*))
      dut.io.den.poke(Vec.Lit(den.map(_.S(coefWidth.W)): _*))

      dut.io.output.ready.poke(true.B)

      // Sample 1: Write 1. on input port
      dut.io.input.bits.poke(1.S)
      dut.io.input.valid.poke(true.B)
      dut.io.input.ready.expect(true.B)
      dut.io.output.valid.expect(false.B)
      dut.clock.step(1)
      dut.io.input.valid.poke(false.B)
      dut.io.input.ready.expect(false.B)

      for (i <- 0 until (num.length + den.length)) {
        dut.io.output.valid.expect(false.B)
        dut.io.input.ready.expect(false.B)
        dut.clock.step(1)
      }

      dut.io.output.bits.expect(2.S)
      dut.io.output.valid.expect(true.B)

      dut.clock.step(1)

      // Sample 2: Write 1. on input port
      dut.io.input.bits.poke(1.S)
      dut.io.input.valid.poke(true.B)
      dut.io.input.ready.expect(true.B)
      dut.io.output.valid.expect(false.B)
      dut.clock.step(1)
      dut.io.input.valid.poke(false.B)

      for (i <- 0 until (num.length + den.length)) {
        dut.io.output.valid.expect(false.B)
        dut.io.input.ready.expect(false.B)
        dut.clock.step(1)
      }

      dut.io.output.bits.expect(3.S)
      dut.io.output.valid.expect(true.B)

      dut.clock.step(1)

      // Sample 3: Write 0. on input port
      dut.io.input.bits.poke(0.S)
      dut.io.input.valid.poke(true.B)
      dut.io.input.ready.expect(true.B)
      dut.io.output.valid.expect(false.B)
      dut.clock.step(1)
      dut.io.input.valid.poke(false.B)

      for (i <- 0 until (num.length + den.length)) {
        dut.io.output.valid.expect(false.B)
        dut.io.input.ready.expect(false.B)
        dut.clock.step(1)
      }

      dut.io.output.bits.expect(1.S)
      dut.io.output.valid.expect(true.B)

      dut.clock.step(1)

      // Sample 4: Write 0. on input port
      dut.io.input.bits.poke(0.S)
      dut.io.input.valid.poke(true.B)
      dut.io.input.ready.expect(true.B)
      dut.io.output.valid.expect(false.B)
      dut.clock.step(1)
      dut.io.input.valid.poke(false.B)

      for (i <- 0 until (num.length + den.length)) {
        dut.io.output.valid.expect(false.B)
        dut.io.input.ready.expect(false.B)
        dut.clock.step(1)
      }

      dut.io.output.bits.expect(3.S)
      dut.io.output.valid.expect(true.B)
    }
  }
}

class IIRFilterDenominatorTest extends AnyFlatSpec with ChiselScalatestTester {
  "IIRFilter denominator" should "work" in {

    val inputWidth = 4
    val coefWidth = 3
    val coefDecimalWidth = 0
    val num = Seq(1, 0)
    val den = Seq(2, 3, 1)
    val outputWidth = inputWidth + coefWidth + log2Ceil(num.length + den.length) + 1

    test(
      new IIRFilter(
        inputWidth = inputWidth,
        coefWidth = coefWidth,
        coefDecimalWidth = coefDecimalWidth,
        outputWidth = outputWidth,
        numeratorNum = num.length,
        denominatorNum = den.length
      )
    ) { dut =>
      dut.io.num.poke(Vec.Lit(num.map(_.S(coefWidth.W)): _*))
      dut.io.den.poke(Vec.Lit(den.map(_.S(coefWidth.W)): _*))

      dut.io.output.ready.poke(true.B)

      // Sample 1: Write 1. on input port
      dut.io.input.bits.poke(1.S)
      dut.io.input.valid.poke(true.B)
      dut.io.input.ready.expect(true.B)
      dut.io.output.valid.expect(false.B)
      dut.clock.step(1)
      dut.io.input.valid.poke(false.B)
      dut.io.input.ready.expect(false.B)

      for (i <- 0 until (num.length + den.length)) {
        dut.io.output.valid.expect(false.B)
        dut.io.input.ready.expect(false.B)
        dut.clock.step(1)
      }

      dut.io.output.bits.expect(1.S)
      dut.io.output.valid.expect(true.B)

      dut.clock.step(1)

      // Sample 2: Write 1. on input port
      dut.io.input.bits.poke(1.S)
      dut.io.input.valid.poke(true.B)
      dut.io.input.ready.expect(true.B)
      dut.io.output.valid.expect(false.B)
      dut.clock.step(1)
      dut.io.input.valid.poke(false.B)

      for (i <- 0 until (num.length + den.length)) {
        dut.io.output.valid.expect(false.B)
        dut.io.input.ready.expect(false.B)
        dut.clock.step(1)
      }

      dut.io.output.bits.expect(-1.S)
      dut.io.output.valid.expect(true.B)

      dut.clock.step(1)

      // Sample 3: Write 1. on input port
      dut.io.input.bits.poke(1.S)
      dut.io.input.valid.poke(true.B)
      dut.io.input.ready.expect(true.B)
      dut.io.output.valid.expect(false.B)
      dut.clock.step(1)
      dut.io.input.valid.poke(false.B)

      for (i <- 0 until (num.length + den.length)) {
        dut.io.output.valid.expect(false.B)
        dut.io.input.ready.expect(false.B)
        dut.clock.step(1)
      }

      dut.io.output.bits.expect(0.S)
      dut.io.output.valid.expect(true.B)

      dut.clock.step(1)

      // Sample 4: Write 1. on input port
      dut.io.input.bits.poke(1.S)
      dut.io.input.valid.poke(true.B)
      dut.io.input.ready.expect(true.B)
      dut.io.output.valid.expect(false.B)
      dut.clock.step(1)
      dut.io.input.valid.poke(false.B)

      for (i <- 0 until (num.length + den.length)) {
        dut.io.output.valid.expect(false.B)
        dut.io.input.ready.expect(false.B)
        dut.clock.step(1)
      }

      dut.io.output.bits.expect(3.S)
      dut.io.output.valid.expect(true.B)

      dut.clock.step(1)

      // Sample 5: Write 0. on input port
      dut.io.input.bits.poke(0.S)
      dut.io.input.valid.poke(true.B)
      dut.io.input.ready.expect(true.B)
      dut.io.output.valid.expect(false.B)
      dut.clock.step(1)
      dut.io.input.valid.poke(false.B)

      for (i <- 0 until (num.length + den.length)) {
        dut.io.output.valid.expect(false.B)
        dut.io.input.ready.expect(false.B)
        dut.clock.step(1)
      }

      dut.io.output.bits.expect(-5.S)
      dut.io.output.valid.expect(true.B)
    }
  }
}

class IIRFilterReadyTest extends AnyFlatSpec with ChiselScalatestTester {
  "IIRFilter" should "work" in {

    val inputWidth = 4
    val coefWidth = 3
    val coefDecimalWidth = 0
    val num = Seq(1, 2, 0)
    val den = Seq(0, 0)
    val outputWidth = inputWidth + coefWidth + log2Ceil(num.length + den.length) + 1

    test(
      new IIRFilter(
        inputWidth = inputWidth,
        coefWidth = coefWidth,
        coefDecimalWidth = coefDecimalWidth,
        outputWidth = outputWidth,
        numeratorNum = num.length,
        denominatorNum = den.length
      )
    ) { dut =>
      dut.io.num.poke(Vec.Lit(num.map(_.S(coefWidth.W)): _*))
      dut.io.den.poke(Vec.Lit(den.map(_.S(coefWidth.W)): _*))

      dut.io.output.ready.poke(false.B)

      // Sample 1: Write 1. on input port
      dut.io.input.bits.poke(1.S)
      dut.io.input.valid.poke(true.B)
      dut.io.input.ready.expect(true.B)
      dut.io.output.valid.expect(false.B)
      dut.clock.step(1)
      dut.io.input.valid.poke(false.B)
      dut.io.input.ready.expect(false.B)

      for (i <- 0 until (num.length + den.length)) {
        dut.io.output.valid.expect(false.B)
        dut.io.input.ready.expect(false.B)
        dut.clock.step(1)
      }

      val extraClockCycles = 10
      for (i <- 0 until extraClockCycles) {
        dut.io.output.valid.expect(true.B)
        dut.io.input.ready.expect(false.B)
        dut.clock.step(1)
      }

      dut.io.output.ready.poke(true.B)

      dut.clock.step(1)

      dut.io.output.bits.expect(1.S)
      dut.io.output.valid.expect(false.B)
      dut.io.input.ready.expect(true.B)
    }
  }
}
