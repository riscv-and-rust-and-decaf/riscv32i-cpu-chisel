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

  // Lock input
  val ramOp       = RegNext(io.ex.ramOp,        init = 0.U.asTypeOf(new RAMOp_Output))
  val wregAddr    = RegNext(io.ex.wrRegOp.addr, init = 0.U(32.W))
  val exWrRegData = RegNext(io.ex.wrRegOp.data, init = 0.U(32.W))

  io.mmu.addr  := ramOp.addr
  io.mmu.wdata := ramOp.wdata
  io.mmu.mode  := ramOp.mode

  // Stall
  val stall = ramOp.mode =/= RAMMode.NOP && !io.mmu.ok

  // Output
  io.wrRegOp.addr := Mux(stall, 0.U, wregAddr)
  io.wrRegOp.rdy  := true.B
  io.wrRegOp.data := Mux(RAMMode.isRead(ramOp.mode), io.mmu.rdata, exWrRegData)

  io.ex.ready := !stall

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
