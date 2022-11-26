package chisel.lib.dclib

import chisel.lib.dclib._
import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

import scala.util.Random

class CreditB2B(credit: Int, validRetime: Int = 0, creditRetime: Int = 0) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(Decoupled(UInt(16.W)))
    val deq = Decoupled(UInt(16.W))
  })
  val sender = Module(new DCCreditSender(UInt(16.W), credit))
  val receiver = Module(new DCCreditReceiver(UInt(16.W), credit))

  io.enq <> sender.io.enq
  receiver.io.enq.valid := ShiftRegister(sender.io.deq.valid, validRetime)
  receiver.io.enq.bits := ShiftRegister(sender.io.deq.bits, validRetime)
  sender.io.deq.credit := ShiftRegister(receiver.io.enq.credit, creditRetime)
  receiver.io.deq <> io.deq
}

class TestCredit extends AnyFreeSpec with ChiselScalatestTester {
  "pass data" in {
    for (rt <- 1 to 5) {
      // Adjusts amount of credit to test for full performance with different retiming
      test(new CreditB2B(5 + rt * 2, rt, rt)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        {
          c.io.enq.initSource().setSourceClock(c.clock)
          c.io.deq.initSink().setSinkClock(c.clock)

          val q = for (i <- 1 to 100) yield i.U(16.W)

          fork {
            for (i <- 1 to 100) {
              c.io.enq.ready.expect(1.B)
              c.io.enq.enqueue(i.U)
            }
          }.fork {
            c.io.deq.expectDequeueSeq(q)
          }.join()
        }
      }
    }
  }

  "start and stop randomly" in {
    for (credit <- 1 to 10) {
      test(new CreditB2B(credit)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        {
          c.io.enq.initSource().setSourceClock(c.clock)
          c.io.deq.initSink().setSinkClock(c.clock)
          val rand = new Random(1)

          val totalCount = 100
          var txCount: Int = 0
          var rxCount: Int = 0

          fork {
            while (txCount < totalCount) {
              if (rand.nextFloat() > 0.35) {
                c.clock.step(1)
              }
              c.io.enq.enqueue(txCount.U)
              txCount += 1
            }
          }.fork {
            while (rxCount < totalCount) {
              if (rand.nextFloat() > 0.35) {
                c.clock.step(1)
              }
              c.io.deq.expectDequeue(rxCount.U)
              rxCount += 1
            }
          }.join()
        }
      }
    }
  }

  "work with valid and credit retiming" in {
    for (retime <- 0 to 8) {
      test(new CreditB2B(5 + 8, retime, 8 - retime)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        {
          c.io.enq.initSource().setSourceClock(c.clock)
          c.io.deq.initSink().setSinkClock(c.clock)
          val rand = new Random(1)

          val total_count = 100
          var tx_count: Int = 0
          var rx_count: Int = 0

          fork {
            while (tx_count < total_count) {
              if (rand.nextFloat() > 0.35) {
                c.clock.step(1)
              }
              c.io.enq.enqueue(tx_count.U)
              tx_count += 1
            }
          }.fork {
            while (rx_count < total_count) {
              if (rand.nextFloat() > 0.35) {
                c.clock.step(1)
              }
              c.io.deq.expectDequeue(rx_count.U)
              rx_count += 1
            }
          }.join()
        }
      }
    }
  }
}
