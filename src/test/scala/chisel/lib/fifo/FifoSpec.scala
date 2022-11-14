// Author: Martin Schoeberl (martin@jopdesign.com)
// License: this code is released into the public domain, see README.md and http://unlicense.org/

package chisel.lib.fifo

import chisel3._
import chiseltest._
import firrtl.AnnotationSeq
import org.scalatest.flatspec.AnyFlatSpec

/**
  * Testing FIFO queue variations
  */
object testFifo {

  private def initIO(dut: Fifo[UInt]): Unit = {
    dut.io.enq.bits.poke(0xab.U)
    dut.io.enq.valid.poke(false.B)
    dut.io.deq.ready.poke(false.B)
  }

  private def push(dut: Fifo[UInt], value: BigInt): Unit = {
    dut.io.enq.bits.poke(value.U)
    dut.io.enq.valid.poke(true.B)
    // wait for slot to become available
    while (!dut.io.enq.ready.peekBoolean) {
      dut.clock.step()
    }
    dut.clock.step()
    dut.io.enq.bits.poke(0xab.U) // overwrite with default value
    dut.io.enq.valid.poke(false.B)
  }

  private def pop(dut: Fifo[UInt]): BigInt = {
    // wait for value to become available
    while (!dut.io.deq.valid.peekBoolean) {
      dut.clock.step()
    }
    // check value
    dut.io.deq.valid.expect(true.B)
    val value = dut.io.deq.bits.peekInt()
    // read it out
    dut.io.deq.ready.poke(true.B)
    dut.clock.step()
    dut.io.deq.ready.poke(false.B)
    value
  }

  private def pop(dut: Fifo[UInt], value: BigInt): Unit = {
    // wait for value to become available
    while (!dut.io.deq.valid.peekBoolean) {
      dut.clock.step()
    }
    // check value
    dut.io.deq.valid.expect(true.B)
    dut.io.deq.bits.expect(value.U)
    // read it out
    dut.io.deq.ready.poke(true.B)
    dut.clock.step()
    dut.io.deq.ready.poke(false.B)
  }

  private def reset(dut: Fifo[UInt]) = {
    dut.reset.poke(true.B)
    dut.clock.step()
    dut.reset.poke(false.B)
    dut.clock.step()
  }

  private def speedTest(dut: Fifo[UInt]): (Int, Double) = {
    dut.io.enq.valid.poke(true.B)
    dut.io.deq.ready.poke(true.B)
    var cnt = 0
    for (i <- 0 until 100) {
      dut.io.enq.bits.poke(i.U)
      if (dut.io.enq.ready.peekBoolean) {
        cnt += 1
      }
      dut.clock.step()
    }
    initIO(dut)
    reset(dut)
    dut.io.deq.valid.expect(false.B)
    (cnt, 100.0 / cnt)
  }

  private def threadedTest(dut: Fifo[UInt]): (Int, Double) = {
    initIO(dut)
    val writer = fork {
      for (i <- 0 until 100) {
        push(dut, i)
      }
    }

    for (i <- 0 until 100) {
      assert(pop(dut) == i)
    }
    writer.join()

    (1, 1)
  }

  def apply(dut: Fifo[UInt], expectedCyclesPerWord: Int = 1): Unit = {
    // some defaults for all signals
    initIO(dut)
    dut.clock.step()

    // write one value and expect it on the deq side
    push(dut, 0x123)
    dut.clock.step(12)
    dut.io.enq.ready.expect(true.B)
    pop(dut, 0x123)
    dut.io.deq.valid.expect(false.B)
    dut.clock.step()

    // file the whole buffer
    (0 until dut.depth).foreach { i => push(dut, i + 1) }

    dut.clock.step()
    // dut.io.enq.ready.expect(false.B, "fifo should be full")
    dut.io.deq.valid.expect(true.B, "fifo should have data available to dequeue")
    dut.io.deq.bits.expect(1.U, "the first entry should be 1")

    // now read it back
    (0 until dut.depth).foreach { i => pop(dut, i + 1) }

    // Do the speed test
    val (cnt, cycles) = speedTest(dut)
    assert(cycles == expectedCyclesPerWord)
    // println(s"$cnt words in 100 clock cycles, $cycles clock cycles per word")
    assert(cycles >= 0.99, "Cannot be faster than one clock cycle per word")

    // Do the threaded test
    threadedTest(dut)
  }
}

class FifoSpec extends AnyFlatSpec with ChiselScalatestTester {
  private val defaultOptions: AnnotationSeq = Seq(WriteVcdAnnotation)

  "BubbleFifo" should "pass" in {
    test(new BubbleFifo(UInt(16.W), 4)).withAnnotations(defaultOptions)(testFifo(_, 2))
  }

  "DoubleBufferFifo" should "pass" in {
    test(new DoubleBufferFifo(UInt(16.W), 4)).withAnnotations(defaultOptions)(testFifo(_))
  }

  "RegFifo" should "pass" in {
    test(new RegFifo(UInt(16.W), 4)).withAnnotations(defaultOptions)(testFifo(_))
  }

  "MemFifo" should "pass" in {
    test(new MemFifo(UInt(16.W), 4)).withAnnotations(defaultOptions)(testFifo(_))
  }

  "CombFifo" should "pass" in {
    test(new CombFifo(UInt(16.W), 4)).withAnnotations(defaultOptions)(testFifo(_))
  }

}
