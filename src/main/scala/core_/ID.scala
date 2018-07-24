package core_

import chisel3._
import chisel3.util._

class IDState extends Bundle {
  val bt = Output(UInt(5.W))
  val l = Output(Bool())
  val type_ = Output(UInt(3.W))
  val opt = Output(UInt(5.W))
  val pc = Output(UInt(5.W))
  val imm = Output(SInt(32.W))

}

class ID extends Module {
  val io = IO(new Bundle {
    val iff = Flipped(new IF_ID)  // naming conflict if use `if`
    val reg = new ID_Reg
    val ex = new ID_EX
    val wrRegOp = new WrRegOp
    val wrCSROp = new WrCSROp
    val csr = new ID_CSR
  
    //exception
    val ifExcep = Flipped(new ExcepStatus)
    val excep  = new ExcepStatus
  
    val csrExcepEn = Input(Bool())

    // forwarding
    val exWrRegOp = Flipped(new WrRegOp)
    val memWrRegOp = Flipped(new WrRegOp)

    val exWrCSROp = Flipped(new WrCSROp)
    val memWrCSROp = Flipped(new WrCSROp)

    //output log
    val debug = new IDState()
  })

  val excepEn = RegInit(false.B)
  val excepCode = RegInit(0.U(32.W))
  val excepPc  = RegInit(0.U(32.W))
  excepEn   := io.ifExcep.en
  excepCode := io.ifExcep.code
  excepPc   := io.ifExcep.pc


  val flush = io.csrExcepEn

  val d = io.debug

  val inst = RegInit(Const.NOP_INST)
  // If ID is stalling, the current instruction is not executed in this cycle.
  //    (i.e. current instruction is `flushed')
  // Therefore, on the next cycle, ID must execute the same instruction.
  // However when IF sees a `stall', it simply gives out a `nop'.
  // As a result, ID should not update (receive from IF) its instruction
  //  when stalled.
  when (!io.iff.id_stall) {
    inst := Mux(io.iff.if_branch, Const.NOP_INST, io.iff.inst)
  }

  when(flush) {
    inst := Const.NOP_INST
  }

  val pc = RegInit(0.U(32.W))
  pc := io.iff.pc

  d.pc := pc

  val decRes = ListLookup(inst, DecTable.defaultDec, DecTable.decMap)
  val instType = decRes(DecTable.TYPE)

  val rs1Addr  = inst(19, 15)
  val rs2Addr  = inst(24, 20)
  val rdAddr   = inst(11, 7)

  val csrAddr = inst(31,20)

  val imm = Wire(SInt(32.W))

  d.imm := imm
  
  //Get regVar and  forwarding

  io.reg.read1.addr := rs1Addr
  io.reg.read2.addr := rs2Addr

  val exWrRegOp = io.exWrRegOp
  val memWrRegOp = io.memWrRegOp
  
  val exWrCSROp = io.exWrCSROp
  val memWrCSROp = io.memWrCSROp
  
  val rs1Val = PriorityMux(Seq(
    (!rs1Addr.orR,                0.U),                // reading x0 always gives 0
    (rs1Addr === exWrRegOp.addr,  exWrRegOp.data),     // forwarding from last instruction
    (rs1Addr === memWrRegOp.addr, memWrRegOp.data),    // from last but one
    (true.B,                      io.reg.read1.data))) // from the register file
  val rs2Val = PriorityMux(Seq(
    (!rs2Addr.orR,                0.U),                // reading x0 always gives 0
    (rs2Addr === exWrRegOp.addr,  exWrRegOp.data),     // forwarding from last instruction
    (rs2Addr === memWrRegOp.addr, memWrRegOp.data),    // from last but one
    (true.B,                      io.reg.read2.data))) // from the register file

  val csrVal = PriorityMux(Seq(
    (exWrCSROp.addr === csrAddr && exWrCSROp.mode.orR,   exWrCSROp.newVal),
    (memWrCSROp.addr === csrAddr && memWrCSROp.mode.orR, memWrCSROp.newVal),
    (true.B ,                                            io.csr.rdata)))

  d.bt := 0.U
  d.l := false.B

  //Null Init
  io.iff.if_branch  := false.B
  io.iff.branch_tar := 0.U

  io.ex.oprd1 := 0.U
  io.ex.oprd2 := 0.U
  io.ex.opt := decRes(DecTable.OPT)
  io.ex.store_data := 0.U
  val wregAddr = Wire(UInt(5.W))
  wregAddr := 0.U

  io.csr.addr := 0.U

  io.wrCSROp.mode := 0.U
  io.wrCSROp.addr := csrAddr // don't care when mode==0
  io.wrCSROp.oldVal := csrVal
  io.wrCSROp.rsVal := 0.U
  io.wrCSROp.newVal := 0.U

