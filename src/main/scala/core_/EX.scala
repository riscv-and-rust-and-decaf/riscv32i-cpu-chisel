package core_

import chisel3._
import chisel3.util._
import OptCode._

class EX extends Module {
  val io = IO(new Bundle {
    val id  = Flipped(new ID_EX)
    val mem = new EX_MEM
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

  //-------------- Reg & Ram Op ------------------

  // Lock input
  val wregAddr = RegNext(io.id.wrRegOp.addr, init=0.U(5.W))
  val store_data = RegNext(io.id.store_data, init=0.U(32.W))

  io.mem.wrRegOp.addr := wregAddr
  io.mem.wrRegOp.data := aluRes
  io.mem.wrRegOp.rdy := (opt & OptCode.LW) =/= OptCode.LW

  io.mem.ramOp.addr := aluRes
  io.mem.ramOp.mode := MuxLookup(opt, RAMMode.NOP, Seq(
    OptCode.LW  -> RAMMode.LW,
    OptCode.SW  -> RAMMode.SW,
    OptCode.LB  -> RAMMode.LB,
    OptCode.LBU -> RAMMode.LBU,
    OptCode.SB  -> RAMMode.SB,
    OptCode.LH  -> RAMMode.LH,
    OptCode.LHU -> RAMMode.LHU))
  io.mem.ramOp.wdata := store_data

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
  
  io.mem.wrCSROp.addr   := wCSRAddr
  io.mem.wrCSROp.oldVal := csrOldVal
  io.mem.wrCSROp.rsVal  := csrRsVal
  io.mem.wrCSROp.mode   := csrMode
  io.mem.wrCSROp.newVal := MuxLookup(csrMode, 0.U, Seq(
    CSRMODE.RW -> csrRsVal,
    CSRMODE.RS -> (csrOldVal | csrRsVal),
    CSRMODE.RC -> (csrOldVal & ~csrRsVal)
  ))


}
