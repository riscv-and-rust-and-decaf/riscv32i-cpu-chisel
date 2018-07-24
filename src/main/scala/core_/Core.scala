package core_

import chisel3._

class CoreState extends Bundle {
  val idex      = new ID_EX_Output()
  val id_branch = Output(UInt(32.W))
  val ifinst    = Output(UInt(32.W))
  val ifpc      = Output(UInt(32.W))
  val reg       = Output(Vec(32, UInt(32.W)))
  val id        = new IDState
}

class Core extends Module {
  val io = IO(new Bundle {
    val dev = new Core_IO()
    val debug = new CoreState
  })

  val iff = Module(new IF())
  val id  = Module(new ID())
  val ex  = Module(new EX())
  val mem = Module(new MEM())
  val reg = Module(new RegFile())
  val mmu = Module(new MMU())
  val csr = Module(new CSR())

  iff.io.mmu   <> mmu.io.iff
  iff.io.id    <> id.io.iff

  id.io.ex         <> ex.io.id
  id.io.reg        <> reg.io.id
  id.io.csr        <> csr.io.id 
  id.io.exWrRegOp  <> ex.io.mem.wrRegOp
  id.io.memWrRegOp <> mem.io.wrRegOp
  id.io.exWrCSROp  <> ex.io.mem.wrCSROp
  id.io.memWrCSROp <> mem.io.wrCSROp

  ex.io.mem     <> mem.io.ex

  mem.io.mmu     <> mmu.io.mem
  mem.io.wrRegOp <> reg.io.mem
  mem.io.wrCSROp <> csr.io.mem

  mmu.io.dev <> io.dev

  // all the fxxking debug things... fxxk chisel
  val d = io.debug
  d.reg       <> reg.io.log
  d.ifinst    <> iff.io.id.inst
  d.ifpc      <> iff.io.id.pc
  d.id_branch <> id.io.iff.branch.bits
  d.idex      <> id.io.ex.asTypeOf(new ID_EX_Output)
  d.id        <> id.d
}
