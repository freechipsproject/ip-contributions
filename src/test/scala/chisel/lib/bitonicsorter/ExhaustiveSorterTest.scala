// See README.md for license details.

package chisel.lib.bitonicsorter

import chisel3._
import chiseltest._
import chiseltest.iotesters.PeekPokeTester
import org.scalatest.flatspec.AnyFlatSpec

/**
  * Test generator that tests all possible input patterns
  * @param factory The generator
  * @tparam T      The type of elements
  */
class ExhaustiveSorterTest[T <: Bool]( factory : () => SorterModuleIfc[T]) extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "SorterTest"

  it should "work" in {

    test(factory()).runPeekPoke { c =>
      new PeekPokeTester( c) {
        def example( a:IndexedSeq[BigInt]) {
          poke( c.io.a, a)
          step(1)
          expect( c.io.z, a.sortWith( _>_))
        }

        for { i <- 0 to c.n } {
          for { on <-(0 until c.n).combinations(i)} {
            val v = on.foldLeft( IndexedSeq.fill(c.n){ BigInt(0)}){ case (x,y) =>
              x updated (y,BigInt(1))
            }
            example( v)
          }
        }

      }
    }
  }
}

class ExhaustiveBitonicSorterTest2  extends ExhaustiveSorterTest( () => new BoolBitonicSorterModule(2))
class ExhaustiveBitonicSorterTest3  extends ExhaustiveSorterTest( () => new BoolBitonicSorterModule(3))
class ExhaustiveBitonicSorterTest4  extends ExhaustiveSorterTest( () => new BoolBitonicSorterModule(4))
class ExhaustiveBitonicSorterTest6  extends ExhaustiveSorterTest( () => new BoolBitonicSorterModule(6))
class ExhaustiveBitonicSorterTest8  extends ExhaustiveSorterTest( () => new BoolBitonicSorterModule(8))
class ExhaustiveBitonicSorterTest12 extends ExhaustiveSorterTest( () => new BoolBitonicSorterModule(12))
class ExhaustiveBitonicSorterTest16 extends ExhaustiveSorterTest( () => new BoolBitonicSorterModule(16))
//class ExhaustiveBitonicSorterTest20 extends ExhaustiveSorterTest( () => new BoolBitonicSorterModule(20))
