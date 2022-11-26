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

          val totalCount = 250
          var txCount: Int = 0
          var rxCount: Int = 0

          fork {
            while (txCount < totalCount) {
              if (rand.nextFloat() > 0.35) {
                c.clock.step(1)
              }
              val item = (txCount * 13421) % 65535
              c.io.enq.enqueue(item.U)
              txCount += 1
            }
          }.fork {
            while (rxCount < totalCount) {
              if (rand.nextFloat() > 0.35) {
                c.clock.step(1)
              }
              val item = (rxCount * 13421) % 65535
              c.io.deq.expectDequeue(item.U)
              rxCount += 1
            }
          }.join()
        }
      }
    }
  }
}
