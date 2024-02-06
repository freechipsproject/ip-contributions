/*
 * Filter a random signal using IIRFilter module and compare with the expected output.
 *
 * See README.md for license details.
 */

package chisel.lib.iirfilter

import chisel3._
import chisel3.experimental.VecLiterals._
import chisel3.util.log2Ceil
import chiseltest._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Random

trait IIRFilterBehavior {

  this: AnyFlatSpec with ChiselScalatestTester =>

  def testFilter(
    inputWidth:        Int,
    inputDecimalWidth: Int,
    coefWidth:         Int,
    coefDecimalWidth:  Int,
    outputWidth:       Int,
    numerators:        Seq[Int],
    denominators:      Seq[Int],
    inputData:         Seq[Int],
    expectedOutput:    Seq[Double],
    precision:         Double
  ): Unit = {

    it should "work" in {
      test(
        new IIRFilter(
          inputWidth = inputWidth,
          coefWidth = coefWidth,
          coefDecimalWidth = coefDecimalWidth,
          outputWidth = outputWidth,
          numeratorNum = numerators.length,
          denominatorNum = (denominators.length - 1)
        )
      ) { dut =>
        // Set numerators and denominators
        dut.io.num.poke(Vec.Lit(numerators.map(_.S(coefWidth.W)): _*))
        dut.io.den.poke(Vec.Lit(denominators.drop(1).map(_.S(coefWidth.W)): _*))

        dut.io.output.ready.poke(true.B)

        for ((d, e) <- (inputData.zip(expectedOutput))) {

          dut.io.input.ready.expect(true.B)

          // Push input sample
          dut.io.input.bits.poke(d.S(inputWidth.W))
          dut.io.input.valid.poke(true.B)

          dut.clock.step(1)

          dut.io.input.valid.poke(false.B)

          for (i <- 0 until (numerators.length + denominators.length - 1)) {
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

class RandomSignalTest extends AnyFlatSpec with IIRFilterBehavior with ChiselScalatestTester with Matchers {

  def computeExpectedOutput(num: Seq[Double], den: Seq[Double], inputData: Seq[Double]): Seq[Double] = {
    var outputMem = Seq.fill(den.length - 1)(0.0)
    return for (i <- 0 until inputData.length) yield {
      val outputSum = (for ((d, y) <- (den.drop(1).zip(outputMem))) yield {
        d * y
      }).reduce(_ + _)

      val inputSum = (for (j <- i until math.max(i - num.length, -1) by -1) yield {
        inputData(j) * num(i - j)
      }).reduce(_ + _)

      outputMem = outputMem.dropRight(1)
      outputMem = (inputSum - outputSum) +: outputMem

      outputMem(0)
    }
  }

  behavior.of("IIRFilter")

  Random.setSeed(53297103)

  // Stable Butterworth high-pass filter
  val num = Seq(0.89194287, -2.6758286, 2.6758286, -0.89194287)
  val den = Seq(1.0, -2.77154144, 2.56843944, -0.79556205)

  // Setup data width
  val inputWidth = 16
  val inputDecimalWidth = 12

  val coefWidth = 32
  val coefDecimalWidth = 28

  val outputWidth = inputWidth + coefWidth + log2Ceil(num.length + den.length) + 1

  // Generate random input data [-1., 1.]
  val inputData = Seq.fill(100)(-1.0 + Random.nextDouble * 2.0)

  // Compute expected outputs
  val expectedOutput = computeExpectedOutput(num, den, inputData)

  // Floating point to fixed point data
  val numInt = for (n <- num) yield { (n * math.pow(2, coefDecimalWidth)).toInt }
  val denInt = for (d <- den) yield { (d * math.pow(2, coefDecimalWidth)).toInt }
  val inputDataInt = for (x <- inputData) yield (x * math.pow(2, inputDecimalWidth)).toInt

  (it should behave).like(
    testFilter(
      inputWidth,
      inputDecimalWidth,
      coefWidth,
      coefDecimalWidth,
      outputWidth,
      numInt,
      denInt,
      inputDataInt,
      expectedOutput,
      0.001
    )
  )
}
