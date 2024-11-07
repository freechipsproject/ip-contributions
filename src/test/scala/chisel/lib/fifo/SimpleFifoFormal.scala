// See README.md for license details.

package chisel.lib.fifo

import chisel3._
import chiseltest._
import chiseltest.formal._
import org.scalatest.flatspec.AnyFlatSpec

class SimpleFifoFormal extends AnyFlatSpec with ChiselScalatestTester with Formal {
  private val defaultOptions = Seq(BoundedCheck(10), WriteVcdAnnotation)

  "RegFifo" should "pass formal verification" in {
    verify(new RegFifo(UInt(16.W), 4), defaultOptions)
  }

  "MemFifo" should "pass formal verification" in {
    verify(new MemFifo(UInt(16.W), 4), defaultOptions)
  }
}
