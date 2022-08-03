package chisel.lib.dclib

import chisel3.util._
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

import scala.util.Random

class SerializerHarness[D <: Data](data: D, width: Int) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(new DecoupledIO(data.cloneType))
    val deq = new DecoupledIO(data.cloneType)
  })
  val ser = Module(new DCSerializer(data, width))
  val deser = Module(new DCDeserializer(data, width))

  ser.io.dataIn <> io.enq
  ser.io.dataOut <> deser.io.dataIn
  deser.io.dataOut <> io.deq
}

class TestSerializer extends AnyFreeSpec with ChiselScalatestTester {
  "start and stop randomly" in {
    for (width <- Seq(2, 3, 4, 8)) {
      test(new SerializerHarness(UInt(16.W), width)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        {
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
              val item = (tx_count * 13421) % 65535
              c.io.enq.enqueue(item.U)
              tx_count += 1
            }
          }.fork {
            while (rx_count < total_count) {
              if (rand.nextFloat() > 0.35) {
                c.clock.step(1)
              }
              val item = (rx_count * 13421) % 65535
              c.io.deq.expectDequeue(item.U)
              rx_count += 1
            }
          }.join()
        }
      }
    }
  }

}
