package chisel.lib.freeset

import chisel3._
import chisel3.util._
import chisel3.internal.firrtl.Width

/** Definition of Inputs/Outputs of the scalable free set
 *  @param get Valid/Ready interface for getting an available tag (loan)
 *  @param putPorts One or more ports to return a tag that is no longer needed
 *  @param burstCount Minimum number of tags available without stall (useful in pipelined designs)
 */
class FreeSetIO(tagCount: Int, burstSize: Int, putPortCnt: Int) extends Bundle {
  def tagWidth : Width = log2Ceil(tagCount).W
  def tag : UInt = UInt(tagWidth)
  val get  = Decoupled(tag)
  val putPorts = Input(Vec(putPortCnt, Valid(tag)))
  def put: Valid[UInt] = putPorts.head // alias for the first and often only put port
  val burstCount = Output(UInt(log2Ceil(burstSize + 1).W))
}

/** Module that manages a set of tags. Their number can be arbitrary large.
 *  @param tagCount Number of tags managed by the circuit
 *  @param putPortCnt Number of put ports for tag return
 *  @param bitsPerRank Number of bits per stage. Design tradeoff. Upper bound depends on technology/frequency.
 *  @param burstSize Size of output buffer, to offer burst of tags without stall
 */
class FreeSet(
  tagCount: Int,
  burstSize: Int = 0,
  putPortCnt: Int = 1,
  bitsPerRank: Int = 6) extends Module { // scalastyle:ignore magic.number
  require(burstSize <= tagCount, "Output buffer size should not exceed the number of tags managed by the circuit")
  val io = IO(new FreeSetIO(tagCount, burstSize, putPortCnt))

  /** I/O bundle of a basic block
   */
  class FreeSetBlockIO(w: Int, xPosW: Int) extends Bundle {
    val x = Input(UInt(w.W))
    val xPos = Input(Vec(w, UInt(xPosW.W)))
    val clear = Output(UInt(w.W))
    val freePos = Decoupled(UInt((xPosW + log2Ceil(w)).W))
  }

  /** Basic Block, one node in the tree. Counts number of trailing zeros in input vector, and offers it downstream.
   */
  class FreeSetBlock(w: Int, xPosW: Int) extends Module {
    val io = IO(new FreeSetBlockIO(w,xPosW))
    val y = PriorityEncoder(io.x)
    val yDecoded = UIntToOH(y, w)
    val anyOne = io.x.orR
    val yPos = Mux1H(yDecoded.asBools, io.xPos)
    val v = RegInit(false.B)
    val load = !v
    v := (v && !io.freePos.ready) || (load && anyOne)
    io.clear := Mux(load, yDecoded, 0.U)
    io.freePos.bits := RegEnable(Cat(y,yPos), load)
    io.freePos.valid := v
  }

  /* Ideas for area reduction:
   *  1- replace PriorityEncoder circuity by a balanced radix-8 tree. (as PriorityEncoder, exploit that x=all-0 allows y=all-1)
   *  2- use a state machine to initialize the free flops 1 bit at a time, using a put port, to save reset circuitry
   *  3- figure out the highest possible radix for the target frequency. Consider higher radix on first rank
   */

  def radix:Int = 1<<bitsPerRank // width of a building block; count so many zeros per block
  def ranksCnt:Int = math.ceil(log2Ceil(tagCount).toDouble/bitsPerRank.toDouble).toInt // Number of ranks needed
  def inputsToRank(r: Int) : Int = math.ceil(tagCount.toDouble/(1<<(bitsPerRank*r)).toDouble).toInt // how many bits into rank r

  // Build phase: Instantiate the freeTag flops, and the basic blocks to search the vector
  val allOnes = 1 << tagCount
  val freeTags = RegInit(((BigInt(1) << tagCount)-1).U)  // Fill(tagCount, 1.U) does not return a literal, so cannot use w/ asynchronous reset
  val freeTagsRanks=Seq.tabulate(ranksCnt) (rank => {
    Seq.tabulate(inputsToRank(rank + 1)) ( block => {
      // last block in the rank (highest numbered) has reduced width
      def blockWidth: Int = if ((block + 1) * radix <= inputsToRank(rank)) radix else inputsToRank(rank) % radix
      Module(new FreeSetBlock(blockWidth,rank*bitsPerRank))
    })
  })
  require(freeTagsRanks.last.length==1, "Last rank must have exactly 1 block")

  // Connect phase: Connect the basic blocks, rank by rank
  // Inputs to first rank = spread freeTags flop outputs
  freeTagsRanks(0).zipWithIndex.map(pair => pair._1.io.x := freeTags(((pair._2 + 1)*radix).min(tagCount)-1,pair._2*radix))
  freeTagsRanks(0).map(b => b.io.xPos ).flatten.map(_ := 0.U(0.W))
  val freeTagsClear = freeTagsRanks(0).map(block => block.io.clear).reduce((l,h)=>Cat(h,l)) // very wide UInt

  // Connections between ranks
  for (rank <- 1 until freeTagsRanks.length) {
    val xNextRank = freeTagsRanks(rank-1).map(_.io.freePos.valid.asUInt).reduce((l,h)=>Cat(h,l)) // input = single-bit valids of previous rank
    require(xNextRank.getWidth==inputsToRank(rank), "CLZ connection error")
    freeTagsRanks(rank).zipWithIndex.map(pair => pair._1.io.x := xNextRank( ((pair._2 + 1)*radix).min(inputsToRank(rank)-1), pair._2*radix))
    freeTagsRanks(rank).map(b => b.io.xPos).flatten.zip(freeTagsRanks(rank-1).map(b => b.io.freePos.bits)).map(pin => pin._1 := pin._2)
    val clearPrevRank = freeTagsRanks(rank).map(block => block.io.clear).reduce((l,h)=>Cat(h,l))
    freeTagsRanks(rank-1).zipWithIndex.map(pair => pair._1.io.freePos.ready := clearPrevRank(pair._2))
  }

  // Final rank output optionally feeds a FIFO
  val freeTag = freeTagsRanks.last.last.io.freePos
  when(freeTag.valid) {
    assert(!freeTags(freeTag.bits), "Offering the same tag twice")
    assert(freeTag.bits<tagCount.U, "Offering an out-of-range tag")
  }
  if (burstSize > 2) {
    val outputFifo = Module(new Queue(io.tag, burstSize))
    outputFifo.io.enq <> freeTag
    io.get <> outputFifo.io.deq
    io.burstCount := outputFifo.io.count
  } else {
    io.get <> freeTag
    io.burstCount := 0.U
  }

  // Circuitry for one or more put ports, for returning a tag that is no longer needed
  val freeTagsSet = io.putPorts.map(p => {
    when (p.valid) {
      assert(!freeTags(p.bits), "Returning a tag that was already available")
      assert(p.bits<tagCount.U, "Returning an out-of-range tag")
    }
    Mux(p.valid, UIntToOH(p.bits, tagCount),0.U) } ).reduce(_|_)

  freeTags := freeTags & ~freeTagsClear | freeTagsSet // S/R flops
  require (freeTagsClear.getWidth==tagCount, "Clear vector width incorrect")
  require (freeTagsSet.getWidth  ==tagCount, "Set vector width incorrect")
}
