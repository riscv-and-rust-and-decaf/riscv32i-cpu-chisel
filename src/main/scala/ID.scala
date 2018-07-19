import chisel3._
import bundles._
import chisel3.util._

class ID extends Module {
  val io = IO(new Bundle {
    val iff = Flipped(new IF_ID())  // naming conflict if use `if`
    val reg = new ID_Reg()
    val ex = new ID_EX()

    val wrRegOp = new WrRegOp

    // forwarding
    val exWrRegOp = Flipped(new WrRegOp)
    val memWrRegOp = Flipped(new WrRegOp)
    val wbWrRegOp = Flipped(new WrRegOp)

    //output log
    val log_bt = Output(UInt(5.W))
    val log_l = Output(Bool())
    val log_type = Output(UInt(3.W))
    val log_opt = Output(UInt(5.W))
    val log_pc = Output(UInt(5.W))
    val log_imm = Output(SInt(32.W))
  })
  
  val inst = RegInit(Const.NOP_INST)
  inst := Mux(io.iff.if_branch, Const.NOP_INST, io.iff.inst)
  val pc = RegInit(0.U(32.W))
  pc := io.iff.pc

  io.log_pc := pc

  val decRes = ListLookup(inst, DecTable.defaultDec, DecTable.decMap)
  val it = decRes(DecTable.TYPE)
  
  val rs1Addr  = inst(19, 15)
  val rs2Addr  = inst(24, 20)
  val rdAddr   = inst(11, 7)

  val imm = Wire(SInt(32.W))
 
  io.log_imm := imm

  //Null Init
  io.iff.if_branch  := false.B
  io.iff.branch_tar := 0.U

  io.ex.oprd1 := 0.U
  io.ex.oprd2 := 0.U
  io.ex.opt := decRes(DecTable.OPT)
  io.ex.store_data := 0.U
  val w_reg_addr = Wire(UInt(5.W))
  w_reg_addr := 0.U

  imm := 0.S
  
  //Get regVar and  forwarding
  
  io.reg.read1.addr := rs1Addr
  io.reg.read2.addr := rs2Addr
  
  val exWrRegOp = Wire(new WrRegOp())
  exWrRegOp := io.exWrRegOp
  val memWrRegOp = Wire(new WrRegOp())
  memWrRegOp := io.memWrRegOp
  val wbWrRegOp = Wire(new WrRegOp())
  wbWrRegOp := io.wbWrRegOp
  // TODO: check rdy
  val rs1Val = Mux(rs1Addr.orR,
    Mux(exWrRegOp.addr === rs1Addr,
      exWrRegOp.data,
      Mux(memWrRegOp.addr === rs1Addr,
        memWrRegOp.data,
        Mux(wbWrRegOp.addr === rs1Addr,
          wbWrRegOp.data,
          io.reg.read1.data))),
    0.U)
  val rs2Val = Mux(rs2Addr.orR,
    Mux(exWrRegOp.addr === rs2Addr,
      exWrRegOp.data,
      Mux(memWrRegOp.addr === rs2Addr,
        memWrRegOp.data,
        Mux(wbWrRegOp.addr === rs2Addr,
          wbWrRegOp.data,
          io.reg.read2.data))),
    0.U)
 
  io.log_bt := 0.U
  io.log_l := false.B
  // deal with different kind inst

  io.log_type := it
  io.log_opt := decRes(DecTable.OPT)

  switch(it) {
    is(InstType.R) {
      io.ex.oprd1 := rs1Val
      io.ex.oprd2 := rs2Val
      w_reg_addr := rdAddr
    }
    is(InstType.I) {
      imm := inst(31,20).asSInt
      io.ex.oprd1 := rs1Val
      io.ex.oprd2 := imm.asUInt
      w_reg_addr := rdAddr

      when(decRes(DecTable.OPT) === OptCode.JALR) {
        io.iff.branch_tar := (imm.asUInt + rs1Val ) & (~ 1.U(32.W))
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

      io.log_bt := bt
      io.log_l := l
    }
    is(InstType.U) {
      imm := (inst & "h_fffff000".U).asSInt
      io.ex.oprd1 := imm.asUInt;
      val ut = decRes(DecTable.OPT)
      io.ex.oprd2 := Mux(ut(0), pc, 0.U)
      io.ex.opt   := OptCode.ADD
      w_reg_addr := rdAddr
    }
    is(InstType.J) {
      imm := Cat(inst(31), inst(19,12), inst(20), inst(30,21), 0.U(1.W)).asSInt
      io.iff.branch_tar := pc + imm.asUInt
      io.iff.if_branch  := true.B

      io.ex.oprd1 := pc
      io.ex.oprd2 := 4.U
      //io.ex.opt   := OptCode.ADD //not necessary
      w_reg_addr := rdAddr 
    }
    is(InstType.BAD) {
      //TODO
    }
  }
  
  io.wrRegOp.addr := w_reg_addr
  io.wrRegOp.data := 0.U
  io.wrRegOp.rdy  := false.B
} 
