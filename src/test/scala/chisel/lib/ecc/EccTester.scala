// See README.md for license details.

package chisel.lib.ecc

import chisel3._
import chisel3.stage.ChiselStage
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Random

class EccTester extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("Testers2")

  it should "send data without errors" in {
    for (width <- 4 to 30) {
      test(new EccPair(width = width)) { c =>
        {
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
      test(new EccPair(width = width)) { c =>
        {
          val rnd = new Random()
          for (i <- 0 to c.getWidthParam) {
            val testVal = rnd.nextInt(1 << c.getWidthParam)

            c.io.dataIn.poke(testVal.U)
            c.io.errorLocation.poke(i.U)
            c.io.injectError.poke(true.B)
            c.io.injectSecondError.poke(false.B)
            c.io.injectEccError.poke(false.B)
            c.clock.step(1)
            c.io.dataOut.expect(testVal.U)
            c.io.outputNotEqual.expect(false.B)
            c.io.doubleBitError.expect(false.B)
          }
        }
      }
    }
  }

  it should "correct single-bit ecc errors" in {
    for (width <- 4 to 30) {
      test(new EccPair(width = width)) { c =>
        {
          val rnd = new Random()
          for (i <- 0 to calcCodeBits(width)) {
            val testVal = rnd.nextInt(1 << c.getWidthParam)

            c.io.dataIn.poke(testVal.U)
            c.io.eccErrorLocation.poke(i.U)
            c.io.injectError.poke(false.B)
            c.io.injectSecondError.poke(false.B)
            c.io.injectEccError.poke(true.B)
            c.clock.step(1)
            c.io.dataOut.expect(testVal.U)
            c.io.outputNotEqual.expect(false.B)
            c.io.doubleBitError.expect(false.B)
          }
        }
      }
    }
  }

  it should "support wide values" in {
    for (width <- Seq(48, 64, 96, 128, 256)) {
      test(new EccPair(width = width)) { c =>
        {
          val rnd = new Random()
          for (i <- 0 to c.getWidthParam) {
            val testVal = rnd.nextInt(256)

            c.io.dataIn.poke(testVal.U)
            c.io.errorLocation.poke(i.U)
            c.io.injectError.poke(true.B)
            c.clock.step(1)
            c.io.outputNotEqual.expect(false.B)
            c.io.doubleBitError.expect(false.B)
          }
          for (i <- 0 to calcCodeBits(width)) {
            c.io.injectError.poke(false.B)
            c.io.injectEccError.poke(true.B)
            c.io.eccErrorLocation.poke(i.U)
            c.clock.step(1)
            c.io.outputNotEqual.expect(false.B)
            c.io.doubleBitError.expect(false.B)
          }
        }
      }
    }
  }

  it should "detect double bit errors" in {
    for (width <- 4 to 30) {
      test(new EccPair(width = width)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        {
          val rnd = new Random()
          for (i <- 0 to c.getWidthParam) {
            val testVal = rnd.nextInt(1 << c.getWidthParam)

            c.io.dataIn.poke(testVal.U)
            c.io.errorLocation.poke((i % c.getWidthParam).U)
            c.io.injectError.poke(true.B)
            c.io.injectSecondError.poke(true.B)
            c.io.injectEccError.poke(false.B)
            c.io.secondErrorLocation.poke(((i + 1) % c.getWidthParam).U)
            c.clock.step(1)
            c.io.outputNotEqual.expect(true.B)
            c.io.doubleBitError.expect(true.B)
          }
        }
      }
    }
  }

  it should "support functional inference" in {
    test(new Module {
      val io = IO(new Bundle {
        val dataIn = Input(UInt(16.W))
        val dataOut = Output(UInt(16.W))
        val doubleBitError = Output(Bool())
      })
      val eccOutput = EccCheck(EccGenerate(io.dataIn))
      io.dataOut := eccOutput.data
      io.doubleBitError := eccOutput.par
    }) { c =>
      for (i <- 0 to 32) {
        c.io.dataIn.poke(i.U)
        c.clock.step(1)
        c.io.dataOut.expect(i.U)
        c.io.doubleBitError.expect(false.B)
      }
    }
  }
}

object EccGenerator extends App {
  (new ChiselStage).emitSystemVerilog(new EccCheck(UInt(8.W)), Array("--target-dir", "generated"))
}
