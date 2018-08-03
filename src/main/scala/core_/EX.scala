package core_

import chisel3._
import chisel3.util._
import OptCode._

class EX extends Module {
  val io = IO(new Bundle {
    val id  = Flipped(new ID_EX)
    val mem = new EX_MEM

    val flush = Input(Bool())
  })

  //------------------- ALU ----------------------

  // Lock input
  val alu = RegNext(io.id.aluOp, init=0.U.asTypeOf(new ALUOp))

  // Alias
  val a   = alu.rd1
  val b   = alu.rd2
  val opt = alu.opt
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
  val wregAddr   = RegNext(io.id.wrRegOp.addr, init=0.U(5.W))
  val store_data = RegNext(io.id.store_data,   init=0.U(32.W))

  io.mem.wrRegOp.addr := wregAddr
  io.mem.wrRegOp.data := aluRes
  io.mem.wrRegOp.rdy  := (opt & OptCode.LW) =/= OptCode.LW

  io.mem.ramOp.addr := aluRes
  io.mem.ramOp.mode := MuxLookup(opt, RAMMode.NOP, Seq(
    OptCode.LW  -> RAMMode.LW,
    OptCode.LB  -> RAMMode.LB,
    OptCode.LBU -> RAMMode.LBU,
    OptCode.LH  -> RAMMode.LH,
    OptCode.LHU -> RAMMode.LHU,
    OptCode.SW  -> RAMMode.SW,
    OptCode.SH  -> RAMMode.SH,
    OptCode.SB  -> RAMMode.SB
  ))
  io.mem.ramOp.wdata := store_data

  //------------------- CSR ----------------------

  val excep   = RegNext(io.id.excep)
  val wrCSROp = RegNext(io.id.wrCSROp)
  io.mem.excep   := excep
  io.mem.wrCSROp := wrCSROp

  //----------------- status ---------------------

  val flush = io.flush
  val stall = !io.mem.ready

  // EX stall => ID stall. So no need to keep registers.
  io.id.ready := !stall

  when(stall) {
    alu := alu
    wregAddr := wregAddr
    store_data := store_data
    excep := excep
    wrCSROp := wrCSROp
  }

  when(flush) {
    opt := OptCode.ADD
    wregAddr := 0.U
    wrCSROp.valid := false.B
    excep.valid := false.B
    excep.valid_inst := false.B
  }

}
