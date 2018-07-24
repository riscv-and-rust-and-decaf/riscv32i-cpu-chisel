package core_

import chisel3._
import chisel3.util._

class MEM extends Module {
  val io = IO(new Bundle {
    val ex  = Flipped(new EX_MEM)
    val mmu = new RAMOp

    val exWrRegOp = Flipped(new WrRegOp)
    val wrRegOp   = new WrRegOp

    val exWrCSROp = Flipped(new WrCSROp)
    val wrCSROp   = new WrCSROp
  })

  // Stall
  io.ex.ready := io.mmu.ok

  // Lock input
  val opt         = RegNext(io.ex.opt,          init = OptCode.ADD)
  val store_data  = RegNext(io.ex.store_data,   init = 0.U(32.W))
  val alu_out     = RegNext(io.ex.alu_out,      init = 0.U(32.W))
  val wregAddr    = RegNext(io.exWrRegOp.addr,  init = 0.U(32.W))
  val exWrRegData = RegNext(io.exWrRegOp.data,  init = 0.U(32.W))

  val loadInsts = Seq(OptCode.LW, OptCode.LB, OptCode.LH, OptCode.LBU, OptCode.LHU)
  val isLoad    = loadInsts.map(x => x === opt).reduce(_ || _)
  val wregData  = Mux(isLoad, io.mmu.rdata, exWrRegData)

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

  //------------------- CSR ----------------------

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
}
