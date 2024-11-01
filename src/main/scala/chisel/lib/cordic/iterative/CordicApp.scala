// See LICENSE for license details.

package chisel.lib.cordic.iterative

import chisel3._
import chisel3.stage.ChiselStage

/**
  * Make an unapply function for the argument parser.
  * It allows us to match on parameters that are integers
  */
object Int {
  def unapply(v: String): Option[Int] = {
    try {
      Some(v.toInt)
    } catch {
      case _: NumberFormatException => None
    }
  }
}

/**
  * Define entry point for CORDIC generator
  */
object CordicApp extends App {
  val usage = s"""Cordic arguments:
                 |--xy <Int>\t\tWidth of x and y
                 |--z <Int>\t\tWidth of z
                 |--correctGain\t\tCorrect gain
                 |--noCorrectGain\t\tDon't correct gain
                 |--stagesPerCycle <Int>\t\tStages to use per cycle
                 |""".stripMargin

  /**
    * Parse arguments
    *
    * Some arguments are used by the cordic generator and are used to construct a FixedCordicParams object.
    * The rest get returned as a List[String] to pass to the Chisel driver
    */
  def argParse(args: List[String], params: FixedCordicParams): (List[String], FixedCordicParams) = {
    args match {
      case "--help" :: tail =>
        println(usage)
        val (newArgs, newParams) = argParse(tail, params)
        ("--help" +: newArgs, newParams)
      case "--xy" :: Int(xy) :: tail              => argParse(tail, params.copy(xyWidth = xy))
      case "--z" :: Int(z) :: tail                => argParse(tail, params.copy(zWidth = z))
      case "--correctGain" :: tail                => argParse(tail, params.copy(correctGain = true))
      case "--noCorrectGain" :: tail              => argParse(tail, params.copy(correctGain = false))
      case "--stagesPerCycle" :: Int(spc) :: tail => argParse(tail, params.copy(stagesPerCycle = spc))
      case chiselOpt :: tail => {
        val (newArgs, newParams) = argParse(tail, params)
        (chiselOpt +: newArgs, newParams)
      }
      case Nil => (args, params)
    }
  }
  val defaultParams = FixedCordicParams(
    xyWidth = 12,
    zWidth = 12,
    stagesPerCycle = 1
  )
  val (chiselArgs, params) = argParse(args.toList, defaultParams)
  // Run the Chisel driver to generate a cordic
  emitVerilog(new IterativeCordic(params), chiselArgs.toArray)
}
