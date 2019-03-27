// See LICENSE for license details.

package chisel.lib.cordic.iterative

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util.Decoupled
import chisel3.util.log2Ceil

import scala.math.{ceil, max}
import dsptools.numbers._

/**
  * Base class for CORDIC parameters
  *
  * These are type generic
  */
trait CordicParams[T <: Data] {
  val protoXY: T
  val protoZ: T
  val protoXYZ: T
  val nStages: Int
  val correctGain: Boolean
  val stagesPerCycle: Int
}

/**
  * CORDIC parameters object for fixed-point CORDICs
  */
case class FixedCordicParams(
  // width of X and Y
  xyWidth: Int,
  // width of Z
  zWidth: Int,
  // scale output by correction factor?
  correctGain: Boolean = true,
  // number of CORDIC stages to perform per clock cycle
  stagesPerCycle: Int = 1,
) extends CordicParams[FixedPoint] {

  // prototype for x and y
  // binary point is (xyWidth-2) to represent 1.0 exactly
  // need an extra digit for XY in the situation where we don't gain correct
  val protoXY = FixedPoint(xyWidth.W, if (correctGain) (xyWidth-2).BP else (xyWidth-3).BP)
  // prototype for z
  // binary point is (xyWidth-3) to represent Pi/2 exactly
  val protoZ = FixedPoint(zWidth.W, (zWidth-3).BP)
  // internal width needs to be n + log2(n) + 2 where n is output width
  val totalWidth: Int = max(xyWidth, zWidth) + log2Ceil(max(xyWidth, zWidth)) + 2
  val protoXYZ = FixedPoint(totalWidth.W, (totalWidth-3).BP)
  // make nStages be output width precision, as integer multiple of stagesPerCycle
  val nStages: Int = ceil(max(xyWidth, zWidth)/stagesPerCycle).toInt * stagesPerCycle
}

/**
  * Bundle type that describes the input, and output of CORDIC
  */
class CordicBundle[T <: Data](params: CordicParams[T]) extends Bundle {
  val x: T = params.protoXY.cloneType
  val y: T = params.protoXY.cloneType
  val z: T = params.protoZ.cloneType

  override def cloneType: this.type = CordicBundle(params).asInstanceOf[this.type]
}
object CordicBundle {
  def apply[T <: Data](params: CordicParams[T]): CordicBundle[T] = new CordicBundle(params)
}

/**
  * To connect vectoring from AXI4
  */
class CordicBundleWithVectoring[T <: Data](params: CordicParams[T]) extends CordicBundle[T](params) {
  val vectoring: Bool = Bool()

  override def cloneType: this.type = CordicBundleWithVectoring(params).asInstanceOf[this.type]
}
object CordicBundleWithVectoring {
  def apply[T <: Data](params: CordicParams[T]): CordicBundleWithVectoring[T] = new CordicBundleWithVectoring(params)
}


/**
  * Bundle type that describes the internal state of CORDIC (needed for better fixed point accuracy
  */
class CordicInternalBundle[T <: Data](params: CordicParams[T]) extends Bundle {
  val x: T = params.protoXYZ.cloneType
  val y: T = params.protoXYZ.cloneType
  val z: T = params.protoXYZ.cloneType

  override def cloneType: this.type = CordicInternalBundle(params).asInstanceOf[this.type]
}
object CordicInternalBundle {
  def apply[T <: Data](params: CordicParams[T]): CordicInternalBundle[T] = new CordicInternalBundle(params)
}


/**
  * Bundle type as IO for iterative CORDIC modules
  */
class IterativeCordicIO[T <: Data](params: CordicParams[T]) extends Bundle {
  val in = Flipped(Decoupled(new CordicBundleWithVectoring(params)))
  val out = Decoupled(CordicBundle(params))

  //val vectoring = Input(Bool())

  override def cloneType: this.type = IterativeCordicIO(params).asInstanceOf[this.type]
}
object IterativeCordicIO {
  def apply[T <: Data](params: CordicParams[T]): IterativeCordicIO[T] =
    new IterativeCordicIO(params)
}

object AddSub {
  def apply[T <: Data : Ring](sel: Bool, a: T, b: T): T = {
    Mux(sel, a + b, a - b)
  }
}

class IterativeCordic[T <: Data : Real : BinaryRepresentation](val params: CordicParams[T]) extends Module {
  // Check parameters
  require(params.nStages > 0)
  require(params.stagesPerCycle > 0)
  require(params.nStages >= params.stagesPerCycle)
  require(params.nStages % params.stagesPerCycle == 0, "nStages % stagesPerCycle must equal 0")

  val io = IO(IterativeCordicIO(params))

  // SETUP - regs, wires, constants

  // register to store vectoring or else it will grab the random value from the bus
  val vecReg = Reg(Bool())

