/**
  */
package chisel.lib.dclib

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

/**
  * Test behavior of dclib components
  *
  * Test operation of each component across a range of widths, and under different flow control conditions.
  * The ColorSource and ColorSink implement sequence generators and checkers.  Each generator and checker
  * takes a flow control pattern with which to assert either source flow control (Valid) or destination flow
  * control (ready).
  */
class DclibTester extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("Testers2")

  it should "test input output" in {
    for (width <- 4 to 32) {
      test(new DCInputOutputTestbench(width)) { c =>
        {
          c.io.srcPat.poke(0xffff.U)
          c.io.dstPat.poke(0xffff.U)
          c.clock.step(101)
          c.io.okCount.expect(100.U) // Should produce valid token every cycle

          // try a couple other flow control patterns
          c.io.srcPat.poke(0xf000.U)
          c.io.dstPat.poke(0xc0a0.U)
          c.clock.step(50)

          c.io.srcPat.poke(0xaa55.U)
          c.io.dstPat.poke(0xf00f.U)
          c.clock.step(50)

          c.io.colorError.expect(false.B)
          c.io.seqError.expect(false.B)
        }
      }
    }
  }

  it should "test hold" in {
    for (width <- 4 to 32) {
      test(new DCHoldTestbench(width)) { c =>
        {
          c.io.srcPat.poke(0xffff.U)
          c.io.dstPat.poke(0xffff.U)
          c.clock.step(101)
          c.io.okCount.expect(50.U) // Should produce valid token every other cycle

          // try a couple other flow control patterns
          c.io.srcPat.poke(0xf000.U)
          c.io.dstPat.poke(0xc0a0.U)
          c.clock.step(50)

          c.io.srcPat.poke(0xaa55.U)
          c.io.dstPat.poke(0xf00f.U)
          c.clock.step(50)

          c.io.colorError.expect(false.B)
          c.io.seqError.expect(false.B)
        }
      }
    }
  }

  it should "test arbiter" in {
    for (ways <- 2 until 8) {
      test(new ArbMirrorTestbench(ways = ways)) { c =>
        {
          c.io.srcPat.poke(0xffff.U)
          c.io.dstPat.poke(0xffff.U)
          c.clock.step(100)

          // try a couple other flow control patterns
          c.io.srcPat.poke(0xf000.U)
          c.io.dstPat.poke(0xc0a0.U)
          c.clock.step(50)

          c.io.srcPat.poke(0xaa55.U)
          c.io.dstPat.poke(0xf00f.U)
          c.clock.step(50)

          c.io.colorError.expect(false.B)
          c.io.seqError.expect(false.B)
        }
      }
    }
  }
}

class ReductionTester extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("Testers2 with Queue")

  it should "add numbers together" in {
    def add(a: UInt, b: UInt): UInt = a + b

    for (n <- 2 to 6) {
      test(new DCReduce(UInt(8.W), n, add)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        {
          // clock source setup
          for (i <- 0 until n) {
            c.io.a(i).initSource().setSourceClock(c.clock)
          }
          c.io.z.initSink().setSinkClock(c.clock)

          // enqueue numbers serially to test interlock
          for (i <- 0 until n) {
            c.io.a(i).enqueueNow(8.U)
          }
          c.io.z.expectDequeueNow((n * 8).U)

          // enqueue numbers in parallel
          for (i <- 0 until n) {
            fork {
              c.io.a(i).enqueueSeq(Seq(6.U, 7.U, 8.U, 9.U))
            }
          }

          c.io.z.expectDequeueSeq(Seq((n * 6).U, (n * 7).U, (n * 8).U, (n * 9).U))
        }
      }
    }
  }

  /**
    * This tests basic connectivity through the crossbar, but does not test performance
    * due to the serial checking of the expectDequeueNow statement.  Attempts to use
    * fork() to make this parallel give error messages.
    */
  it should "sort numbers in a crossbar" in {
    test(new DCCrossbar(UInt(8.W), 3, 3)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      {
        for (i <- 0 to 2) {
          c.io.c(i).initSource().setSourceClock(c.clock)
          c.io.p(i).initSink().setSinkClock(c.clock)
          c.io.sel(i).poke(((i + 1) % 3).U)
        }
        for (i <- 0 to 2) {
          val sendSeq = for (j <- 0 to 2) yield j.U(8.W)
          fork {
            c.io.c(i).enqueueSeq(sendSeq)
          }
        }
        for (i <- 0 to 2) {
          for (j <- 0 to 2) {
            c.io.p(j).expectDequeueNow(i.U)
          }
          //val expSeq = for (j <- 0 to 2) yield i.U(8.W)
          //c.io.p(i).expectDequeueSeq(expSeq)
        }
      }
    }
  }
}