  imm := 0.S

  // deal with different kind inst

  d.type_ := instType
  d.opt := decRes(DecTable.OPT)

  // read-after-load data hazard
  val instTypesUsingRs1 = Seq(InstType.R, InstType.I, InstType.S, InstType.B)
  val instTypesUsingRs2 = Seq(InstType.R, InstType.S, InstType.B)
  val rs1Hazard = (rs1Addr === exWrRegOp.addr) && 
    instTypesUsingRs1.map(x => x === instType).reduce(_ || _)
  val rs2Hazard = (rs2Addr === exWrRegOp.addr) && 
    instTypesUsingRs2.map(x => x === instType).reduce(_ || _)
  val stall = (!exWrRegOp.rdy) && (exWrRegOp.addr.orR) && (rs1Hazard || rs2Hazard)

  when (stall) {
    // flush current instruction
    wregAddr := 0.U             // don't write registers
    io.ex.opt := OptCode.ADD    // don't write memory
    io.iff.if_branch := false.B // don't branch
    io.iff.id_stall := true.B   // tell IF not to advance
  } .otherwise {
    io.iff.id_stall := false.B
    switch(instType) {
      is(InstType.R) {
        io.ex.oprd1 := rs1Val
        io.ex.oprd2 := rs2Val
        wregAddr := rdAddr
      }
      is(InstType.I) {
        imm := inst(31,20).asSInt
        io.ex.oprd1 := rs1Val
        io.ex.oprd2 := imm.asUInt
        wregAddr := rdAddr

        when(decRes(DecTable.OPT) === OptCode.JALR) {
          io.iff.branch_tar := (imm.asUInt + rs1Val) & (~ 1.U(32.W))
          io.iff.if_branch  := true.B

          io.ex.oprd1 := pc
          io.ex.oprd2 := 4.U
          io.ex.opt   := OptCode.ADD
        }
      }
      is(InstType.S) {
        imm := Cat(inst(31,25), inst(11,7)).asSInt
        io.ex.oprd1 := rs1Val
        io.ex.oprd2 := imm.asUInt
        io.ex.store_data := rs2Val
      }
      is(InstType.B) {
        imm := Cat( inst(31), inst(7), inst(30,25), inst(11,8), 0.U(1.W)).asSInt
        io.iff.branch_tar := pc + imm.asUInt
        val bt = decRes(DecTable.OPT)
        val l = Mux(bt(0), rs1Val.asSInt < rs2Val.asSInt, rs1Val < rs2Val)
        val g = Mux(bt(0), rs1Val.asSInt > rs2Val.asSInt, rs1Val > rs2Val)
        val e = (rs1Val === rs2Val)
        io.iff.if_branch := (l & bt(3)) | (e & bt(2)) | (g & bt(1))

        io.ex.opt := OptCode.ADD

        d.bt := bt
        d.l := l
      }
      is(InstType.U) {
        imm := (inst & "h_fffff000".U).asSInt
        io.ex.oprd1 := imm.asUInt;
        val ut = decRes(DecTable.OPT)
        io.ex.oprd2 := Mux(ut(0), pc, 0.U)
        io.ex.opt   := OptCode.ADD
        wregAddr := rdAddr
      }
      is(InstType.J) {
        imm := Cat(inst(31), inst(19,12), inst(20), inst(30,21), 0.U(1.W)).asSInt
        io.iff.branch_tar := pc + imm.asUInt
        io.iff.if_branch  := true.B

        io.ex.oprd1 := pc
        io.ex.oprd2 := 4.U
        //io.ex.opt   := OptCode.ADD //not necessary
        wregAddr := rdAddr
      }
      is(InstType.SYS) {
        val fct3 = inst(14,12)

        when(fct3.orR) {
          io.csr.addr := csrAddr
          io.ex.oprd1 := csrVal

          io.wrCSROp.mode := fct3(1,0)
          io.wrCSROp.rsVal := Mux(fct3(2), rs1Addr, rs1Val)

          wregAddr := rdAddr
        }
        .otherwise {
          //TODO:  EBREAK
          when(!excepEn) {
            excepEn := true.B
            excepCode := 8.U
          }
        }

      }
      is(InstType.BAD) {
        when(!excepEn) {
          excepEn := true.B
          excepCode := 2.U
        }
      }
    }
  }

  io.wrRegOp.addr := wregAddr
  io.wrRegOp.data := 0.U
  io.wrRegOp.rdy  := false.B

  io.excep.en   := excepEn
  io.excep.code := excepCode
  io.excep.pc   := excepPc
}
