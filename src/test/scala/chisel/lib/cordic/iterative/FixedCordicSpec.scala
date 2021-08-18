// See LICENSE for license details.

package chisel.lib.cordic.iterative

import dsptools.numbers._
import org.scalatest.flatspec.AnyFlatSpec

import scala.math.{ceil, max}

class FixedCordicSpec extends AnyFlatSpec {
  behavior of "FixedIterativeCordic"

  val params = FixedCordicParams(
    xyWidth = 16,
    zWidth = 16,
    correctGain = true,
    stagesPerCycle = 1
  )
  it should "rotate" in {
    val baseTrial = XYZ(xin= 1.0, yin=0.0, zin=0.0, vectoring=false)
    val angles = Seq(-3, -2, -1, -0.5, 0, 0.25, 0.5, 1, 2, 3)
    val trials = angles.map { phi => baseTrial.copy(zin = phi, xout = Some(math.cos(phi)), yout = Some(math.sin(phi)), zout = Some(0)) }
    assert(FixedCordicTester(params, trials))
  }
  it should "vector" in {
    val baseTrial = XYZ(xin= 1.0, yin=0.0, zin=0.0, vectoring=true)
    val angles = Seq(-3, -2, -1, -0.5, 0, 0.25, 0.5, 1, 2, 3)
    val trials = angles.map { phi => baseTrial.copy(xin = math.cos(phi), yin = math.sin(phi), xout = Some(1), yout = Some(0), zout = Some(phi)) }
    assert(FixedCordicTester(params, trials))
  }

  // No gain tests.
  val paramsNoGain = FixedCordicParams(
    xyWidth = params.xyWidth,
    zWidth = params.zWidth,
    correctGain = false,
    stagesPerCycle = params.stagesPerCycle
  )
  val nStages = ceil(max(params.xyWidth, params.zWidth)/params.stagesPerCycle).toInt * params.stagesPerCycle
  var gainCor = Constants.gain(nStages)

  it should "rotate without gain correction" in {
    val baseTrial = XYZ(xin= 1.0, yin=0.0, zin=0.0, vectoring=false)
    val angles = Seq(-3, -2, -1, -0.5, 0, 0.25, 0.5, 1, 2, 3)
    val trials = angles.map { phi => baseTrial.copy(zin = phi, xout = Some(math.cos(phi)*gainCor), yout = Some(math.sin(phi)*gainCor), zout = Some(0)) }
    assert(FixedCordicTester(paramsNoGain, trials))
  }
  it should "vector without gain correction" in {
    val baseTrial = XYZ(xin= 1.0, yin=0.0, zin=0.0, vectoring=true)
    val angles = Seq(-3, -2, -1, -0.5, 0, 0.25, 0.5, 1, 2, 3)
    val trials = angles.map { phi => baseTrial.copy(xin = math.cos(phi), yin = math.sin(phi), xout = Some(gainCor), yout = Some(0), zout = Some(phi)) }
    assert(FixedCordicTester(paramsNoGain, trials))
  }


  behavior of "RealIterativeCordic"

  val realParams = new CordicParams[DspReal] {
    val protoXY = DspReal()
    val protoZ = DspReal()
    val protoXYZ = DspReal()
    val nStages = 30
    val correctGain = true
    val stagesPerCycle = params.stagesPerCycle
  }
  it should "rotate" in {
    val baseTrial = XYZ(xin= 1.0, yin=0.0, zin=0.0, vectoring=false)
    val angles = Seq(-3, -2, -1, -0.5, 0, 0.25, 0.5, 1, 2, 3)
    val trials = angles.map { phi => baseTrial.copy(zin = phi, xout = Some(math.cos(phi)), yout = Some(math.sin(phi)), zout = Some(0)) }
    assert(RealCordicTester(realParams, trials))
  }
  it should "vector" in {
    val baseTrial = XYZ(xin= 1.0, yin=0.0, zin=0.0, vectoring=true)
    val angles = Seq(-3, -2, -1, -0.5, 0, 0.25, 0.5, 1, 2, 3)
    val trials = angles.map { phi => baseTrial.copy(xin = math.cos(phi), yin = math.sin(phi), xout = Some(1), yout = Some(0), zout = Some(phi)) }
    assert(RealCordicTester(realParams, trials))
  }

  // No gain tests.
  val realParamsNoGain = new CordicParams[DspReal] {
    val protoXY = DspReal()
    val protoZ = DspReal()
    val protoXYZ = DspReal()
    val nStages = realParams.nStages
    val correctGain = false
    val stagesPerCycle = params.stagesPerCycle
  }
  gainCor = Constants.gain(realParams.nStages)

  it should "rotate without gain correction" in {
    val baseTrial = XYZ(xin= 1.0, yin=0.0, zin=0.0, vectoring=false)
    val angles = Seq(-3, -2, -1, -0.5, 0, 0.25, 0.5, 1, 2, 3)
    val trials = angles.map { phi => baseTrial.copy(zin = phi, xout = Some(math.cos(phi)*gainCor), yout = Some(math.sin(phi)*gainCor), zout = Some(0)) }
    assert(RealCordicTester(realParamsNoGain, trials))
  }
  it should "vector without gain correction" in {
    val baseTrial = XYZ(xin= 1.0, yin=0.0, zin=0.0, vectoring=true)
    val angles = Seq(-3, -2, -1, -0.5, 0, 0.25, 0.5, 1, 2, 3)
    val trials = angles.map { phi => baseTrial.copy(xin = math.cos(phi), yin = math.sin(phi), xout = Some(gainCor), yout = Some(0), zout = Some(phi)) }
    assert(RealCordicTester(realParamsNoGain, trials))
  }

}
