package chisel.lib.freeset

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

/** Hardware module that loans all tags from the free set, and then gives them all back
  * Purpose is to test the FreeSet module.
  * Testcase can fail in different ways
  * 1- asserts built-in the DUT
  * 2- asserts defined here in the Test Bench
  *
  * This testbench simulates a particular parametrization of the module, state machine driven.
  * Phase Loan:   get all tags from the free set
  * Phase Check1: make sure all tags are in use and unique
  * Phase Return: put all tags back
  * Phase Check2: make sure all tags are back
  * It runs the above sequence 2x to make sure no leftover state from first iteration corrupts a next round
  */
class FreeSetTestBench(val tagCount: Int, burstSize: Int = 0) extends Module {
  object State extends ChiselEnum {
    val sLoan, sCheck1, sReturn, sCheck2 = Value
  }
  import State._

  val state = RegInit(sLoan)
  val done = IO(Output(Bool()))
  val lastTestDone = RegInit(false.B)
  done := lastTestDone
  val iterCnt = 2  // how often to cycle thru the state machine
  val testSeqCnt = RegInit((iterCnt-1).U)

  val dut = Module(new FreeSet(tagCount, burstSize))

  // If the freeset offered 1 tag, and then nothing in the next 2 cycles: it ran dry
  val freeSetEmpty = ShiftRegister(dut.io.get.valid, 3) && !ShiftRegisters(dut.io.get.valid, 2).reduce(_ || _)
  val tagsInUse = RegInit(0.U(tagCount.W))

  dut.io.get.ready := false.B
  dut.io.put.valid := false.B
  dut.io.put.bits  := PriorityEncoder(tagsInUse)

  switch(state) { // get all tags from the free set, check they are unique
    is (sLoan) {
      dut.io.get.ready := true.B
      when (dut.io.get.fire) {
        tagsInUse := tagsInUse | (1.U << dut.io.get.bits)
        assert (!tagsInUse(dut.io.get.bits), "Received same tag twice. Must be unique")
      }
      when (freeSetEmpty) { state := sCheck1 }
    }
    is (sCheck1) { // check that we have all tags
      assert (tagsInUse.andR, "Not all tags are in use")
      state := sReturn
    }
    is (sReturn) { // put everything back
      dut.io.put.valid := tagsInUse.orR
      tagsInUse := tagsInUse & ~(1.U << dut.io.put.bits)
      when (dut.io.put.valid) {
        assert(tagsInUse(dut.io.put.bits), "Testbench problem: returning tag that was not in use")
      }
      when (!tagsInUse.orR) { state := sCheck2 }
    }
    is (sCheck2) { // check that we have all tags
      testSeqCnt := testSeqCnt - 1.U
      when (testSeqCnt===0.U) { lastTestDone := true.B }
      state := sLoan
    }
  }
}

/** Simulate the FreeSet module, embedded in a testbench.
  */
class FreeSetTest extends AnyFlatSpec with ChiselScalatestTester {

  def run_test(dut: FreeSetTestBench): Unit = {
    def testLength = 2*dut.tagCount*3 + dut.tagCount // 2 iterations, 3 cycles per tag (2 for put, 1 for get) + init time
    dut.clock.setTimeout(testLength)
    dut.done.expect(false.B)
    while (!dut.done.peek().litToBoolean) { // wait for done
      dut.clock.step(1)
    }
  }

  "SimpleFreeSet" should "work" in {
    test(new FreeSetTestBench(15)) { dut => run_test(dut) }
  }
  "LargeFreeSet" should "work" in {
    test(new FreeSetTestBench(417, burstSize = 7)) { dut => run_test(dut) }
  }

}
