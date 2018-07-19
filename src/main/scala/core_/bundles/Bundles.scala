package bundles

import chisel3._
import chisel3.util._

// repesents an operation of "writing registers", when data might not be ready
class WrRegOp extends Bundle {
  val addr = Output(UInt(5.W)) // if 0 then don't write
  val data = Output(UInt(32.W))
  val rdy  = Output(Bool())     // data might be ready in different stages e.g. EX and MEM
}

// represents an operation of "ram access"
class RAMOp extends Bundle {
  val addr  = Output(UInt(32.W))
  val mode  = Output(UInt(4.W))   // Consts.scalaRAMMode.XX
  val wdata = Output(UInt(32.W))

  val rdata = Input(UInt(32.W))
}

// represents an operation of "reading registers"
class RdRegOp extends Bundle {
  val addr = Output(UInt(5.W))
  val data = Input(UInt(32.W))
}

class IF_ID extends Bundle {
  val pc   = Output(UInt(32.W))
  val inst = Output(UInt(32.W))
  val if_branch  = Input(Bool())
  val branch_tar = Input(UInt(32.W))
}

class ID_Reg extends Bundle {
  val read1 = new RdRegOp()
  val read2 = new RdRegOp()
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

