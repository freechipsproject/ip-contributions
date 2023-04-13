package chisel.lib.spi

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Random

class SpiMasterTest extends AnyFlatSpec with ChiselScalatestTester {

  val frequency = 100000000
  val clkfreq = 10000000

  def transferOneWord(bsize: Int, mode: Int, msbFirst: Boolean, dut: => Master) {
    test(dut) { dut =>

        val clkStepPerHalfSclk = (frequency + clkfreq/2) / clkfreq / 2 - 1

        val rnd = new Random()
        val outputVal = rnd.nextLong() & (Math.pow(2, bsize).toInt - 1)
        val inputVal = rnd.nextLong() & (Math.pow(2, bsize).toInt - 1)

        var sclkExp = if ((mode == 0) || (mode == 1)) false else true

        dut.clock.step()

        mode match {
          case 0 => {
            dut.io.cpol.poke(false.B)
            dut.io.cpha.poke(false.B)
          }
          case 1 => {
            dut.io.cpol.poke(false.B)
            dut.io.cpha.poke(true.B)
          }
          case 2 => {
            dut.io.cpol.poke(true.B)
            dut.io.cpha.poke(false.B)
          }
          case 3 => {
            dut.io.cpol.poke(true.B)
            dut.io.cpha.poke(true.B)
          }
        }

        dut.io.msbfirst.poke(msbFirst.B)

        dut.clock.step()

        // Idle state
        dut.io.sclk.expect(sclkExp)
        dut.io.mosi.expect(false.B)
        dut.io.busy.expect(false.B)

        // Send data
        dut.io.din.ready.poke(true.B)
        dut.io.dout.bits.poke(outputVal.asUInt(bsize.W))

        dut.io.dout.valid.poke(true.B)

        dut.clock.step()

        dut.io.dout.valid.poke(false.B)

        if ((mode == 1) || (mode == 3)) {
          // Wait half sclk step
          for (s <- 0 to clkStepPerHalfSclk) {
            dut.io.busy.expect(true.B)
            dut.io.sclk.expect(sclkExp)

            dut.clock.step()
          }

          sclkExp = !sclkExp
        }

        val firstBit = if (msbFirst) bsize - 1 else 0
        val lastBit = if (msbFirst) 0 else bsize - 1
        val step = if (msbFirst) -1 else 1

        for (bit <- firstBit to lastBit by step) {

          if ((inputVal & (0x1 << bit)) != 0) {
            dut.io.miso.poke(true.B)
          } else {
            dut.io.miso.poke(false.B)
          }

          for (s <- 0 to clkStepPerHalfSclk) {
            dut.io.busy.expect(true.B)
            dut.io.sclk.expect(sclkExp)
            if ((outputVal & (0x1 << bit)) != 0) {
              dut.io.mosi.expect(true.B)
            } else {
              dut.io.mosi.expect(false.B)
            }

            dut.clock.step()
          }

          sclkExp = !sclkExp

          if (bit == lastBit) {
            // End of word
            dut.io.din.valid.expect(true.B)
            dut.io.din.bits.expect(inputVal.asUInt)
          } else {
            dut.io.din.valid.expect(false.B)
          }

          for (s <- 0 to clkStepPerHalfSclk) {
            dut.io.busy.expect(true.B)
            dut.io.sclk.expect(sclkExp)
            if ((outputVal & (0x1 << bit)) != 0) {
              dut.io.mosi.expect(true.B)
            } else {
              dut.io.mosi.expect(false.B)
            }

            dut.clock.step()
          }

          sclkExp = !sclkExp
        }

        dut.io.busy.expect(false.B)
      }
  }

  it should "work in mode 0 lsb first" in {
    for (bsize <- Seq(4, 8, 16, 32)) {
      transferOneWord(bsize, 0, false, new Master(frequency, clkfreq, bsize))
    }
  }

  it should "work in mode 1 lsb first" in {
    for (bsize <- Seq(4, 8, 16, 32)) {
      transferOneWord(bsize, 1, false, new Master(frequency, clkfreq, bsize))
    }
  }

  it should "work in mode 2 lsb first" in {
    for (bsize <- Seq(4, 8, 16, 32)) {
      transferOneWord(bsize, 2, false, new Master(frequency, clkfreq, bsize))
    }
  }

  it should "work in mode 3 lsb first" in {
    for (bsize <- Seq(4, 8, 16, 32)) {
      transferOneWord(bsize, 3, false, new Master(frequency, clkfreq, bsize))
    }
  }

  it should "work in mode 0 msb first" in {
    for (bsize <- Seq(4, 8, 16, 32)) {
      transferOneWord(bsize, 0, true, new Master(frequency, clkfreq, bsize))
    }
  }

  it should "work in mode 1 msb first" in {
    for (bsize <- Seq(4, 8, 16, 32)) {
      transferOneWord(bsize, 1, true, new Master(frequency, clkfreq, bsize))
    }
  }

  it should "work in mode 2 msb first" in {
    for (bsize <- Seq(4, 8, 16, 32)) {
      transferOneWord(bsize, 2, true, new Master(frequency, clkfreq, bsize))
    }
  }

  it should "work in mode 3 msb first" in {
    for (bsize <- Seq(4, 8, 16, 32)) {
      transferOneWord(bsize, 3, true, new Master(frequency, clkfreq, bsize))
    }
  }
}
