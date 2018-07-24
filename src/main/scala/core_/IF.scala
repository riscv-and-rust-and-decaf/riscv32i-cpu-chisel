package core_

import chisel3._
import chisel3.util._


class IF extends Module {
  val io = IO(new Bundle {
    val mmu = new RAMOp()
    val id  = new IF_ID()
  })

  val stall = !io.mmu.ok || !io.id.ready

  // pc bookkeeping
  val pc  = RegInit(Const.PC_INIT)
  val nextPC = PriorityMux(Seq(
    (io.id.branch.valid, io.id.branch.bits),  // even if stalled, acknowledge branch
    (stall,              pc),                // when stalled, don't advance
    (true.B,             pc + 4.U)))
  pc := nextPC

  // instruction fetch
  io.mmu.addr  := pc; // fetch current instruction
  io.mmu.mode  := RAMMode.LW
  io.mmu.wdata := 0.U

  // feed to ID
  io.id.pc   := pc
  io.id.inst := Mux(stall, Const.NOP_INST, io.mmu.rdata)
}
