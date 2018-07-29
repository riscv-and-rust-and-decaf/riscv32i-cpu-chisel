package core_

import chisel3._
import chisel3.util._

class CSR extends Module {

  val io = IO(new Bundle {
    val id  = Flipped(new ID_CSR)
    val mem = Flipped(new MEM_CSR)
    val mmu = new CSR_MMU

    val flush = Output(Bool())
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

  io.id.rdata := MuxLookup(io.id.addr, csr(io.id.addr), Seq(
    ADDR.mvendorid -> mvendorid,
    ADDR.marchid -> marchid,
    ADDR.mimpid -> mimpid,
    ADDR.mhartid -> mhartid
  ))

  when(io.mem.wrCSROp.mode =/= CSRMODE.NOP && io.mem.wrCSROp.addr < 0x400.U) {
    csr(io.mem.wrCSROp.addr) := io.mem.wrCSROp.newVal
  }

  //val pc = Wire(UInt(32.W))
  //val excep = Wire(Bool())
  val excep = RegInit(false.B)
  val xRet  = RegInit(false.B)
  val xRet_x = RegInit(Priv.M)

  excep := false.B
  xRet := false.B

  // Alias
  val mstatus = csr(ADDR.mstatus).asTypeOf(new MStatus)
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

  when(io.mem.excep.valid && ( newMode > prv || (newMode === prv && ie))) {
    excep  := true.B

    val cause = Mux(io.mem.excep.code === 8.U,
      io.mem.excep.code + prv,
      io.mem.excep.code)
    val ePc = io.mem.excep.pc + 4.U //TODO: May not +4

    switch(newMode) {
      is(Priv.M) {
        mstatus.MPP := prv
        mepc := ePc
        mcause := cause
      }
      is(Priv.S) {
        mstatus.SPP := (prv === Priv.S)
        sepc := ePc
        scause := cause
      }
      is(Priv.U) {
        uepc := ePc
        ucause := cause
      }
    }
    prv := newMode

  }
  .elsewhen(io.mem.xRet.valid && (io.mem.xRet.bits <= prv)) {
    xRet := true.B
    val x = io.mem.xRet.bits
    xRet_x := x
    // prv<- xPP
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
  }


  io.csrNewPc := 0.U
  io.flush := false.B

  when(excep) {
    io.flush := true.B
    val pcA4 = Cat(mtvec(31,2), 0.U(2.W))
    io.csrNewPc := Mux(mtvec(1,0) === 0.U,
      pcA4,
      pcA4 + 4.U * mcause
      )
  }
  .elsewhen(xRet) {
    printf("xRet")
    io.flush := true.B
    io.csrNewPc := MuxLookup(xRet_x, 0.U , Seq(
      Priv.M -> mepc,
      Priv.S -> sepc,
      Priv.U -> uepc
    ))
  }

  //------------------- MMU ----------------------
  io.mmu.satp := csr(ADDR.satp)
  io.mmu.sum := csr(ADDR.mstatus)(18)
  io.mmu.mxr := csr(ADDR.mstatus)(19)
  io.mmu.flush.valid := false.B // TODO
  io.mmu.flush.bits := 0.U
  io.mmu.priv := prv
}


