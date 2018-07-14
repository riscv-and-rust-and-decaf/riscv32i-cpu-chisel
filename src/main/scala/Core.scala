import chisel3._
import bundles._


class Core extends Module {
  val io = IO(new Bundle {
    val idex = new ID_EX()
    val ifinst = Output(UInt(32.W))
    val ifpc = Output(UInt(32.W))
    val log = Output(UInt(32.W))
  })

  val iff  = Module(new IF())
  val id  = Module(new ID())
  val ex  = Module(new EX())
  val mem = Module(new MEM())
  val reg = Module(new RegFile)
  val mmu = Module(new IMemMMU())

  io.log := reg.io.log

  iff.io.ram   <> mmu.io.iff
  iff.io.id    <> id.io.iff

  id.io.ex   <> ex.io._ID
  id.io.reg  <> reg.io._ID

  io.idex <> id.io.ex
  io.ifinst := iff.io.id.inst
  io.ifpc := iff.io.id.pc

  ex.io._MEM  <> mem.io._EX

  mem.io._MMU <> mmu.io._MEM
  mem.io._Reg <> reg.io._MEM

}
