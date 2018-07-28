package core_

import chisel3._
import chisel3.util._

class CSR extends Module {

  val io = IO(new Bundle {
    val id  = Flipped(new ID_CSR)
    val mem = Flipped(new MEM_CSR)
    val mmu = new CSR_MMU

    val flush      = Output(Bool())
    val csrExcepPc = Output(UInt(32.W))
  })

  val priv = RegInit(Priv.M)

  object ADDR {
    // M Info
    val mvendorid = "hF11".U
    val marchid   = "hF12".U
    val mimpid    = "hF13".U
    val mhartid   = "hF14".U
    //M Trap Setup
    val mstatus   = "h300".U
    val misa      = "h301".U
    val medeleg   = "h302".U
    val mideleg   = "h303".U
    val mie       = "h304".U
    val mtvec     = "h305".U
    val mcounteren= "h306".U
    //M Trap Hangding
    val mscratch  = "h340".U
    val mepc      = "h341".U
    val mcause    = "h342".U
    val mtval     = "h343".U
    val mip       = "h344".U
    //S Trap Setup
    val sstatus   = "h100".U
    val sedeleg   = "h102".U
    val sideleg   = "h103".U
    val sie       = "h104".U
    val stvec     = "h105".U
    val scounteren= "h106".U
    //S Trap Hangding
    val sscratch  = "h140".U
    val sepc      = "h141".U
    val scause    = "h142".U
    val stval     = "h143".U
    val sip       = "h144".U
    // S Protection and Translation
    val satp      = "h180".U
  }

  val csr = Mem(0x400, UInt(32.W))

  when(reset.toBool) {
    // Don't use `for(i <- 0 until 0x400)`.
    // It will generate many unused D-triggers, which slow down the compiling.
    //
    // Just reflect all fields in ADDR
    for(i <- ADDR.getClass.getDeclaredFields.map(f => {
      f.setAccessible(true)
      f.get(ADDR).asInstanceOf[UInt]
    })) {
      csr(i) := 0.U
    }
  }

  // read-only m info
  val mvendorid = 2333.U(32.W)
  val marchid   = "h8fffffff".U(32.W)
  val mimpid    = 2333.U(32.W)
  val mhartid   = 0.U(32.W)

  io.id.rdata := MuxLookup(io.id.addr, csr(io.id.addr), Seq(
    ADDR.mvendorid -> mvendorid,
    ADDR.marchid -> marchid,
    ADDR.mimpid -> mimpid,
    ADDR.mhartid -> mhartid
  ))

  when(io.mem.wrCSROp.mode =/= CSRMODE.NOP && io.mem.wrCSROp.addr < 0x400.U) {
    csr(io.mem.wrCSROp.addr) := io.mem.wrCSROp.newVal
  }

  val pc = Wire(UInt(32.W))
  //val excep = Wire(Bool())
  val excep = RegInit(false.B)

  excep := io.mem.excep.valid

  when(io.mem.excep.valid) {
    csr(ADDR.mepc)   := io.mem.excep.pc
    csr(ADDR.mcause) := Mux(io.mem.excep.code === Cause.ECallU,
      io.mem.excep.code + priv,
      io.mem.excep.code)
  }

  val mtvec = csr(ADDR.mtvec)
  val pcA4 = Cat(mtvec(31,2), 0.U(2.W))
  pc := Mux(mtvec(1,0) === 0.U,
    pcA4,
    pcA4 + 4.U * csr(ADDR.mcause)
  )

  io.csrExcepPc := pc
  io.flush := excep

  //------------------- MMU ----------------------
  io.mmu.satp := csr(ADDR.satp)
  io.mmu.sum := csr(ADDR.mstatus)(18)
  io.mmu.mxr := csr(ADDR.mstatus)(19)
  io.mmu.flush.valid := false.B // TODO
  io.mmu.flush.bits := 0.U
  io.mmu.priv := priv
}