  // registers to store xi, yi, and zi
  val xyz = Reg(CordicInternalBundle(params))

  // intermediate wires for loop unrolling
  val xyzn = Wire(Vec(params.stagesPerCycle+1, CordicInternalBundle(params)))

  // counter that will control for how many cycles we are computing & not ready to receive more data
  val counter = RegInit(0.U(log2Ceil(params.nStages+1).W))

  // sequence of calculated arctans => vector called alpha
  val alpha = VecInit(Constants.arctan(params.nStages).map(params.protoXYZ.fromDouble(_)))

  // perform range extension as well using an initial 90 degree rotation if necessary
  // initial rotation by pi/2 if in quadrants II or III
  val halfPi = params.protoXYZ.fromDouble(scala.math.Pi/2)
  val ext = Mux(io.in.bits.vectoring, io.in.bits.x.isSignNegative(), io.in.bits.z.abs() > halfPi)
  val bottom = Mux(io.in.bits.vectoring, io.in.bits.y.isSignNegative(), io.in.bits.z.isSignNegative())
  val extReg, bottomReg = Reg(Bool())

  // STATE MACHINE

  // we are ready to receive data by default and output data is not valid
  val inReady = RegInit(true.B)
  io.in.ready := inReady
  val outValid = RegInit(false.B)
  io.out.valid := outValid

  // when the input interface turns valid deassert ready & load the initial data (incl. initial rotation if necessary)
  when (io.in.fire()) {
    inReady := false.B
    vecReg := io.in.bits.vectoring
    extReg := ext
    bottomReg := bottom
    xyz.x := Mux(ext, Mux(bottom, -1*io.in.bits.y, io.in.bits.y), io.in.bits.x)
    xyz.y:= Mux(ext, Mux(bottom, io.in.bits.x, -1*io.in.bits.x), io.in.bits.y)
    xyz.z := Mux(ext, Mux(bottom, halfPi, -1*halfPi)+io.in.bits.z, io.in.bits.z)
  }

  // when we are no longer ready, start computing by incrementing counter
  when (!inReady && counter < (params.nStages).U) {
    counter := counter + params.stagesPerCycle.U
    // and update register from previous clock cycle
    xyz := xyzn(params.stagesPerCycle)
  }

  // finally when the counter hits 0, and latch out data/valid (gain corrected if desired).
  // reset counter and return to in ready/out invalid state only after the valid went high and output interface is ready
  when (counter >= (params.nStages).U) {
    outValid := true.B

    when (io.out.fire()) {
      counter := 0.U
      inReady := true.B
      outValid := false.B
    }
  }

  // COMBINATORIAL CORDIC COMPUTATION

  // calculate each stage's x,y,z
  // 0th wire in Vec always equal to the register for easy indexing inside loop
  xyzn(0) := xyz
  for (j <- 0 until params.stagesPerCycle) {
    val delta = Mux(vecReg, xyzn(j).y.signBit()^xyzn(j).x.signBit(), xyzn(j).z.isSignPositive())
    val shift = counter + j
    xyzn(j+1).x := AddSub(!delta, xyzn(j).x, xyzn(j).y >> shift)
    xyzn(j+1).y := AddSub(delta, xyzn(j).y, xyzn(j).x >> shift)
    xyzn(j+1).z := AddSub(!delta, xyzn(j).z, alpha(shift))
  }

  // OUTPUTS

  // gain correction
  val gainCor = params.protoXYZ.fromDouble(1/Constants.gain(params.nStages)) //always less than 1
  val xoutCor = Mux(params.correctGain.B, xyz.x*gainCor, xyz.x)
  val youtCor = Mux(params.correctGain.B, xyz.y*gainCor, xyz.y)

  // if rotation, flip x & y if rotation was into left half plane
  io.out.bits.x := Mux(!vecReg && extReg, -1*xoutCor, xoutCor)
  io.out.bits.y := Mux(!vecReg && extReg, -1*youtCor, youtCor)

  // if vectoring, add/subtract pi to z if vectoring was from left half plane
  val pi = params.protoXYZ.fromDouble(scala.math.Pi)
  io.out.bits.z := Mux(vecReg && extReg, Mux(bottomReg, xyz.z-pi, xyz.z+pi), xyz.z)

}

/**
  * Mixin for top-level rocket to add a PWM
  *
  */
//trait HasPeripheryCordic extends BaseSubsystem {
//  // instantiate cordic chain
//  val cordicChain = LazyModule(new CordicThing(FixedCordicParams(8, 10)))
//  // connect memory interfaces to pbus
//  pbus.toVariableWidthSlave(Some("cordicWrite")) { cordicChain.writeQueue.mem.get }
//  pbus.toVariableWidthSlave(Some("cordicRead")) { cordicChain.readQueue.mem.get }
//}
