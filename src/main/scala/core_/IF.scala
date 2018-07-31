package core_

import chisel3._
import chisel3.util._


class IF extends Module {
  val io = IO(new Bundle {
    val mmu = new MMUOp
    val id = new IF_ID
  })

  val stall = !io.mmu.ok || !io.id.ready

  val pc      = RegInit(Const.PC_INIT)
  val branch  = RegInit(0.U.asTypeOf(Valid(UInt(32.W))))

  // Log branch
  when(io.id.branch.valid) {
    branch := io.id.branch
  }

  // Change status only when mmu.ok
  when(!stall) {
    pc := PriorityMux(Seq(
      (io.id.branch.valid,  io.id.branch.bits),
      (branch.valid,        branch.bits),
      (true.B,              pc + 4.U)))
    branch := 0.U.asTypeOf(Valid(UInt(32.W))) // Clear branch log
  }

  // instruction fetch
  io.mmu.addr  := pc; // fetch current instruction
  io.mmu.mode  := Mux(reset.toBool, RAMMode.NOP, RAMMode.LW)
  io.mmu.wdata := 0.U


  // Feed to ID: valid only when no stall && no branch

  // Default output
  io.id.inst  := Const.NOP_INST
  io.id.excep := 0.U.asTypeOf(new Exception)

  when(!(stall || branch.valid || io.id.branch.valid)) {
    io.id.inst := io.mmu.rdata
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
