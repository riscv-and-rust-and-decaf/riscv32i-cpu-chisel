import chisel3._
import bundles._


class IDTestModule extends Module {
  val io = IO(new Bundle {
    val iff = Flipped(new IF_ID())  // tester acts as ID
    val ex = new ID_EX()
    val regw = Flipped(new MEM_Reg())
  })

  val reg = Module(new RegFile())
  reg.io._MEM.addr := io.regw.addr
  reg.io._MEM.data := io.regw.data

  val id = Module(new ID())
  id.io.iff.pc := io.iff.pc
  id.io.iff.inst := io.iff.inst
  io.iff.if_branch := id.io.iff.if_branch
  io.iff.branch_tar := id.io.iff.branch_tar
  id.io.reg.read1 <> reg.io._ID.read1
  id.io.reg.read2 <> reg.io._ID.read2
  io.ex.oprd1 := id.io.ex.oprd1
  io.ex.oprd2 := id.io.ex.oprd2
  io.ex.opt := id.io.ex.opt
  io.ex.reg_w_add := id.io.ex.reg_w_add
}
