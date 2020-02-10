package chisel.lib.ecc

import chisel3.iotesters.PeekPokeTester

class EccTester(dut: EccPair) extends PeekPokeTester(dut) {

  val dutWidth = dut.getWidthParam

  // send through some data without errors
  for (i <- 0 to 20) {
    poke(dut.io.dataIn, i)
    poke(dut.io.errorLocation, 0)
    poke(dut.io.injectError, 0)
    step(1)
    expect(dut.io.dataOut == dut.io.dataIn)
  }
}

object EccTester extends App {
  iotesters.Driver.execute(Array("--target-dir", "generated", "--generate-vcd-output", "on"), () => new EccPair(8)) {
    c => new EccTester(c)
  }
}
