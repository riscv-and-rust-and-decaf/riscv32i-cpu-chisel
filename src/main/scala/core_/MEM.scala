package core_

import chisel3._
import chisel3.util._

class MEM extends Module {
  val io = IO(new Bundle {
    val ex  = Flipped(new EX_MEM)
    val mmu = new RAMOp
   
    val wrRegOp   = new WrRegOp
    val wrCSROp   = new WrCSROp

    //exception
    val exExcep = Flipped(new ExcepStatus)
    val excep  = new ExcepStatus // to CSR

    val csrExcepEn = Input(Bool())
  })
  
  val excepEn = RegInit(false.B)
  val excepCode = RegInit(0.U(32.W))
  val excepPc  = RegInit(0.U(32.W))
  excepEn   := io.exExcep.en
  excepCode := io.exExcep.code
  excepPc   := io.exExcep.pc

  // Lock input
  val ramOp       = RegInit(0.U.asTypeOf(new RAMOp_Output))
  val wregAddr    = RegInit(0.U(32.W))
  val exWrRegData = RegInit(0.U(32.W))

  // Stall
  val stall = ramOp.mode =/= RAMMode.NOP && !io.mmu.ok

  when(!stall) {
    ramOp := io.ex.ramOp
    wregAddr := io.ex.wrRegOp.addr
    exWrRegData := io.ex.wrRegOp.data
  }

  io.mmu.addr  := ramOp.addr
  io.mmu.wdata := ramOp.wdata
  io.mmu.mode  := ramOp.mode

  // Output
  io.wrRegOp.addr := Mux(stall, 0.U, wregAddr)
  io.wrRegOp.rdy  := true.B
  io.wrRegOp.data := Mux(RAMMode.isRead(ramOp.mode), io.mmu.rdata, exWrRegData)

  io.ex.ready := !stall

  //------------------- CSR ----------------------

  val wrCSROp = RegInit(0.U.asTypeOf(new WrCSROp))
  when(!stall) {
    wrCSROp := io.ex.wrCSROp
  }

/*
  wCSRAddr  := io.exWrCSROp.addr
  csrMode   := io.exWrCSROp.mode
  csrOldVal := io.exWrCSROp.oldVal
  csrRsVal  := io.exWrCSROp.rsVal
  csrNewVal := io.exWrCSROp.newVal
  
  io.wrCSROp.addr   := wCSRAddr
  io.wrCSROp.oldVal := csrOldVal
  io.wrCSROp.rsVal  := csrRsVal
  io.wrCSROp.mode   := csrMode
  io.wrCSROp.newVal := csrNewVal
*/
  when(io.csrExcepEn) {
    excepEn := false.B
  }
  when(excepEn || io.csrExcepEn) {
    wregAddr := 0.U
    wrCSROp.mode := 0.U
    //csrMode := 0.U
    io.mmu.mode := RAMMode.NOP // TODO: change Reg opt instead of mmu.mode
    printf("! Exception [%d] Pc: 0x%x Excep: %d\n", wregAddr, excepPc, excepEn);
  }
  printf("Pc: 0x%x (WrRegAddr) [%d <- %d]\n", excepPc, io.wrRegOp.addr, io.wrRegOp.data);
  io.excep.en   := excepEn
  io.excep.code := excepCode
  io.excep.pc   := excepPc

  io.wrCSROp := wrCSROp
}
