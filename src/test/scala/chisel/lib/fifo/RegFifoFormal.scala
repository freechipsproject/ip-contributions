// See README.md for license details.

package chisel.lib.fifo

import chisel3._
import chisel3.util._
import chiseltest._
import chiseltest.formal._
import firrtl.AnnotationSeq
import org.scalatest.flatspec.AnyFlatSpec

class RegFifoFormal extends AnyFlatSpec with ChiselScalatestTester with Formal {
  private val defaultOptions: AnnotationSeq = Seq(BoundedCheck(10), WriteVcdAnnotation)

  "RegFifo" should "pass formal verification" in {
    verify(new RegFifo(UInt(16.W), 4), defaultOptions)
  }
}
