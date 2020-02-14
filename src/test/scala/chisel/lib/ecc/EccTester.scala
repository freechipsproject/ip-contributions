// See README.md for license details.

package chisel.lib.ecc

import chisel3._
import chiseltest._
import org.scalatest._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.WriteVcdAnnotation

import scala.util.Random

class EccTester extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Testers2"

  it should "send data without errors" in {
    for (width <- 4 to 30) {
      test(new EccPair(width = width)) {
        c => {
          val rnd = new Random()
          for (i <- 0 to 20) {
            val testVal = rnd.nextInt(1 << c.getWidthParam)

            c.io.dataIn.poke(testVal.U)
            c.io.errorLocation.poke(0.U)
            c.io.injectError.poke(false.B)
            c.io.injectSecondError.poke(false.B)
            c.clock.step(1)
            c.io.dataOut.expect(testVal.U)
            c.io.outputNotEqual.expect(false.B)
            c.io.doubleBitError.expect(false.B)
          }
        }
      }
    }
  }

  it should "correct single bit errors" in {
    for (width <- 4 to 30) {
      test(new EccPair(width = width)) {
        c => {
          val rnd = new Random()
          for (i <- 0 to c.getWidthParam) {
            val testVal = rnd.nextInt(1 << c.getWidthParam)

            c.io.dataIn.poke(testVal.U)
            c.io.errorLocation.poke(i.U)
            c.io.injectError.poke(true.B)
            c.io.injectSecondError.poke(false.B)
            c.clock.step(1)
            c.io.dataOut.expect(testVal.U)
            c.io.outputNotEqual.expect(false.B)
            c.io.doubleBitError.expect(false.B)
          }
        }
      }
    }
  }

  it should "detect double bit errors" in {
    for (width <- 4 to 30) {
      test(new EccPair(width = width)).withAnnotations(Seq(WriteVcdAnnotation)) {
        c => {
          val rnd = new Random()
          for (i <- 0 to c.getWidthParam) {
            val testVal = rnd.nextInt(1 << c.getWidthParam)

            c.io.dataIn.poke(testVal.U)
            c.io.errorLocation.poke((i % c.getWidthParam).U)
            c.io.injectError.poke(true.B)
            c.io.injectSecondError.poke(true.B)
            c.io.secondErrorLocation.poke(((i + 1) % c.getWidthParam).U)
            c.clock.step(1)
            c.io.outputNotEqual.expect(true.B)
            c.io.doubleBitError.expect(true.B)
          }
        }
      }
    }
  }
}

object EccGenerator extends App {
  chisel3.Driver.execute(Array("--target-dir", "generated"), () => new EccCheck(UInt(8.W)))
}