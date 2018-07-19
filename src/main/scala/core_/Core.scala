import chisel3._
import bundles._

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
    val ram = new RAMOp()
    val debug = new CoreState
  })

  val iff = Module(new IF())
  val id  = Module(new ID())
  val ex  = Module(new EX())
  val mem = Module(new MEM())
  val reg = Module(new RegFile())
  val mmu = Module(new MMU())

  iff.io.ram   <> mmu.io.iff
  iff.io.id    <> id.io.iff
  iff.io.stall <> mmu.io.ifStall

  id.io.ex         <> ex.io.id
  id.io.reg        <> reg.io.id
  id.io.wrRegOp    <> ex.io.idWrRegOp
  id.io.exWrRegOp  <> ex.io.wrRegOp
  id.io.memWrRegOp <> mem.io.wrRegOp

  ex.io.mem     <> mem.io.ex
  ex.io.wrRegOp <> mem.io.exWrRegOp

  mem.io.mmu     <> mmu.io.mem
  mem.io.wrRegOp <> reg.io.mem

  mmu.io.ram <> io.ram
  printf("[Core] ram: addr=%x, wdata=%x, rdata=%x, mode=%d\n", io.ram.addr, io.ram.wdata, io.ram.rdata, io.ram.mode)

  // all the fxxking debug things... fxxk chisel
  val d = io.debug
  d.reg       <> reg.io.log
  d.ifinst    <> iff.io.id.inst
  d.ifpc      <> iff.io.id.pc
  d.id_branch <> id.io.iff.branch_tar
  d.idex      <> id.io.ex
  d.id        <> id.d
}
