package bundles

import chisel3._
import chisel3.util._


class IF_ID extends Bundle {
  val pc   = Output(UInt(32.W))
  val inst = Output(UInt(32.W))
}

class _MMU extends Bundle {
  val addr  = Output(UInt(32.W))
  val mode  = Output(UInt(4.W))
  val wdata = Output(UInt(32.W)) 
  
  val rdata = Input(UInt(32.W))
}

class IF_MMU extends _MMU {
  val ifstall = Input(Bool())
}

class _Reg extends Bundle {
  val addr = Output(UInt(5.W))
  val data = Input(UInt(32.W))
}

class ID_Reg extends Bundle {
  val read1 = new _Reg()
  val read2 = new _Reg()
}

class ID_IF extends Bundle {
  val if_branch  = Output(Bool())
  val branch_tar = Output(Bool())
}

class ID_EX extends Bundle {
  val oprd1 = Output(UInt(32.W))
  val oprd2 = Output(UInt(32.W))
  val opt   = Output(UInt(4.W))

  val reg_w_add = Output(UInt(5.W))
}

class EX_MEM extends Bundle {
  val alu_out = Output(UInt(32.W))
  val opt   = Output(UInt(4.W))
  
  val reg_w_add = Output(UInt(32.W))
}

class MEM_Reg extends Bundle {
  val addr = Output(UInt(5.W))
  val data = Output(UInt(32.W))
}



