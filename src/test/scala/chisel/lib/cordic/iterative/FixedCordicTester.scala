// See LICENSE for license details.

package chisel.lib.cordic.iterative

import dsptools.DspTester

/**
  * Case class holding information needed to run an individual test
  */
case class XYZ(
  // input x, y and z
  xin: Double,
  yin: Double,
  zin: Double,
  // mode
  vectoring: Boolean,
  // optional outputs
  // if None, then don't check the result
  // if Some(...), check that the result matches
  xout: Option[Double] = None,
  yout: Option[Double] = None,
  zout: Option[Double] = None
)

/**
  * DspTester for FixedIterativeCordic
  *
  * Run each trial in @trials
  */
class CordicTester[T <: chisel3.Data](c: IterativeCordic[T], trials: Seq[XYZ], tolLSBs: Int = 2) extends DspTester(c) {
  val maxCyclesWait = 50

  poke(c.io.out.ready, 1)
  poke(c.io.in.valid, 1)

  for (trial <- trials) {
    poke(c.io.in.bits.x, trial.xin)
    poke(c.io.in.bits.y, trial.yin)
    poke(c.io.in.bits.z, trial.zin)
    poke(c.io.in.bits.vectoring, trial.vectoring)

    // wait until input is accepted
    var cyclesWaiting = 0
    while (!peek(c.io.in.ready) && cyclesWaiting < maxCyclesWait) {
      cyclesWaiting += 1
      if (cyclesWaiting >= maxCyclesWait) {
        expect(false, "waited for input too long")
      }
      step(1)
    }
    // wait until output is valid
    cyclesWaiting = 0
    while (!peek(c.io.out.valid) && cyclesWaiting < maxCyclesWait) {
      cyclesWaiting += 1
      if (cyclesWaiting >= maxCyclesWait) {
        expect(false, "waited for output too long")
      }
      step(1)
    }
    // set desired tolerance
    // in this case, it's pretty loose (2 bits)
    // can you get tolerance of 1 bit? 0? what makes the most sense?
    fixTolLSBs.withValue(tolLSBs) {
      // check every output where we have an expected value
      trial.xout.foreach { x => expect(c.io.out.bits.x, x) }
      trial.yout.foreach { y => expect(c.io.out.bits.y, y) }
      trial.zout.foreach { z => expect(c.io.out.bits.z, z) }
    }
  }
}

/**
  * Convenience function for running tests
  */

object FixedCordicTester {
  def apply(params: FixedCordicParams, trials: Seq[XYZ]): Boolean = {
    dsptools.Driver.execute(
      () => new IterativeCordic(params),
      Array("-tbn", "firrtl", "-fiwv"))
    {
      c => new CordicTester(c, trials)
    }
  }
}

object RealCordicTester {
  def apply(params: CordicParams[dsptools.numbers.DspReal], trials: Seq[XYZ]): Boolean = {
    dsptools.Driver.execute(
      () => new IterativeCordic(params),
      Array("--backend-name", "verilator", "-fiwv", "-rtdec", "6")
    ) {
      c => new CordicTester(c, trials)
    }
  }
}
