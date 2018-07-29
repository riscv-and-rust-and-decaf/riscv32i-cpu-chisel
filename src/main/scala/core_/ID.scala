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
    val csr = new ID_CSR
  
    val flush = Input(Bool())

    // forwarding
    val exWrRegOp = Flipped(new WrRegOp)
    val memWrRegOp = Flipped(new WrRegOp)

    val exWrCSROp = Flipped(new WrCSROp)
    val memWrCSROp = Flipped(new WrCSROp)

    //output log
    val debug = new IDState()
  })

  val flush = io.flush

  val inst = RegInit(Const.NOP_INST)
  val pc = RegInit(0.U(32.W))
  // If ID is stalling, the current instruction is not executed in this cycle.
  //    (i.e. current instruction is `flushed')
  // Therefore, on the next cycle, ID must execute the same instruction.
  // However when IF sees a `stall', it simply gives out a `nop'.
  // As a result, ID should not update (receive from IF) its instruction
  //  when stalled.

  // Decode instruction
  val decRes   = ListLookup(inst, DecTable.defaultDec, DecTable.decMap)
  val instType = decRes(DecTable.TYPE)
  val rs1Addr  = inst(19, 15)
  val rs2Addr  = inst(24, 20)
  val rdAddr   = inst(11, 7)
  val csrAddr  = inst(31,20)
  val imm      = Wire(SInt(32.W)) // Assign later

  // Read from RegFile
  io.reg.read1.addr := rs1Addr
  io.reg.read2.addr := rs2Addr
  io.csr.addr := csrAddr

  // Final register value (include forwarding)
  val rs1Val = PriorityMux(Seq(
    (rs1Addr === 0.U,                 0.U),                // reading x0 always gives 0
    (rs1Addr === io.exWrRegOp.addr,   io.exWrRegOp.data),  // forwarding from EX
    (rs1Addr === io.memWrRegOp.addr,  io.memWrRegOp.data), // forwarding from MEM
    (true.B,                          io.reg.read1.data))) // from the register file
  val rs2Val = PriorityMux(Seq(
    (rs2Addr === 0.U,                 0.U),                // reading x0 always gives 0
    (rs2Addr === io.exWrRegOp.addr,   io.exWrRegOp.data),  // forwarding from EX
    (rs2Addr === io.memWrRegOp.addr,  io.memWrRegOp.data), // forwarding from MEM
    (true.B,                          io.reg.read2.data))) // from the register file
  val csrVal = PriorityMux(Seq(
    (csrAddr === io.exWrCSROp.addr && io.exWrCSROp.valid,   io.exWrCSROp.data),
    (csrAddr === io.memWrCSROp.addr && io.memWrCSROp.valid, io.memWrCSROp.data),
    (true.B,                                                io.csr.rdata)))

  // Debug
  val d = io.debug
  d.pc := pc
  d.imm := imm
  d.bt := 0.U
  d.l := false.B
  d.type_ := instType
  d.opt := decRes(DecTable.OPT)

  // Default output
  io.iff.branch := 0.U.asTypeOf(Valid(UInt(32.W)))
  io.ex.aluOp := 0.U.asTypeOf(new ALUOp)
  io.ex.aluOp.opt := decRes(DecTable.OPT)
  io.ex.wrCSROp := 0.U.asTypeOf(new WrCSROp)
  io.ex.wrRegOp := 0.U.asTypeOf(new WrRegOp)
  io.ex.xRet := 0.U.asTypeOf(Valid(UInt(32.W)))
  io.ex.excep := 0.U.asTypeOf(new Exception)
  io.ex.store_data := 0.U
  imm := 0.S
  // OPTIMIZE: Better way to set io.ex = 0 ?

  // deal with different kind inst

  // read-after-load data hazard
  val instTypesUsingRs1 = Seq(InstType.R, InstType.I, InstType.S, InstType.B)
  val instTypesUsingRs2 = Seq(InstType.R, InstType.S, InstType.B)
  val rs1Hazard = (rs1Addr === io.exWrRegOp.addr) &&
    instTypesUsingRs1.map(x => x === instType).reduce(_ || _)
  val rs2Hazard = (rs2Addr === io.exWrRegOp.addr) &&
    instTypesUsingRs2.map(x => x === instType).reduce(_ || _)
  val stall = (!io.exWrRegOp.rdy) && (io.exWrRegOp.addr.orR) && (rs1Hazard || rs2Hazard) || !io.ex.ready
  
  val excep = RegInit(0.U.asTypeOf(new Exception))
  when(!stall) {
    excep := io.iff.excep
  }
  io.ex.excep := excep

  when(flush) {
    pc := 0.U
    inst := Const.NOP_INST
    excep.valid := false.B
  }
  .elsewhen (io.iff.ready) {
    pc := io.iff.pc
    inst := Mux(io.iff.branch.valid, Const.NOP_INST, io.iff.inst)
  }

  when (stall) {
    // flush current instruction
    io.ex.wrRegOp.addr := 0.U             // don't write registers
    io.ex.aluOp.opt := OptCode.ADD    // don't write memory
    io.iff.branch.valid := false.B // don't branch
    io.iff.ready := false.B     // tell IF not to advance
  } 
  .otherwise {
    io.iff.ready := true.B
    switch(instType) {
      is(InstType.R) {
        io.ex.aluOp.rd1 := rs1Val
        io.ex.aluOp.rd2 := rs2Val
        io.ex.wrRegOp.addr := rdAddr
      }
      is(InstType.I) {
        imm := inst(31,20).asSInt
        io.ex.aluOp.rd1 := rs1Val
        io.ex.aluOp.rd2 := imm.asUInt
        io.ex.wrRegOp.addr := rdAddr

        when(decRes(DecTable.OPT) === OptCode.JALR) {
          io.iff.branch.bits := (imm.asUInt + rs1Val) & (~ 1.U(32.W))
          io.iff.branch.valid  := true.B

          io.ex.aluOp.rd1 := pc
          io.ex.aluOp.rd2 := 4.U
          io.ex.aluOp.opt   := OptCode.ADD
        }
      }
      is(InstType.S) {
        imm := Cat(inst(31,25), inst(11,7)).asSInt
        io.ex.aluOp.rd1 := rs1Val
        io.ex.aluOp.rd2 := imm.asUInt
        io.ex.store_data := rs2Val
      }
      is(InstType.B) {
        imm := Cat( inst(31), inst(7), inst(30,25), inst(11,8), 0.U(1.W)).asSInt
        io.iff.branch.bits := pc + imm.asUInt
        val bt = decRes(DecTable.OPT)
        val l = Mux(bt(0), rs1Val.asSInt < rs2Val.asSInt, rs1Val < rs2Val)
        val g = Mux(bt(0), rs1Val.asSInt > rs2Val.asSInt, rs1Val > rs2Val)
        val e = rs1Val === rs2Val
        io.iff.branch.valid := (l & bt(3)) | (e & bt(2)) | (g & bt(1))

        io.ex.aluOp.opt := OptCode.ADD

        d.bt := bt
        d.l := l
      }
      is(InstType.U) {
        imm := (inst & "h_fffff000".U).asSInt
        io.ex.aluOp.rd1 := imm.asUInt
        val ut = decRes(DecTable.OPT)
        io.ex.aluOp.rd2 := Mux(ut(0), pc, 0.U)
        io.ex.aluOp.opt   := OptCode.ADD
        io.ex.wrRegOp.addr := rdAddr
      }
      is(InstType.J) {
        imm := Cat(inst(31), inst(19,12), inst(20), inst(30,21), 0.U(1.W)).asSInt
        io.iff.branch.bits := pc + imm.asUInt
        io.iff.branch.valid  := true.B

        io.ex.aluOp.rd1 := pc
        io.ex.aluOp.rd2 := 4.U
        io.ex.aluOp.opt   := OptCode.ADD //not necessary
        io.ex.wrRegOp.addr := rdAddr
      }
      is(InstType.SYS) {
        val fct3 = inst(14,12)

        when(fct3.orR) {  // CSR inst. Calculate new value here.
          val mode = fct3(1,0)
          val rsVal = Mux(fct3(2), rs1Addr, rs1Val)
          val newVal = MuxLookup(mode, 0.U, Seq(
            (CSRMODE.RW, rsVal),
            (CSRMODE.RS, csrVal | rsVal),
            (CSRMODE.RC, csrVal & ~rsVal)
          ))
          io.ex.wrCSROp.valid := true.B
          io.ex.wrCSROp.addr := csrAddr
          io.ex.wrCSROp.data := newVal
          io.ex.wrRegOp.addr := rdAddr
          io.ex.aluOp.rd1    := csrVal
        }
        .otherwise {
          val inst_p2 = inst(24,20)
          switch(inst_p2) {
            is(SYS_INST_P2.ECALL) {
              when(!excep.valid) {
                io.ex.excep.valid := true.B
                io.ex.excep.code := Cause.ECallU
              }
            }
            is(SYS_INST_P2.EBREAK) {
              //TODO
            }
            is(SYS_INST_P2.xRET) {
              io.ex.xRet.valid := true.B
              io.ex.xRet.bits := inst(29,28)
            }
          }
        }
      }
      is(InstType.BAD) {
        when(!excep.valid) {
          io.ex.excep.valid := true.B
          io.ex.excep.code := Cause.IllegalInstruction
        }
      }
    }
  }

}
