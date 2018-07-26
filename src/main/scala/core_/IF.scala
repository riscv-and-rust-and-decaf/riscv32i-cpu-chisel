package core_

import chisel3._
import chisel3.util._


class IF extends Module {
  val io = IO(new Bundle {
    val mmu = new RAMOp
    val id = new IF_ID
    val excep = new ExcepStatus

    val csrNewPc = Input(UInt(32.W))
    val csrFlush = Input(Bool())
  })

  io.excep.en := false.B
  io.excep.code := 0.U

  val stall = !io.mmu.ok || !io.id.ready

  val pc      = RegInit(Const.PC_INIT)
  val branch  = RegInit(0.U.asTypeOf(Valid(UInt(32.W))))

  io.excep.pc := pc

  // Log branch
  when(io.csrFlush) {
    branch.valid := true.B
    branch.bits := io.csrNewPc
  }
  .elsewhen(io.id.branch.valid) {
    branch := io.id.branch
  }

  // Change status only when mmu.ok
  when(!stall) {
    pc := PriorityMux(Seq(
      (io.csrFlush,       io.csrNewPc),
      (io.id.branch.valid,  io.id.branch.bits),
      (branch.valid,        branch.bits),
      (true.B,              pc + 4.U)))
    branch := 0.U.asTypeOf(Valid(UInt(32.W))) // Clear branch log
  }

  // instruction fetch
  io.mmu.addr  := pc; // fetch current instruction
  io.mmu.mode  := RAMMode.LW
  io.mmu.wdata := 0.U

  // Feed to ID: valid only when no stall && no branch
  when(stall || branch.valid) {
    io.id.pc   := 0.U
    io.id.inst := Const.NOP_INST
  }.otherwise {
    io.id.pc   := pc
    io.id.inst := io.mmu.rdata
  }
}
