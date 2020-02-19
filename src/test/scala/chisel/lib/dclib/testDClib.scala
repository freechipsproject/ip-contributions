/**
 *
 */
package chisel.lib.dclib

import chisel3._
import chiseltest._
import org.scalatest._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.WriteVcdAnnotation

class DclibTester extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Testers2"

  it should "test input output" in {
    test(new DCInputOutputTestbench()) {
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

  it should "test hold" in {
    test(new DCHoldTestbench) {
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
