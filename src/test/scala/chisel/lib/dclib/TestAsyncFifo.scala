package chisel.lib.dclib

import chisel3._
import chisel3.util.DecoupledIO
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

import scala.util.Random

class AsyncWrap[D <: Data](data: D, depth : Int) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(new DecoupledIO(data.cloneType))
    val deq = new DecoupledIO(data.cloneType)
  })

  val af = Module(new DCAsyncFifo(data, depth))
  af.io.enq_clock := clock
  af.io.enq_reset := reset
  af.io.deq_clock := clock
  af.io.deq_reset := reset

  af.io.enq <> io.enq
  af.io.deq <> io.deq
}

class TestAsyncFifo  extends AnyFreeSpec with ChiselScalatestTester{
  "start and stop randomly" in {
    test(new AsyncWrap(UInt(16.W), 8)).withAnnotations(Seq(WriteVcdAnnotation)) {
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
            if (rand.nextFloat() > 0.65) {
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
