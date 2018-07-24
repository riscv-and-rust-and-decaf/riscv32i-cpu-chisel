package core_

import chisel3._
import chisel3.util._
import OptCode._

class EX extends Module {
  val io = IO(new Bundle {
    val id  = Flipped(new ID_EX)
    val mem = new EX_MEM
    val wrRegOp = new WrRegOp
    val wrCSROp = new WrCSROp
  })

  // Stall
  io.id.ready := io.mem.ready

  //------------------- ALU ----------------------

  // Lock input
  val a = RegNext(io.id.oprd1, init=0.U(32.W))
  val b = RegNext(io.id.oprd2, init=0.U(32.W))
  val opt = RegNext(io.id.opt, init=OptCode.ADD)

  val shamt = b(4, 0)

  // NOTICE: SLL,SRL,SRA only use lower 5 bits of b
  val aluRes = MuxLookup(opt,
    a + b,
    Seq(
      ADD -> (a + b),
      SUB -> (a - b),
      SLT -> Mux(a.asSInt < b.asSInt, 1.U, 0.U),
      SLTU -> Mux(a < b, 1.U, 0.U),
      XOR -> (a ^ b),
      OR -> (a | b),
      AND -> (a & b),
      SLL -> (a << shamt),
      SRL -> (a >> shamt),
      SRA -> (a.asSInt >> shamt).asUInt
      // not necessary, all rest (a+b)
    )
  )
  io.mem.alu_out := aluRes

  //------------------- Reg ----------------------

  // Lock input
  val wregAddr = RegNext(io.id.wrRegOp.addr, init=0.U(5.W))
  val store_data = RegNext(io.id.store_data, init=0.U(32.W))

  io.wrRegOp.addr := wregAddr
  io.wrRegOp.data := aluRes
  io.wrRegOp.rdy := (opt & OptCode.LW) =/= OptCode.LW

  io.mem.opt       := opt
  io.mem.store_data := store_data

  //------------------- CSR ----------------------

  val wCSRAddr  = RegInit(0.U(12.W))
  val csrMode   = RegInit(0.U(2.W))
  val csrOldVal = RegInit(0.U(32.W))
  val csrRsVal  = RegInit(0.U(32.W))
  val csrNewVal = RegInit(0.U(32.W))

  wCSRAddr  := io.id.wrCSROp.addr
  csrMode   := io.id.wrCSROp.mode
  csrOldVal := io.id.wrCSROp.oldVal
  csrRsVal  := io.id.wrCSROp.rsVal
  csrNewVal := io.id.wrCSROp.newVal
  
  io.wrCSROp.addr   := wCSRAddr
  io.wrCSROp.oldVal := csrOldVal
  io.wrCSROp.rsVal  := csrRsVal
  io.wrCSROp.mode   := csrMode
  io.wrCSROp.newVal := MuxLookup(csrMode, 0.U, Seq(
    CSRMODE.RW -> csrRsVal,
    CSRMODE.RS -> (csrOldVal | csrRsVal),
    CSRMODE.RC -> (csrOldVal & ~csrRsVal)
  ))


}
