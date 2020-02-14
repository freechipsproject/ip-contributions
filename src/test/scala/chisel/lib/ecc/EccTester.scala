// See README.md for license details.

package chisel.lib.ecc

import chisel3._
import chiseltest._
import org.scalatest._
//import chisel3.iotesters.PeekPokeTester

import scala.util.Random

class EccTester extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Testers2"

  it should "send data without errors" in {
    test(new EccPair(width=8)) {
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
        }
      }
    }
  }

  it should "correct single bit errors" in {
    test(new EccPair(width=8)) {
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
        }
      }
    }
  }

  it should "correct double bit errors" in {
    test(new EccPair(width=8)) {
      c => {
        val rnd = new Random()
        for (i <- 0 to c.getWidthParam) {
          val testVal = rnd.nextInt(1 << c.getWidthParam)

          c.io.dataIn.poke(testVal.U)
          c.io.errorLocation.poke(i.U)
          c.io.injectError.poke(true.B)
          c.io.injectSecondError.poke(true.B)
          c.io.errorLocation.poke(((i+1)%c.getWidthParam).U)
          c.clock.step(1)
          c.io.outputNotEqual.expect(true.B)
        }
      }
    }
  }
}
/*
class EccTester(dut: EccPair) extends PeekPokeTester(dut) {
  val dutWidth = dut.getWidthParam
  var testVal : Int = 0
  // send through some data without errors
  for (i <- 0 to 20) {
    testVal = rnd.nextInt() & ((1 << dutWidth) - 1)

    poke(dut.io.dataIn, testVal.U)
    poke(dut.io.errorLocation, 0.U)
    poke(dut.io.injectError, 0.U)
    step(1)
    expect(dut.io.dataOut, testVal.U)
  }

  // inject single bit errors
  for (i <- 0 to dutWidth) {
    testVal = rnd.nextInt() & ((1 << dutWidth) - 1)

    poke(dut.io.errorLocation, i.U)
    poke(dut.io.injectError, 1.U)
    poke(dut.io.dataIn, testVal.U)
    step(1)
    expect(dut.io.dataOut, testVal.U)
  }
}
*/

object EccTester extends App {
//  for (width <- 8 to 32) {
//    iotesters.Driver.execute(Array("--target-dir", "generated", "--generate-vcd-output", "on"), () => new EccPair(width=width)) {
//      dut => new EccTester(dut)
//    }
//  }
}

object EccGenerator extends App {
  chisel3.Driver.execute(Array("--target-dir", "generated"), () => new EccCheck(UInt(8.W)))
}