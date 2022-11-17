// Author: Martin Schoeberl (martin@jopdesign.com)
// License: this code is released into the public domain, see README.md and http://unlicense.org/

package chisel.lib.fifo

import chisel3._
import chiseltest._
import firrtl.AnnotationSeq
import org.scalatest.flatspec.AnyFlatSpec

class ReplayFormal extends AnyFlatSpec with ChiselScalatestTester {
  private val defaultOptions: AnnotationSeq = Seq(WriteVcdAnnotation) //, VerilatorBackendAnnotation)

  "MemFifo" should "reply formal issue" in {
    test(new MemFifo(UInt(16.W), 4)).withAnnotations(defaultOptions) { dut =>
      dut.io.enq.bits.poke(0x1111)
      dut.io.enq.valid.poke(true.B)
      dut.io.deq.ready.poke(true.B)
      dut.clock.step()
      dut.io.enq.valid.poke(false.B)
      dut.clock.step()
      dut.io.deq.bits.expect(0x1111)
      dut.clock.step()
    }
  }
}
