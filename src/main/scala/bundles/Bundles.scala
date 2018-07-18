package bundles

import chisel3._
import chisel3.util._

// repesents an operation of "writing registers"
class WrRegOp extends Bundle {
  val addr = UInt(5.W)  // which register to write? if 0, then don't write
  val data = UInt(32.W)
  val rdy  = Bool()     // data might be ready in different stages e.g. EX and MEM
}

class IF_ID extends Bundle {
  val pc   = Output(UInt(32.W))
  val inst = Output(UInt(32.W))
  val if_branch  = Input(Bool())
  val branch_tar = Input(UInt(32.W))
}

class RAMOp extends Bundle {
  val addr  = Output(UInt(32.W))
  val mode  = Output(UInt(4.W))
  val wdata = Output(UInt(32.W)) 
  
  val rdata = Input(UInt(32.W))
}

class IFRAMOp extends RAMOp {
  val ifstall = Input(Bool())
}

class _Reg extends Bundle {
  val addr = Output(UInt(5.W))
  val data = Input(UInt(32.W))
}

class _CSR extends Bundle {
  val addr = Output(UInt(12.W))
  val wdata = Output(UInt(32.W))
  val rdata = Input(UInt(32.W))
  val mode = Output(UInt(2.W)) 
}

class ID_Reg extends Bundle {
  val read1 = new _Reg()
  val read2 = new _Reg()
}

class ID_EX extends Bundle {
  val oprd1 = Output(UInt(32.W))
  val oprd2 = Output(UInt(32.W))
  val opt   = Output(UInt(5.W))

  var store_data = Output(UInt(32.W)) // for Store Inst only
}

class EX_MEM extends Bundle {
  val alu_out = Output(UInt(32.W))
  val opt   = Output(UInt(5.W))
  
  var store_data = Output(UInt(32.W)) // for Store Inst only
}

class MEM_Reg extends Bundle {
  val addr = Output(UInt(5.W))
  val data = Output(UInt(32.W))
}

