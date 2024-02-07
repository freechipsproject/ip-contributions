/*
 * Filter a random signal using FIRFilter module and compare with the expected output.
 *
 * See README.md for license details.
 */

package chisel.lib.firfilter

import chisel3._
import chisel3.experimental.VecLiterals._
import chisel3.util.log2Ceil
import chiseltest._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Random

trait FIRFilterBehavior {

  this: AnyFlatSpec with ChiselScalatestTester =>

  def testFilter(
    inputWidth:        Int,
    inputDecimalWidth: Int,
    coefWidth:         Int,
    coefDecimalWidth:  Int,
    coefs:             Seq[Int],
    inputData:         Seq[Int],
    expectedOutput:    Seq[Double],
    precision:         Double
  ): Unit = {

    it should "work" in {
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

        for ((d, e) <- (inputData.zip(expectedOutput))) {

          dut.io.input.ready.expect(true.B)

          // Push input sample
          dut.io.input.bits.poke(d.S(inputWidth.W))
          dut.io.input.valid.poke(true.B)

          dut.clock.step(1)

          dut.io.input.valid.poke(false.B)

          for (i <- 0 until coefs.length) {
            dut.io.output.valid.expect(false.B)
            dut.io.input.ready.expect(false.B)
            dut.clock.step(1)
          }

          // Check output
          val outputDecimalWidth = coefDecimalWidth + inputDecimalWidth
          val output = dut.io.output.bits.peek().litValue.toFloat / math.pow(2, outputDecimalWidth).toFloat
          val upperBound = e + precision
          val lowerBound = e - precision

          assert(output < upperBound)
          assert(output > lowerBound)

          dut.io.output.valid.expect(true.B)

          dut.clock.step(1)
        }
      }
    }
  }
}

class RandomSignalTest extends AnyFlatSpec with FIRFilterBehavior with ChiselScalatestTester with Matchers {

  def computeExpectedOutput(coefs: Seq[Double], inputData: Seq[Double]): Seq[Double] = {
    return for (i <- 0 until inputData.length) yield {
      val inputSum = (for (j <- i until math.max(i - coefs.length, -1) by -1) yield {
        inputData(j) * coefs(i - j)
      }).reduce(_ + _)

      inputSum
    }
  }

  behavior.of("FIRFilter")

  Random.setSeed(11340702)

  // 9 taps Kaiser high-pass filter 50Hz (sampling freq: 44.1kHz)
  val coefs = Seq(-0.00227242, -0.00227255, -0.00227265, -0.00227271, 0.99999962, -0.00227271, -0.00227265, -0.00227255,
    -0.00227242)

  // Setup data width
  val inputWidth = 16
  val inputDecimalWidth = 12

  val coefWidth = 32
  val coefDecimalWidth = 28

  // Generate random input data [-1., 1.]
  val inputData = Seq.fill(100)(-1.0 + Random.nextDouble * 2.0)

  // Compute expected outputs
  val expectedOutput = computeExpectedOutput(coefs, inputData)

  // Floating point to fixed point data
  val coefsInt = for (n <- coefs) yield { (n * math.pow(2, coefDecimalWidth)).toInt }
  val inputDataInt = for (x <- inputData) yield (x * math.pow(2, inputDecimalWidth)).toInt

  (it should behave).like(
    testFilter(
      inputWidth,
      inputDecimalWidth,
      coefWidth,
      coefDecimalWidth,
      coefsInt,
      inputDataInt,
      expectedOutput,
      0.0005
    )
  )
}
