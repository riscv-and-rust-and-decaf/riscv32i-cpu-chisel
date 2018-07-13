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

  io._Reg.addr := io._EX.reg_w_add
  io._Reg.data := Mux(
    (io._EX.opt & OptCode.LW) === OptCode.LW,
    io._MMU.rdata,
    io._EX.alu_out
  )

  io._MMU.addr  := io._EX.alu_out
  io._MMU.wdata := io._EX.store_data
  io._MMU.mode  := Mux(
    io._EX.opt(4).toBool,
    io._EX.opt(3,0),
    0.U(4.W)
  )
}

  
