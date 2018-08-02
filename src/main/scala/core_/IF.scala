package core_

import chisel3._
import chisel3.util._


class IF extends Module {
  val io = IO(new Bundle {
    val mmu = new MMUOp
    val id = new IF_ID
  })

  val pc         = RegInit(Const.PC_INIT)
  val log_branch = RegInit(0.U.asTypeOf(Valid(UInt(32.W))))
  val log_inst   = RegInit(0.U.asTypeOf(Valid(UInt(32.W))))

  val mmu_inst = Wire(Valid(UInt(32.W)))
  mmu_inst.valid := io.mmu.ok
  mmu_inst.bits := io.mmu.rdata

  val branch = Mux(io.id.branch.valid, io.id.branch, log_branch)
  val inst   = Mux(log_inst.valid, log_inst, mmu_inst)

  // Log branch & inst
  when(io.id.branch.valid) {
    log_branch := io.id.branch
  }
  when(!log_inst.valid && io.mmu.ok) {
    log_inst.valid := true.B
    log_inst.bits := io.mmu.rdata
  }

  val stall = !inst.valid || !io.id.ready

  // Change status only when mmu.ok
  when(!stall) {
    pc := PriorityMux(Seq(
      (branch.valid, branch.bits),
      (true.B,       pc + 4.U)))
    log_branch := 0.U.asTypeOf(Valid(UInt(32.W))) // Clear branch log
    log_inst   := 0.U.asTypeOf(Valid(UInt(32.W))) // Clear inst log
  }

  // instruction fetch
  io.mmu.addr  := pc; // fetch current instruction
  io.mmu.mode  := Mux(log_inst.valid, RAMMode.NOP, RAMMode.LW)
  io.mmu.wdata := 0.U

  // Feed to ID: valid only when no stall && no branch

  // Default output
  io.id.inst  := Const.NOP_INST
  io.id.excep := 0.U.asTypeOf(new Exception)

  when(!(stall || branch.valid)) {
    io.id.inst := inst.bits
    io.id.excep.pc := pc
    io.id.excep.valid_inst := true.B
    when(pc(1,0).orR) {
      io.id.excep.valid := true.B
      io.id.excep.value := pc
      io.id.excep.code := Cause.InstAddressMisaligned
    }
    when(io.mmu.pageFault) {
      io.id.excep.valid := true.B
      io.id.excep.value := pc
      io.id.excep.code := Cause.InstPageFault
    }
  }
}
