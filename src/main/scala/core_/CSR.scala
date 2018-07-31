package core_

import chisel3._
import chisel3.util._

class CSR extends Module {

  val io = IO(new Bundle {
    val id  = Flipped(new ID_CSR)
    val mem = Flipped(new MEM_CSR)
    val mmu = new CSR_MMU

    val flush = Output(Bool())  // Tell modules to clear registers at NEXT cycle
    val csrNewPc = Output(UInt(32.W))
  })

  val prv = RegInit(Priv.M)
  prv := prv

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
    // U
    val uepc      = "h041".U
    val ucause    = "h042".U
  }

  val csr = Mem(0x400, UInt(32.W))

  // Don't use `for(i <- 0 until 0x400)` to iterate all CSRs.
  // It may generate many unused D-triggers, which makes compiling time unacceptable.
  // Just reflect all fields in ADDR here.
  val csr_ids = ADDR.getClass.getDeclaredFields.map(f => {
    f.setAccessible(true)
    f.get(ADDR).asInstanceOf[UInt]
  })

  when(reset.toBool) {
    for(i <- csr_ids) {
      csr(i) := 0.U
    }
  }

  class MStatus extends Bundle {
    val SD = Bool()
    val zero1 = UInt(8.W)
    val TSR = Bool()
    val TW = Bool()
    val TVM = Bool()
    val MXR = Bool()
    val SUM = Bool()
    val MPriv = Bool()
    val XS = UInt(2.W)
    val FS = UInt(2.W)
    val MPP = UInt(2.W)
    val old_HPP = UInt(2.W)
    val SPP = UInt(1.W)
    val MPIE = Bool()
    val old_HPIE = Bool()
    val SPIE = Bool()
    val UPIE = Bool()
    val MIE = Bool()
    val old_HIE = Bool()
    val SIE = Bool()
    val UIE = Bool()
  }


  // read-only m info
  val mvendorid = 2333.U(32.W)
  val marchid   = "h8fffffff".U(32.W)
  val mimpid    = 2333.U(32.W)
  val mhartid   = 0.U(32.W)

  val mstatus = RegInit(0.U.asTypeOf(new MStatus))

  // Read CSR from ID
  io.id.rdata := MuxLookup(io.id.addr, csr(io.id.addr), Seq(
    ADDR.mvendorid -> mvendorid,
    ADDR.marchid -> marchid,
    ADDR.mimpid -> mimpid,
    ADDR.mhartid -> mhartid,
    ADDR.mstatus -> mstatus.asUInt
  ))
  io.id.prv := prv

  // Write CSR from MEM
  when(io.mem.wrCSROp.valid) {
    for(i <- csr_ids) {
      when(i === io.mem.wrCSROp.addr) {
        csr(i) := io.mem.wrCSROp.data
      }
    }
    when(io.mem.wrCSROp.addr === ADDR.mstatus) {
      mstatus := io.mem.wrCSROp.data.asTypeOf(new MStatus)
    }
  }

  // Alias
  val mepc = csr(ADDR.mepc)
  val sepc = csr(ADDR.sepc)
  val uepc = csr(ADDR.uepc)
  val mcause = csr(ADDR.mcause)
  val scause = csr(ADDR.scause)
  val ucause = csr(ADDR.ucause)
  val mtvec = csr(ADDR.mtvec)

  val newMode = Priv.M //TODO: S-mode Trap
  val ie = MuxLookup(prv, false.B, Seq(
    Priv.M  -> mstatus.MIE,
    Priv.S  -> mstatus.SIE,
    Priv.U  -> mstatus.UIE
  ))

  io.flush := io.mem.excep.valid
  io.csrNewPc := 0.U

  // Handle exception from MEM at the same cycle
  when(io.mem.excep.valid) {
    val cause = io.mem.excep.code
    // xRet
    when(Cause.isRet(cause)) {
      val x = Cause.retX(cause)
      // prv <- xPP
      // xIE <- xPIE
      // xPIE <- 1
      // xPP <- U
      prv := MuxLookup(x, 0.U, Seq(
        Priv.M  -> mstatus.MPP,
        Priv.S  -> mstatus.SPP,
        Priv.U  -> Priv.U
      ))
      switch(x) {
        is(Priv.M) {
          mstatus.MIE := mstatus.MPIE
          mstatus.MPIE := 1.U
          mstatus.MPP := Priv.U
        }
        is(Priv.S) {
          mstatus.SIE := mstatus.SPIE
          mstatus.SPIE := 1.U
          mstatus.SPP := 0.U
        }
        is(Priv.U) {
          mstatus.UIE := mstatus.MPIE
          mstatus.UPIE := 1.U
        }
      }
      io.csrNewPc := MuxLookup(Cause.retX(cause), 0.U , Seq(
        Priv.M -> mepc,
        Priv.S -> sepc,
        Priv.U -> uepc
      ))
    }.otherwise { // Exception
      val epc = io.mem.excep.pc // NOTE: no +4, do by trap handler if necessary
      switch(newMode) {
        is(Priv.M) {
          mstatus.MPP := prv
          mepc := epc
          mcause := cause
        }
        is(Priv.S) {
          mstatus.SPP := (prv === Priv.S)
          sepc := epc
          scause := cause
        }
        is(Priv.U) {
          uepc := epc
          ucause := cause
        }
      }
      csr(ADDR.mtval) := io.mem.excep.value
      prv := newMode
      val pcA4 = Cat(mtvec(31,2), 0.U(2.W))
      io.csrNewPc := Mux(mtvec(1,0) === 0.U,
        pcA4,
        pcA4 + 4.U * mcause
      )
    }
  }

  //------------------- MMU ----------------------
  io.mmu.satp := csr(ADDR.satp)
  io.mmu.sum := mstatus.SUM
  io.mmu.mxr := mstatus.MXR
  io.mmu.flush.valid := false.B // TODO
  io.mmu.flush.bits := 0.U
  io.mmu.priv := prv
}


