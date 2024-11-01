// See README.md for license details.

package chisel.lib.bitonicsorter

import chisel3._
import chiseltest._
import chiseltest.iotesters.PeekPokeTester
import org.scalatest.flatspec.AnyFlatSpec

//scalastyle:off magic.number

/**
  * Test generator that tests an assortment of random input patterns
  * @author Steve Burns
  *
  * @param numExamples examples to test
  * @param factory     sorter factory
  * @tparam T          element type
  */
class RandomSorterTest[T <: UInt](
  val numExamples: Int,
  factory:         () => SorterModuleIfc[T])
    extends AnyFlatSpec
    with ChiselScalatestTester {
  private val rnd = new scala.util.Random()

  behavior.of("SorterTest")

  it should "work" in {
    test(factory()).runPeekPoke { c =>
      new PeekPokeTester(c) {
        def example(a: IndexedSeq[BigInt]): Unit = {
          poke(c.io.a, a)
          step(1)
          expect(c.io.z, a.sortWith(_ > _))
        }

        for { _ <- 0 until numExamples } {
          val a = IndexedSeq.fill(c.n)(BigInt(c.io.a(0).getWidth, rnd))
          example(a)
        }

      }
    }
  }
}

class BoolBitonicSorterModule(n: Int) extends BitonicSorterModule(n, Bool(), (x: UInt, y: UInt) => x < y)
class UInt8BitonicSorterModule(n: Int) extends BitonicSorterModule(n, UInt(8.W), (x: UInt, y: UInt) => x < y)

// this test takes forever
// class RandomBitonicSorterTest64 extends RandomSorterTest(10000, () => new UInt8BitonicSorterModule(64))
//This tests takes a bit long to do every time
// class RandomBitonicSorterTest384 extends RandomSorterTest( 10000, () => new UInt8BitonicSorterModule( 384))
