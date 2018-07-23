package core_

import chisel3._

class CoreState extends Bundle {
  val idex      = new ID_EX()
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

  iff.io.ram   <> mmu.io.iff
  iff.io.id    <> id.io.iff
  iff.io.excep <> id.io.ifExcep

  id.io.ex         <> ex.io.id
  id.io.reg        <> reg.io.id
  id.io.csr        <> csr.io.id 
  id.io.wrRegOp    <> ex.io.idWrRegOp
  id.io.exWrRegOp  <> ex.io.wrRegOp
  id.io.memWrRegOp <> mem.io.wrRegOp
  id.io.exWrCSROp  <> ex.io.wrCSROp
  id.io.memWrCSROp <> mem.io.wrCSROp
  id.io.wrCSROp    <> ex.io.idWrCSROp
  id.io.excep      <> ex.io.idExcep

  ex.io.mem     <> mem.io.ex
  ex.io.wrRegOp <> mem.io.exWrRegOp
  ex.io.wrCSROp <> mem.io.exWrCSROp
  ex.io.excep   <> mem.io.exExcep

  mem.io.mmu     <> mmu.io.mem
  mem.io.wrRegOp <> reg.io.mem
  mem.io.wrCSROp <> csr.io.mem
  mem.io.excep   <> csr.io.memExcep

  mmu.io.dev <> io.dev

  //flush of exceptions
  iff.io.csrExcepEn := csr.io.csrExcepEn
  iff.io.csrExcepPc := csr.io.csrExcepPc
  id.io.csrExcepEn := csr.io.csrExcepEn
  ex.io.csrExcepEn := csr.io.csrExcepEn
  mem.io.csrExcepEn := csr.io.csrExcepEn


  // all the fxxking debug things... fxxk chisel
  val d = io.debug
  d.reg       <> reg.io.log
  d.ifinst    <> iff.io.id.inst
  d.ifpc      <> iff.io.id.pc
  d.id_branch <> id.io.iff.branch_tar
  d.idex      <> id.io.ex
  d.id        <> id.d
}
