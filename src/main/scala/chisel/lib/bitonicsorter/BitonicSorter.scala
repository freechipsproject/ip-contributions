// See README.md for license details.

package chisel.lib.bitonicsorter

import chisel3._
import chisel3.util._

/**
  * Builds a mux whose that tests its two inputs
  * Construct hardware to swap inputs to outputs or pass them straight through depending on comparison function
  * otherwise
  * @param proto  The type generator for the swapper
  * @param lt     The comparision function
  * @tparam T     The type of the inputs and outputs, derived from proto
  */
class Swapper[T <: Data]( proto : T, lt : (T,T) => Bool) extends Module {
  val io = IO(new Bundle {
    val a0 = Input( proto )
    val a1 = Input( proto )
    val z0 = Output( proto )
    val z1 = Output( proto )
  })
  when ( lt(io.a1,io.a0)) {
    io.z0 := io.a1
    io.z1 := io.a0
  } .otherwise {
    io.z0 := io.a0
    io.z1 := io.a1
  }
}

/**
  * Bitonic Sorter is a factory for combinational sort hardware.
  * It requires that the elements to be sorted are a power of 2.
  *
  * @see https://en.wikipedia.org/wiki/Bitonic_sorter
  *
  */
object BitonicSorter {
  def apply[T <: Data]( a : IndexedSeq[Option[T]], factory : () => Swapper[T]) : IndexedSeq[Option[T]] = {
    assert( (1 << log2Up(a.length)) == a.length)
    def insertSorter( a : IndexedSeq[Option[T]], lo : Int, hi : Int) : IndexedSeq[Option[T]] = {
      (a(lo),a(hi)) match {
        case (_, None) => a
        case (None, Some(aH)) => a updated (lo,Some(aH)) updated (hi,None)
        case (Some(aL), Some(aH)) =>
          val m = Module( factory())
          m.io.a0 := aL
          m.io.a1 := aH
          a updated (lo,Some(m.io.z0)) updated (hi,Some(m.io.z1))
      }
    }
    (for { i <- 0 until log2Up(a.length)
           j <- i to 0 by -1
           k0 <- a.indices by (2<<j)
           k1 <- 0 until 1<<j} yield {
      val lo = k0 + k1
      val hi = lo + (1<<j)
      if ( (lo >> (i + 1)) % 2 == 0) (lo, hi) else (hi, lo)
    }).foldLeft(a){ case (s,(l,h)) => insertSorter( s, l, h)}
  }
}

/**
  * Defines the interface used for the Bitonic Sort module
  * @param n      The number of elements
  * @param proto  An instance of the type of all elements
  * @tparam T     The type as derived from proto
  */
class SorterModuleIfc[T <: Data](val n : Int, proto : T) extends Module {
  val io = IO( new Bundle {
    val a = Input( Vec( n, proto.cloneType))
    val z = Output( Vec( n, proto.cloneType))
  })
}

/*
  * What follows are some example code generation calls.
  */

class BitonicSorterModule[T <: Data]( n : Int, proto : T, lt : (T,T) => Bool) extends SorterModuleIfc(n,proto) {
  private val a = IndexedSeq.tabulate( 1 << log2Up(io.a.length)){ i => if ( i < n) Some(io.a(i)) else None}
  io.z := VecInit( BitonicSorter( a, () => new Swapper( proto.cloneType, lt)).slice( 0, n) map (_.get))
}

//scalastyle:off magic.number
object BitonicSorterUInt8_64Driver extends App {
  Driver.execute( args, () => new BitonicSorterModule( 64, UInt(8.W), (x:UInt,y:UInt)=>x<y))
}

//scalastyle:off magic.number
object BitonicSorterUInt8_384Driver extends App {
  Driver.execute( args, () => new BitonicSorterModule( 384, UInt(8.W), (x:UInt,y:UInt)=>x<y))
}
