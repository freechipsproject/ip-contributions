package chisel.lib

import scala.collection.immutable.Map
import scala.collection.mutable.ListBuffer

package object ecc {
  def pow2(x : Int) : Int = {
    scala.math.ceil(scala.math.pow(2, x)).toInt
  }

  def calcCodeBits(dataBits : Int) : Int = {
    var m = 1
    var c = 0
    while (c < dataBits) {
      m += 1
      c = pow2(m) - m - 1
    }
    m
  }

  def calcBitMapping(dataBits: Int, reversed : Boolean) : Map[Int, Int] = {
    val outWidth = dataBits + calcCodeBits(dataBits)
    var power : Int = 0
    var mapping1 = new ListBuffer[Int]()
    var mapping2 = new ListBuffer[Int]()

    for (i <- 1 until outWidth+1) {
      if (pow2(power) == i)
        power += 1
      else {
        mapping1 += (i-1)
        mapping2 += (i-power-1)
      }
    }

    if (reversed)
      (mapping2 zip mapping1).toMap
    else
      (mapping1 zip mapping2).toMap
  }

  def buildSeq(bitNum : Int, outWidth: Int) : List[Int] = {
    var bitIndex = new ListBuffer[Int]()
    var cur = 0
    var skip = pow2(bitNum)-1
    var check = 0

    if (skip == 0)
      check = pow2(bitNum)
    else
      check = 0

    while (cur < outWidth) {
      if (check > 0) {
        if (cur != pow2(bitNum)-1)
          bitIndex += cur
        check -= 1
        if (check == 0)
          skip = pow2(bitNum)
      } else {
        skip -= 1
        if (skip == 0)
          check = pow2(bitNum)
      }
      cur += 1
    }

    bitIndex.toList
  }
}
