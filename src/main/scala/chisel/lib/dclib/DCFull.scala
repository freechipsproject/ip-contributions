package chisel.lib.dclib
import chisel3._
import chisel3.util._

/**
  * Provides timing closure on valid, ready and bits interfaces
  * Effectively a 2-entry FIFO, but with registered inputs and outputs
  */
object DCFull {
  def apply[D <: Data](x : DecoupledIO[D]) : DecoupledIO[D] = {
    val tfull = Module(new DCFull(x.bits.cloneType))
    tfull.io.enq <> x
    tfull.io.deq <> tfull.io.enq
    tfull.io.deq
  }
}

/**
 * This is a port of Frank's sdfull module with better input/output timing
 *
 * Frank's version also supports internal control replication for wide datapaths, which was not ported
 *
 * @param data Incoming data type
 */
class DCFull[D <: Data](data: D) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(new DecoupledIO(data.cloneType))
    val deq = new DecoupledIO(data.cloneType)
  })
  override def desiredName: String = "DCFull_" + data.toString

  // These are used for control replication for large fan-out
  //val ctrl_fanout=64
  //val tmp_modulos=data.getWidth % ctrl_fanout;
  //val ctrl_rep = if (tmp_modulos==0) data.getWidth/ctrl_fanout else (data.getWidth/ctrl_fanout + 1)
  //val lfo=data.getWidth/ctrl_rep
  //val hfo=data.getWidth-lfo*(ctrl_rep-1)

  // S_0_0: both hold flop empty
  // S_2_1: hold flop 0 is tail, holding flop 1 is head
  val s_0_0 :: s_1_0 :: s_0_1 :: s_2_1 :: Nil = Enum(4)
  val state = RegInit(init=s_0_0)
  val nxt_state = WireDefault(state)

  val hold_1 = Reg(data.cloneType)
  val hold_0 = Reg(data.cloneType)
  val nxt_shift = WireDefault(0.B)
  val nxt_load = WireDefault(0.B)
  val nxt_send_sel = WireDefault(1.B)
  val shift = Reg(Bool())
  val load = Reg(Bool())
  val send_sel = Reg(Bool())
  val c_drdy = RegInit(1.B)
  val p_srdy = RegInit(0.B)

  io.enq.ready := c_drdy
  io.deq.valid := p_srdy

  // State control
  val push_vld = io.enq.ready & io.enq.valid
  val pop_vld = io.deq.valid & io.deq.ready

  state := nxt_state

  switch (state) {
    is (s_0_0) {
      when (push_vld) {
        nxt_state := s_1_0
        c_drdy := 1.B
        p_srdy := 1.B
        nxt_send_sel := 1.B;
      }
    }

    is (s_1_0) {
      when (push_vld && io.deq.ready) {
        nxt_state := s_1_0;
      }.elsewhen  ((push_vld) && (!io.deq.ready)) {
        nxt_state := s_2_1;
      }.elsewhen  ((!push_vld) && (io.deq.ready)) {
        nxt_state := s_0_0
      }.elsewhen  ((!push_vld) && (!io.deq.ready)) {
        nxt_state := s_0_1;
      }
    }
    is (s_0_1) {
      when (push_vld && io.deq.ready) {
        nxt_state := s_1_0
      }.elsewhen  ((push_vld) && (!io.deq.ready)) {
        nxt_state := s_2_1
      }.elsewhen  ((!push_vld) && (io.deq.ready)) {
        nxt_state := s_0_0
      }.elsewhen  ((!push_vld) && (!io.deq.ready)) {
        nxt_state := s_0_1
      }
    }
    is (s_2_1) {
      when((!push_vld) && (io.deq.ready)) {
        nxt_state := s_1_0
      }
    }
  }

  switch (nxt_state) {
    is (s_0_0) {
      nxt_shift := 0.B
      nxt_load := 1.B
      c_drdy := 1.B
      p_srdy := 0.B
    }
    is (s_0_1) {
      nxt_load := 1.B
      c_drdy := 1.B
      p_srdy := 1.B
      nxt_send_sel := 0.B
    }
    is (s_1_0) {
      nxt_shift := 1.B
      nxt_load := 1.B
      c_drdy := 1.B
      p_srdy := 1.B
      nxt_send_sel := 1.B
    }
    is (s_2_1) {
      c_drdy := 0.B
      p_srdy := 1.B
      nxt_send_sel := 0.B
    }
  }

  shift := nxt_shift
  load := nxt_load
  send_sel := nxt_send_sel

  when (shift) {
    hold_0 := hold_1
  }
  when (load) {
    hold_1 := io.enq.bits
  }
  io.deq.bits := Mux(send_sel, hold_1, hold_0)
}
