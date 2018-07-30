package core_

import chisel3._
import chisel3.util._

class MEM extends Module {
  val io = IO(new Bundle {
    val ex  = Flipped(new EX_MEM)
    val mmu = new MMUOp
    val csr = new MEM_CSR
    val reg = new WrRegOp

    val flush = Input(Bool())
  })

  // Lock input
  val ramOp   = RegInit(0.U.asTypeOf(new RAMOp_Output))
  val reg     = RegInit(0.U.asTypeOf(new WrRegOp))
  val wrCSROp = RegInit(0.U.asTypeOf(new WrCSROp))
  val excep   = RegInit(0.U.asTypeOf(new Exception))

  // Stall
  val stall = ramOp.mode =/= RAMMode.NOP && !io.mmu.ok
  io.ex.ready := !stall

  when(!stall) {
    ramOp := io.ex.ramOp
    reg := io.ex.wrRegOp
    wrCSROp := io.ex.wrCSROp
    excep := io.ex.excep
  }

  // Default Output
  io.mmu.addr  := ramOp.addr
  io.mmu.wdata := ramOp.wdata
  io.mmu.mode  := ramOp.mode

  io.reg.addr := reg.addr
  io.reg.rdy  := true.B
  io.reg.data := Mux(RAMMode.isRead(ramOp.mode), io.mmu.rdata, reg.data)

  io.csr.wrCSROp := wrCSROp
  io.csr.excep := excep

  // PageFault
  when(io.mmu.pageFault) {
    io.csr.excep.valid := true.B
    io.csr.excep.code := Mux(RAMMode.isRead(ramOp.mode), Cause.LoadPageFault, Cause.StorePageFault)
  }

  // Handle Exception
  when(io.csr.excep.valid) {
    io.reg.addr := 0.U
    io.csr.wrCSROp.valid := false.B
    //printf("[MEM] ! Exception Pc: 0x%x Excep: %d\n", excepPc, excepEn)
  }
  when(excep.valid) {
    // Avoid combinational loop
    io.mmu.mode := RAMMode.NOP
  }

  // Stall, output null
  when(stall) {
    io.csr.excep.valid_inst := false.B
    io.csr.excep.valid := false.B
    io.csr.wrCSROp.valid := false.B
    io.reg.addr := 0.U
  }

  // Handle flush
  when(io.flush) {
    excep.valid := false.B
    excep.valid_inst := false.B
    reg.addr := 0.U
    wrCSROp.valid := false.B
    ramOp.mode := RAMMode.NOP
  }
}

