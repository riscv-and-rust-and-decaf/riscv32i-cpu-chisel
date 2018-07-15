import chisel3._
import chisel3.util._
import bundles._

import Const._

class MEM extends Module {
  val io = IO(new Bundle {
    val _EX  = Flipped(new EX_MEM()) 
    val _MMU = new RAMOp()
    val _Reg = new MEM_Reg()

    val exWrRegOp = Input(new WrRegOp())
    val wrRegOp = Output(new WrRegOp())
  })

  val opt = RegInit(OptCode.ADD)
  opt := io._EX.opt
  val store_data = RegInit(0.U(32.W))
  store_data := io._EX.store_data
  val alu_out = RegInit(0.U(32.W))
  alu_out := io._EX.alu_out
  val wregAddr = RegInit(0.U(32.W))
  wregAddr := io.exWrRegOp.addr
  val wregData = RegInit(0.U(32.W))
  wregData := Mux(
    (io._EX.opt & OptCode.LW) === OptCode.LW, // must use io.opt here
    io._MMU.rdata,
    io.exWrRegOp.data
  )

  io._MMU.addr  := alu_out
  io._MMU.wdata := store_data
  io._MMU.mode  := Mux(
    opt(4).toBool,
    opt(3,0),
    0.U(4.W)
  )

  io.wrRegOp.addr := wregAddr
  io.wrRegOp.rdy  := true.B
  io.wrRegOp.data := wregData

  io._Reg.addr := wregAddr
  io._Reg.data := wregData
}
