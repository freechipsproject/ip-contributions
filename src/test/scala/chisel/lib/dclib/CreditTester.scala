package chisel.lib.dclib

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class CreditHarness(delay : Int, maxCredit : Int) extends Module {
  val io = IO(new Bundle {
    val dataIn = Flipped(Decoupled(UInt(8.W)))
    val dataOut = Decoupled(UInt(8.W))
    val backpressured = Output(Bool())
  })
  val sender = Module(new DCCreditSender(io.dataIn.bits, maxCredit))
  val receiver = Module(new DCCreditReceiver(io.dataIn.bits, maxCredit))
  val backpressured = RegInit(false.B)

  if (delay >= 1) {
    val delayPipe = Wire(Vec(delay, new CreditIO(io.dataIn.bits)))
    sender.io.deq.credit := RegNext(delayPipe(0).credit)
    delayPipe(0).valid := RegNext(sender.io.deq.valid)
    delayPipe(0).bits := RegNext(sender.io.deq.bits)
    receiver.io.enq.valid := RegNext(delayPipe(delay-1).valid)
    receiver.io.enq.bits := RegNext(delayPipe(delay-1).bits)
    delayPipe(delay-1).credit := RegNext(receiver.io.enq.credit)

    if (delay >= 2) {
      for (i <- 0 to delay - 2) {
        delayPipe(i).credit := RegNext(delayPipe(i + 1).credit)
        delayPipe(i + 1).valid := RegNext(delayPipe(i).valid)
        delayPipe(i + 1).bits := RegNext(delayPipe(i).bits)
      }
    }
  } else {
    sender.io.deq <> receiver.io.enq
  }

  when (io.dataIn.valid && !io.dataIn.ready) {
    backpressured := true.B
  }
  io.dataIn <> sender.io.enq
  DCOutput(receiver.io.deq) <> io.dataOut
  io.backpressured := backpressured
}

class CreditTester extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Testers2 with Queue"

  it should "test fixed credit with variable delay" in {
    for (n <- 0 to 5) {
      test(new CreditHarness(n, 4)).withAnnotations(Seq(WriteVcdAnnotation)) {
        c => {
          c.io.dataIn.initSource().setSourceClock(c.clock)
          c.io.dataOut.initSink().setSinkClock(c.clock)

          val dataPattern = for (i <- 1 to 50) yield i.U
          fork {
            c.io.dataIn.enqueueSeq(dataPattern)
          }
          c.io.dataOut.expectDequeueSeq(dataPattern)
        }
      }
    }
  }

  it should "not flow control with sufficient credit" in {
    for (n <- 4 to 10) {
      test(new CreditHarness(n, n*3+6)).withAnnotations(Seq(WriteVcdAnnotation)) {
        c => {
          c.io.dataIn.initSource().setSourceClock(c.clock)
          c.io.dataOut.initSink().setSinkClock(c.clock)

          val dataPattern = for (i <- 1 to n*5) yield i.U
          fork {
            c.io.dataIn.enqueueSeq(dataPattern)
          }
          c.io.dataOut.expectDequeueSeq(dataPattern)

          // check that sender never asserted backpressure during test
          c.io.backpressured.expect(false.B)
        }
      }
    }
  }

  it should "flow control with small credit" in {
    for (n <- 2 to 10) {
      test(new CreditHarness(n, 2)).withAnnotations(Seq(WriteVcdAnnotation)) {
        c => {
          c.io.dataIn.initSource().setSourceClock(c.clock)
          c.io.dataOut.initSink().setSinkClock(c.clock)

          val dataPattern = for (i <- 1 to n*5) yield i.U
          fork {
            c.io.dataIn.enqueueSeq(dataPattern)
          }
          c.io.dataOut.expectDequeueSeq(dataPattern)
          c.io.backpressured.expect(true.B)
        }
      }
    }
  }
}
