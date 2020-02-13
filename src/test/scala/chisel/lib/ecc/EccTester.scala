package chisel.lib.ecc

import chisel3._
import chisel3.iotesters.PeekPokeTester

class EccTester(dut: EccPair) extends PeekPokeTester(dut) {
  val dutWidth = dut.getWidthParam
  var testVal : Int = 0
  // send through some data without errors
  for (i <- 0 to 20) {
    testVal = rnd.nextInt() & ((1 << dutWidth) - 1)

    poke(dut.io.dataIn, testVal.U)
    poke(dut.io.errorLocation, 0.U)
    poke(dut.io.injectError, 0.U)
    step(1)
    expect(dut.io.dataOut, testVal.U)
  }

  // inject single bit errors
  for (i <- 0 to dutWidth) {
    testVal = rnd.nextInt() & ((1 << dutWidth) - 1)

    poke(dut.io.errorLocation, i.U)
    poke(dut.io.injectError, 1.U)
    poke(dut.io.dataIn, testVal.U)
    step(1)
    expect(dut.io.dataOut, testVal.U)
  }
}

object EccTester extends App {
  for (width <- 8 to 32) {
    iotesters.Driver.execute(Array("--target-dir", "generated", "--generate-vcd-output", "on"), () => new EccPair(width=width)) {
      dut => new EccTester(dut)
    }
  }
}

object EccGenerator extends App {
  chisel3.Driver.execute(Array("--target-dir", "generated"), () => new EccCheck(UInt(8.W)))
}