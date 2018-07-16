import chisel3._
import bundles._


class Core extends Module {
  val io = IO(new Bundle {
    val idex = new ID_EX()
    val id_branch = Output(UInt(32.W))
    val ifinst = Output(UInt(32.W))
    val ifpc = Output(UInt(32.W))
    val idpc = Output(UInt(32.W))
    val idimm = Output(SInt(32.W))

    val log = Output(Vec(32, UInt(32.W)))
  })

  val iff  = Module(new IF())
  val id  = Module(new ID())
  val ex  = Module(new EX())
  val mem = Module(new MEM())
  val wb = Module(new WB())
  val reg = Module(new RegFile)
  val mmu = Module(new IMemMMU())

  io.log := reg.io.log

  iff.io.ram   <> mmu.io.iff
  iff.io.id    <> id.io.iff

  id.io.ex   <> ex.io._ID
  id.io.reg  <> reg.io._ID
  id.io.wrRegOp <> ex.io.idWrRegOp
  id.io.exWrRegOp <> ex.io.wrRegOp
  id.io.memWrRegOp <> mem.io.wrRegOp
  id.io.wbWrRegOp <> wb.io.wrRegOp

  io.idex <> id.io.ex
  io.ifinst := iff.io.id.inst
  io.ifpc := iff.io.id.pc
  io.id_branch := id.io.iff.branch_tar
  io.idpc := id.io.log_pc
  io.idimm := id.io.log_imm

  ex.io._MEM  <> mem.io._EX
  ex.io.wrRegOp <> mem.io.exWrRegOp

  mem.io._MMU <> mmu.io._MEM
  mem.io._Reg <> reg.io._MEM
  mem.io.wrRegOp <> wb.io.memWrRegOp
}
