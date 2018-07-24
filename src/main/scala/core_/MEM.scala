package core_

import chisel3._
import chisel3.util._

class MEM extends Module {
  val io = IO(new Bundle {
    val ex  = Flipped(new EX_MEM) 
    val mmu = new RAMOp

    val exWrRegOp = Flipped(new WrRegOp)
    val wrRegOp = new WrRegOp

    val exWrCSROp = Flipped(new WrCSROp)
    val wrCSROp = new WrCSROp
    
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

  val opt = RegInit(OptCode.ADD)
  opt := io.ex.opt
  val store_data = RegInit(0.U(32.W))
  store_data := io.ex.store_data
  val alu_out = RegInit(0.U(32.W))
  alu_out := io.ex.alu_out
  val wregAddr = RegInit(0.U(32.W))
  wregAddr := io.exWrRegOp.addr
  val exWrRegData = RegInit(0.U(32.W))
  exWrRegData := io.exWrRegOp.data

  val loadInsts = Seq(OptCode.LW, OptCode.LB, OptCode.LH,
    OptCode.LBU, OptCode.LHU)
  val isLoad = loadInsts.map(x => x === opt).reduce(_ || _)
  val wregData = Mux(isLoad, io.mmu.rdata, exWrRegData)

  io.mmu.addr  := alu_out
  io.mmu.wdata := store_data
  io.mmu.mode  := MuxLookup(opt, RAMMode.NOP, Seq(
    OptCode.LW  -> RAMMode.LW,
    OptCode.SW  -> RAMMode.SW,
    OptCode.LB  -> RAMMode.LB,
    OptCode.LBU -> RAMMode.LBU,
    OptCode.SB  -> RAMMode.SB,
    OptCode.LH  -> RAMMode.LH,
    OptCode.LHU -> RAMMode.LHU))

  io.wrRegOp.addr := wregAddr
  io.wrRegOp.rdy  := true.B
  io.wrRegOp.data := wregData


  val wCSRAddr  = RegInit(0.U(12.W))
  val csrMode   = RegInit(0.U(2.W))
  val csrOldVal = RegInit(0.U(32.W))
  val csrRsVal  = RegInit(0.U(32.W))
  val csrNewVal = RegInit(0.U(32.W))

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

  when(io.csrExcepEn) {
    excepEn := false.B
  }
  when(excepEn || io.csrExcepEn) {
    wregAddr := 0.U
    csrMode := 0.U
    io.mmu.mode := RAMMode.NOP 
  }
  io.excep.en   := excepEn
  io.excep.code := excepCode
  io.excep.pc   := excepPc
}
