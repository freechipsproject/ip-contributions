/*
 * A very simple test collection for FIRFilter module.
 *
 * See README.md for license details.
 */

package chisel.lib.firfilter

import chisel3._
import chisel3.experimental.VecLiterals._
import chisel3.util.log2Ceil
import chiseltest._

import org.scalatest.flatspec.AnyFlatSpec

class FIRFilterCoefTest extends AnyFlatSpec with ChiselScalatestTester {
  "FIRFilter coef" should "work" in {

    val inputWidth = 4
    val coefWidth = 3
    val coefDecimalWidth = 0
    val coefs = Seq(2, 1, 0, 3)

    test(
      new FIRFilter(
        inputWidth = inputWidth,
        coefWidth = coefWidth,
        coefDecimalWidth = coefDecimalWidth,
        coefNum = coefs.length
      )
    ) { dut =>
      dut.io.coef.poke(Vec.Lit(coefs.map(_.S(coefWidth.W)): _*))

      dut.io.output.ready.poke(true.B)

      // Sample 1: Write 1. on input port
      dut.io.input.bits.poke(1.S)
      dut.io.input.valid.poke(true.B)
      dut.io.input.ready.expect(true.B)
      dut.io.output.valid.expect(false.B)
      dut.clock.step(1)
      dut.io.input.valid.poke(false.B)
      dut.io.input.ready.expect(false.B)

      for (i <- 0 until coefs.length) {
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

      for (i <- 0 until coefs.length) {
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

      for (i <- 0 until coefs.length) {
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

      for (i <- 0 until coefs.length) {
        dut.io.output.valid.expect(false.B)
        dut.io.input.ready.expect(false.B)
        dut.clock.step(1)
      }

      dut.io.output.bits.expect(3.S)
      dut.io.output.valid.expect(true.B)
    }
  }
}

class FIRFilterReadyTest extends AnyFlatSpec with ChiselScalatestTester {
  "FIRFilter" should "work" in {

    val inputWidth = 4
    val coefWidth = 3
    val coefDecimalWidth = 0
    val coefs = Seq(1, 2, 0)

    test(
      new FIRFilter(
        inputWidth = inputWidth,
        coefWidth = coefWidth,
        coefDecimalWidth = coefDecimalWidth,
        coefNum = coefs.length
      )
    ) { dut =>
      dut.io.coef.poke(Vec.Lit(coefs.map(_.S(coefWidth.W)): _*))

      dut.io.output.ready.poke(false.B)

      // Sample 1: Write 1. on input port
      dut.io.input.bits.poke(1.S)
      dut.io.input.valid.poke(true.B)
      dut.io.input.ready.expect(true.B)
      dut.io.output.valid.expect(false.B)
      dut.clock.step(1)
      dut.io.input.valid.poke(false.B)
      dut.io.input.ready.expect(false.B)

      for (i <- 0 until coefs.length) {
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
