package chisel.lib.dclib

import chisel3._
import chisel3.util._
import chisel.lib.dclib.DCFull
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

import scala.util.Random

class TestDcFull extends AnyFreeSpec with ChiselScalatestTester {

  "pass data" in {
    test(new DCFull(UInt(16.W))).withAnnotations(Seq(WriteVcdAnnotation)) {
      c => {
        c.io.enq.initSource().setSourceClock(c.clock)
        c.io.deq.initSink().setSinkClock(c.clock)

        val q = for (i <- 1 to 100) yield i.U(16.W)
        //val q = Seq(10.U, 20.U)

        fork {
          c.io.enq.enqueueSeq(q)
        }.fork {
          c.io.deq.expectDequeueSeq(q)
        }.join()
      }
    }
  }

  "work with flow control" in {
    test(new DCFull(UInt(16.W))).withAnnotations(Seq(WriteVcdAnnotation)) {
      c => {
        c.io.enq.valid.poke(1.B)
        c.io.enq.bits.poke(0x11.U)
        c.io.enq.ready.expect(true.B)
        c.clock.step(1)
        c.io.enq.ready.expect(true.B)
        c.io.enq.bits.poke(0x22.U)

        c.io.deq.valid.expect(true.B)
        c.io.deq.bits.expect(0x11.U)
        c.clock.step(1)
        c.io.enq.valid.poke(0.B)
        c.io.enq.ready.expect(false.B)

        c.io.deq.valid.expect(true.B)
        c.io.deq.bits.expect(0x11.U)

        c.io.deq.ready.poke(true.B)
        c.clock.step(1)
        c.io.enq.ready.expect(true.B)

        c.io.deq.valid.expect(true.B)
        c.io.deq.bits.expect(0x22.U)

        c.clock.step(1)
        c.io.enq.ready.expect(true.B)
        c.io.deq.valid.expect(false.B)


      }
    }
  }

  "start and stop" in {
    test(new DCFull(UInt(16.W))).withAnnotations(Seq(WriteVcdAnnotation)) {
      c => {
        c.io.enq.initSource().setSourceClock(c.clock)
        c.io.deq.initSink().setSinkClock(c.clock)

        c.io.enq.enqueue(0x1122.U)
        c.io.enq.enqueue(0x3345.U)


        fork {
          c.clock.step(2)
          c.io.enq.enqueue(0x7432.U(16.W))
          c.io.enq.enqueue(0x9988.U)
          c.clock.step(3)
          c.io.enq.enqueueSeq(Seq(0x1111.U, 0x2222.U, 0x3333.U))
        }.fork {
          c.io.deq.expectDequeueSeq(Seq(0x1122.U, 0x3345.U, 0x7432.U, 0x9988.U))
          c.clock.step(4)
          c.io.deq.expectDequeue(0x1111.U)
          c.clock.step(1)
          c.io.deq.expectDequeueSeq(Seq(0x2222.U, 0x3333.U))
        }.join()
      }
    }
  }

  "start and stop randomly" in {
    test(new DCFull(UInt(16.W))).withAnnotations(Seq(WriteVcdAnnotation)) {
      c => {
        c.io.enq.initSource().setSourceClock(c.clock)
        c.io.deq.initSink().setSinkClock(c.clock)
        val rand = new Random(1)

        val total_count = 250
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
