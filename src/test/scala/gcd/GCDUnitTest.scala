// See README.md for license details.

package gcd

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

/**
 * compute the gcd and the number of steps it should take to do it
 *
 * @param a positive integer
 * @param b positive integer
 * @return the GCD of a and b
 */
object computeGcd {
  def apply(a: Int, b: Int): (Int, Int) = {
    var x = a
    var y = b
    var depth = 1
    while(y > 0 ) {
      if (x > y) {
        x -= y
      }
      else {
        y -= x
      }
      depth += 1
    }
    (x, depth)
  }
}

/**
  * This is a trivial example of how to run this Specification
  * From within sbt use:
  * {{{
  * testOnly gcd.GCDTester
  * }}}
  * From a terminal shell use:
  * {{{
  * sbt 'testOnly gcd.GCDTester'
  * }}}
  */
class GCDTester extends AnyFlatSpec with ChiselScalatestTester {
  private def doTest(gcd: GCD): Unit = {
    for(i <- 1 to 40 by 3) {
      for (j <- 1 to 40 by 7) {
        gcd.io.value1.poke(i.U)
        gcd.io.value2.poke(j.U)
        gcd.io.loadingValues.poke(true.B)
        gcd.clock.step()
        gcd.io.loadingValues.poke(false.B)

        val (expected_gcd, steps) = computeGcd(i, j)

        gcd.clock.step(steps - 1) // -1 is because we step(1) already to toggle the enable
        gcd.io.outputGCD.expect(expected_gcd.U)
        gcd.io.outputValid.expect(true.B)
      }
    }
  }

  "GCD" should "correctly compute the greatest common denominator" in {
    test(new GCD)(doTest)
  }

}
