package core_

import chisel3._
import chisel3.util._


class IF extends Module {
  val io = IO(new Bundle {
    val ram = new RAMOp
    val id = new IF_ID
    val excep = new ExcepStatus

    val csrExcepPc = Input(UInt(32.W))
    val csrExcepEn = Input(Bool())
  })

  io.excep.en := false.B
  io.excep.code := 0.U

  val stall = !io.ram.ok || io.id.id_stall

  // pc bookkeeping
  val pc  = RegInit(Const.PC_INIT)
  io.excep.pc := pc
  val nextPC = PriorityMux(Seq(
    (io.csrExcepEn,   io.csrExcepPc),
    (io.id.if_branch, io.id.branch_tar),  // even if stalled, acknowledge branch
    (stall,           pc),                // when stalled, don't advance
    (true.B,          pc + 4.U)))
  pc := nextPC

  // instruction fetch
  io.ram.addr  := pc; // fetch current instruction
  io.ram.mode  := RAMMode.LW
  io.ram.wdata := 0.U

  // feed to ID
  io.id.pc   := pc
  io.id.inst := Mux(stall, Const.NOP_INST, io.ram.rdata)
}
