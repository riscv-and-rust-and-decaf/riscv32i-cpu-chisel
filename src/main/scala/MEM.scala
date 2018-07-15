import chisel3._
import chisel3.util._
import bundles._

import Const._

class MEM extends Module {
  val io = IO(new Bundle {
    val _EX  = Flipped(new EX_MEM()) 
    val _Reg = new MEM_Reg() 
    val _MMU = new RAMOp()
  })

  val reg_w_add = RegInit(0.U(32.W))
  reg_w_add := io._EX.reg_w_add
  val opt = RegInit(OptCode.ADD)
  opt := io._EX.opt
  val alu_out = RegInit(0.U(32.W))
  alu_out := io._EX.alu_out
  val store_data = RegInit(0.U(32.W))
  store_data := io._EX.store_data

  io._Reg.addr := reg_w_add
  io._Reg.data := Mux(
    (opt & OptCode.LW) === OptCode.LW,
    io._MMU.rdata,
    alu_out
  )

  io._MMU.addr  := alu_out
  io._MMU.wdata := store_data
  io._MMU.mode  := Mux(
    opt(4).toBool,
    opt(3,0),
    0.U(4.W)
  )
}

  
