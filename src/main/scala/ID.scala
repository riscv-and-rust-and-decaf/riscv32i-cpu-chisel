import chisel3._
import bundles._
import chisel3.util._

class ID extends Module {
  val io = IO(new Bundle {
    val iff = Flipped(new IF_ID())  // naming conflict if use `if`
    val reg = new ID_Reg()
    val ex = new ID_EX()
  })

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
}
