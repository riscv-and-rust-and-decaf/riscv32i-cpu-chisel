package core_

import chisel3._
import chisel3.util._

class MEM extends Module {
  val io = IO(new Bundle {
    val ex  = Flipped(new EX_MEM)
    val mmu = new RAMOp
   
    val wrRegOp   = new WrRegOp
    val wrCSROp   = new WrCSROp

    val xRet      = Output(Valid(UInt(2.W)))
    //exception
    val exExcep = Flipped(new ExcepStatus)
    val excep  = new ExcepStatus // to CSR

    val csrFlush = Input(Bool())
  })
  
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
  io.wrCSROp := wrCSROp

  val xRet = RegInit(0.U.asTypeOf(new Valid(UInt(2.W))))
  when(!stall) {
    xRet := io.ex.xRet
  }
  io.xRet := xRet
  
  val excep = RegInit(0.U.asTypeOf(new ExcepStatus))
  when(!stall) {
    excep := io.exExcep
  }
  io.excep :=excep
  /*
  val excepEn = RegInit(false.B)
  val excepCode = RegInit(0.U(32.W))
  val excepPc  = RegInit(0.U(32.W))
  excepEn   := io.exExcep.en
  excepCode := io.exExcep.code
  excepPc   := io.exExcep.pc
  */

  when(io.csrFlush) {
    excep.en := false.B
    wregAddr := 0.U
    wrCSROp.mode := CSRMODE.NOP
    ramOp.mode := RAMMode.NOP 
    printf("! exception come, flushed (0x%x)\n", excep.pc);
  }
  when(excep.en) {
    io.wrRegOp.addr := 0.U
    io.wrCSROp.mode := CSRMODE.NOP
    io.mmu.mode := RAMMode.NOP
    printf("! Exception Pc: 0x%x ExcepCode: %d\n", excep.pc, excep.code);
  }

  //printf("Pc: 0x%x (WrRegAddr) [%d <- %d]\n", excepPc, io.wrRegOp.addr, io.wrRegOp.data);
/*
  io.excep.en   := excepEn
  io.excep.code := excepCode
  io.excep.pc   := excepPc
*/
}
