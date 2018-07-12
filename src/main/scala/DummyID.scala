import chisel3._
import bundles._

class DummyID extends Module {
  val io = IO(new Bundle {
    val _IF  = Flipped(new IF_ID ())
    val _EX  = new ID_EX()
    val _Reg = new ID_Reg() 
  })

  io._IF.if_branch := false.B
  io._IF.branch_tar := 0.U(32.W)

  io._EX.oprd1 := 7.U(32.W)
  io._EX.oprd2 := 4.U(32.W)
  io._EX.opt   := OptCode.ADD
  io._EX.store_data := 0.U(32.W)
  io._EX.reg_w_add  := 2.U(5.W)

  io._Reg.read1.addr := 1.U(5.W)
  io._Reg.read2.addr := 2.U(5.W)
}
