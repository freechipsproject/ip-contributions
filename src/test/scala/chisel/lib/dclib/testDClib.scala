/**
 *
 */
package chisel.lib.dclib

import chisel3._
import chiseltest._
import org.scalatest._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.WriteVcdAnnotation

/**
 * Test behavior of dclib components
 *
 * Test operation of each component across a range of widths, and under different flow control conditions.
 * The ColorSource and ColorSink implement sequence generators and checkers.  Each generator and checker
 * takes a flow control pattern with which to assert either source flow control (Valid) or destination flow
 * control (ready).
 */
class DclibTester extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Testers2"

  it should "test input output" in {
    for (width <- 4 to 32) {
      //test(new DCInputOutputTestbench(width)).withAnnotations(Seq(WriteVcdAnnotation)) {
      test(new DCInputOutputTestbench(width)) {
        c => {
          c.io.srcPat.poke(0xFFFF.U)
          c.io.dstPat.poke(0xFFFF.U)
          c.clock.step(101)
          c.io.okCount.expect(100.U) // Should produce valid token every cycle

          // try a couple other flow control patterns
          c.io.srcPat.poke(0xF000.U)
          c.io.dstPat.poke(0xC0A0.U)
          c.clock.step(50)

          c.io.srcPat.poke(0xAA55.U)
          c.io.dstPat.poke(0xF00F.U)
          c.clock.step(50)

          c.io.colorError.expect(false.B)
          c.io.seqError.expect(false.B)
        }
      }
    }
  }

  it should "test hold" in {
    for (width <- 4 to 32) {
      test(new DCHoldTestbench(width)) {
        c => {
          c.io.srcPat.poke(0xFFFF.U)
          c.io.dstPat.poke(0xFFFF.U)
          c.clock.step(101)
          c.io.okCount.expect(50.U) // Should produce valid token every other cycle

          // try a couple other flow control patterns
          c.io.srcPat.poke(0xF000.U)
          c.io.dstPat.poke(0xC0A0.U)
          c.clock.step(50)

          c.io.srcPat.poke(0xAA55.U)
          c.io.dstPat.poke(0xF00F.U)
          c.clock.step(50)

          c.io.colorError.expect(false.B)
          c.io.seqError.expect(false.B)
        }
      }
    }
  }

  it should "test arbiter" in {
    for (ways <- 2 until 8) {
      test(new ArbMirrorTestbench(ways = ways)) {
        c => {
          c.io.srcPat.poke(0xFFFF.U)
          c.io.dstPat.poke(0xFFFF.U)
          c.clock.step(100)

          // try a couple other flow control patterns
          c.io.srcPat.poke(0xF000.U)
          c.io.dstPat.poke(0xC0A0.U)
          c.clock.step(50)

          c.io.srcPat.poke(0xAA55.U)
          c.io.dstPat.poke(0xF00F.U)
          c.clock.step(50)

          c.io.colorError.expect(false.B)
          c.io.seqError.expect(false.B)
        }
      }
    }
  }
}

class OperatorTester extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Testers2 with Queue"

  it should "add numbers together" in {
    def add(a: UInt, b: UInt) : UInt = a + b

    for (n <- 2 to 6) {
      test(new DCOperator(n, 8, add)).withAnnotations(Seq(WriteVcdAnnotation)) {
        c => {
          // clock source setup
          for (i <- 0 until n) {
            c.io.a(i).initSource().setSourceClock(c.clock)
          }
          c.io.z.initSink().setSinkClock(c.clock)

          // enqueue numbers serially to test interlock
          for (i <- 0 until n) {
            c.io.a(i).enqueueNow(8.U)
          }
          c.io.z.expectDequeueNow((n*8).U)

          // enqueue numbers in parallel
          for (i <- 0 until n) {
            fork {
              c.io.a(i).enqueueSeq(Seq(6.U, 7.U, 8.U, 9.U))
            }
          }

          c.io.z.expectDequeueSeq(Seq((n*6).U, (n*7).U, (n*8).U, (n*9).U))
        }
      }
    }
  }
}