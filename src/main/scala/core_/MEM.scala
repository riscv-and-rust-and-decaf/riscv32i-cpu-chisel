package core_

import chisel3._
import chisel3.util._

class MEM extends Module {
  val io = IO(new Bundle {
    val ex  = Flipped(new EX_MEM)
    val mmu = new RAMOp

    val wrRegOp   = new WrRegOp
    val wrCSROp   = new WrCSROp
  })

  // Stall
  io.ex.ready := io.mmu.ok

  // Lock input
  val ramOp       = RegNext(io.ex.ramOp,        init = 0.U.asTypeOf(new RAMOp_Output))
  val wregAddr    = RegNext(io.ex.wrRegOp.addr, init = 0.U(32.W))
  val exWrRegData = RegNext(io.ex.wrRegOp.data, init = 0.U(32.W))

  val isLoad    = RAMMode.isRead(ramOp.mode)
  val wregData  = Mux(isLoad, io.mmu.rdata, exWrRegData)

  io.mmu.addr  := ramOp.addr
  io.mmu.wdata := ramOp.wdata
  io.mmu.mode  := ramOp.mode

  io.wrRegOp.addr := wregAddr
  io.wrRegOp.rdy  := true.B
  io.wrRegOp.data := wregData

  //------------------- CSR ----------------------

  val wCSRAddr  = RegInit(0.U(12.W))
  val csrMode   = RegInit(0.U(2.W))
  val csrOldVal = RegInit(0.U(32.W))
  val csrRsVal  = RegInit(0.U(32.W))
  val csrNewVal = RegInit(0.U(32.W))

  wCSRAddr  := io.ex.wrCSROp.addr
  csrMode   := io.ex.wrCSROp.mode
  csrOldVal := io.ex.wrCSROp.oldVal
  csrRsVal  := io.ex.wrCSROp.rsVal
  csrNewVal := io.ex.wrCSROp.newVal
  
  io.wrCSROp.addr   := wCSRAddr
  io.wrCSROp.oldVal := csrOldVal
  io.wrCSROp.rsVal  := csrRsVal
  io.wrCSROp.mode   := csrMode
  io.wrCSROp.newVal := csrNewVal
}
