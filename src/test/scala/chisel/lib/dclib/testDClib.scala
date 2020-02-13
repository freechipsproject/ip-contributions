/**
 *
 */
package chisel.lib.dclib

import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}


object testDClib extends App {
  // Run unit tests
  iotesters.Driver.execute(args, () => new DCInputOutputTestbench) {
    tb => new DCInputOutputTester(tb)
  }

  iotesters.Driver.execute(args, () => new DCHoldTestbench) {
    tb => new DCHoldTester(tb)
  }

  iotesters.Driver.execute(args, () => new ArbMirrorTestbench(5)) {
    tb => new ArbMirrorTester(tb)
  }
}

