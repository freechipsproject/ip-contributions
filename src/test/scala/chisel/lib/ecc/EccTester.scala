package chisel.lib.ecc

import chisel3._
import chisel3.iotesters.PeekPokeTester

class EccTester(dut: EccPair) extends PeekPokeTester(dut) {
  val dutWidth = dut.getWidthParam

  // send through some data without errors
  for (i <- 0 to 20) {
    poke(dut.io.dataIn, i.U)
    poke(dut.io.errorLocation, 0.U)
    poke(dut.io.injectError, 0.U)
    step(1)
    expect(dut.io.dataOut, i.U)
  }

  // inject single bit errors
  for (i <- 0 to dutWidth) {
    poke(dut.io.errorLocation, i.U)
    poke(dut.io.injectError, 1.U)
    poke(dut.io.dataIn, "xFF".U)
    step(1)
    expect(dut.io.dataOut, "xFF".U)
  }
}

object EccTester extends App {
  iotesters.Driver.execute(Array("--target-dir", "generated", "--generate-vcd-output", "on"), () => new EccPair(8)) {
    dut => new EccTester(dut)
  }
}

object EccGenerator extends App {
  chisel3.Driver.execute(Array("--target-dir", "generated"), () => new EccCheck(UInt(8.W)))
}