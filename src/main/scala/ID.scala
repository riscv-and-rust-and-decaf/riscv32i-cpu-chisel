import chisel3._
import bundles._
import chisel3.util._

class ID extends Module {
  val io = IO(new Bundle {
    val iff = Flipped(new IF_ID())  // naming conflict if use `if`
    val reg = new ID_Reg()
    val ex = new ID_EX()

    val im_log = Output(SInt(32.W))
  })

  val inst = io.iff.inst
  val decRes = ListLookup(inst, DecTable.defaultDec, DecTable.decMap)
  val It = decRes(DecTable.TYPE)
  
  val rs1Addr  = inst(19, 15)
  val rs2Addr  = inst(24, 20)
  val rdAddr   = inst(11, 7)
  
  val imm = Wire(SInt(32.W))
  io.im_log := imm
  
  //Null Init
  io.iff.if_branch  := false.B
  io.iff.branch_tar := 0.U

  io.reg.read1.addr := rs1Addr
  val rs1Val = io.reg.read1.data
  io.reg.read2.addr := rs2Addr
  val rs2Val = io.reg.read2.data

  io.ex.oprd1 := 0.U
  io.ex.oprd2 := 0.U
  io.ex.opt := decRes(DecTable.OPT)
  io.ex.store_data := 0.U
  io.ex.reg_w_add := 0.U

  imm := 0.S
  // deal with different kind inst


  switch(It) {
    is(InstType.R) {
      io.ex.oprd1 := rs1Val
      io.ex.oprd2 := rs2Val
      io.ex.reg_w_add := rdAddr
    }
    is(InstType.I) {
      imm := inst(31,20).asSInt
      io.ex.oprd1 := rs1Val
      io.ex.oprd2 := imm.asUInt
      io.ex.reg_w_add := rdAddr
    }
    is(InstType.S) {
      imm := Cat(inst(31,25), inst(11,7)).asSInt
      io.ex.oprd1 := rs1Val
      io.ex.oprd2 := imm.asUInt
      io.ex.store_data := rs2Val
    }
    is(InstType.B) {
      imm := Cat( inst(31), inst(7), inst(30,25), inst(11,8), 0.U).asSInt
      io.iff.branch_tar := io.iff.pc + imm.asUInt
      val bt = decRes(DecTable.OPT)
      val l = Mux(bt(0), rs1Val.asSInt < rs2Val.asSInt, rs1Val < rs2Val)
      val g = Mux(bt(0), rs1Val.asSInt > rs2Val.asSInt, rs1Val > rs2Val)
      val e = (rs1Val === rs2Val)
      io.iff.if_branch := (l & bt(3)) | (e & bt(2)) | (g & bt(1))

      //io.ex.opt := OptCode.ADD //not necessary but better for understand 
    }
    is(InstType.U) {

    }
    is(InstType.BAD) {
      //TODO
    }
  }
/*  
  // parse instruction
  val rs1Addr  = io.iff.inst(19, 15)
  val rs2Addr  = io.iff.inst(24, 20)
  val rdAddr   = io.iff.inst(11, 7)

  // read immediate + sign/zero extend
  val iImm = Wire(SInt(32.W))
  iImm := io.iff.inst(31, 20).asSInt

  // read registers
  io.reg.read1.addr := rs1Addr
  io.reg.read2.addr := rs2Addr
  val rs1val = io.reg.read1.data
  val rs2val = io.reg.read2.data

  // decode control signals
  val decRes = ListLookup(io.iff.inst, DecTable.defaultDec, DecTable.decMap)

  val InstType = decRes(DecTable.TYPE)
 
  when( InstType === InstType.R) {
  }
  .otherwise {
  }

  val oprd1 = MuxLookup(decRes(DecTable.NUM1_SEL), 0.U(32.W), Seq(
      Num1Sel.NUM1_RS1 -> rs1val
  ))
  val oprd2 = MuxLookup(decRes(DecTable.NUM2_SEL), 0.U(32.W), Seq(
      Num2Sel.NUM2_RS2 -> rs2val,
      Num2Sel.NUM2_I_IMM -> iImm.asUInt
  ))

  // branch signals wired back to IF.
  io.iff.if_branch := decRes(DecTable.BR)
  io.iff.branch_tar := 0.U // TODO

  // pass to ex
  io.ex.oprd1 := oprd1
  io.ex.oprd2 := oprd2
  io.ex.opt := decRes(DecTable.OPT)
  io.ex.reg_w_add := Mux(decRes(DecTable.WREG).toBool, rdAddr, 0.U)
  io.ex.store_data := 0.U // TODO

  // TODO: deal with bad instructions (illegal), raise exception.
*/
}
