package core_

import chisel3._
import chisel3.util._

class CSR extends Module {

  val io = IO(new Bundle {
    val id  = Flipped(new ID_CSR)
    val mem = Flipped(new WrCSROp)

    val memExcep = Flipped(new ExcepStatus)
    val memXRet  = Input(Valid(UInt(2.W)))

    val csrFlush = Output(Bool())
    val csrNewPc = Output(UInt(32.W))
  })

  object PRV {
    val U = 0.U(2.W)
    val S = 1.U(2.W)
    val M = 3.U(2.W)
  }

  val prv = RegInit(PRV.M)

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
    // S
    val sepc      = "h141".U
    val scause    = "h142".U
    // U
    val uepc      = "h041".U
    val ucause    = "h042".U
  }

  class MStatus extends Bundle {
    val SD = Bool()
    val zero1 = UInt(8.W)
    val TSR = Bool()
    val TW = Bool()
    val TVM = Bool()
    val MXR = Bool()
    val SUM = Bool()
    val MPRV = Bool()
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

  // read-write m Trap Setup
  val mstatus = RegInit(0.U(32.W).asTypeOf(new MStatus))
  val misa = RegInit(0.U(32.W))
  val medeleg = RegInit(0.U(32.W))
  val mideleg = RegInit(0.U(32.W))
  val mie = RegInit(0.U(32.W))
  val mtvec = RegInit(0.U(32.W))
  val mcounteren = RegInit(0.U(32.W))

  // read-write m Trap Handling
  val mscratch = RegInit(0.U(32.W))
  val mepc = RegInit(0.U(32.W))
  val mcause = RegInit(0.U(32.W))
  val mtval = RegInit(0.U(32.W))
  val mip = RegInit(0.U(32.W))

  // s
  val sepc = RegInit(0.U(32.W))
  val scause = RegInit(0.U(32.W))
  // u
  val uepc = RegInit(0.U(32.W))
  val ucause = RegInit(0.U(32.W))


  io.id.rdata := MuxLookup(io.id.addr, 0.U, Seq(
    ADDR.mvendorid -> mvendorid,
    ADDR.marchid -> marchid,
    ADDR.mimpid -> mimpid,
    ADDR.mhartid -> mhartid,
    ADDR.mstatus -> mstatus.asUInt(),
    ADDR.misa -> misa,
    ADDR.medeleg -> medeleg,
    ADDR.mideleg -> mideleg,
    ADDR.mie -> mie,
    ADDR.mtvec -> mtvec,
    ADDR.mcounteren -> mcounteren,
    ADDR.mscratch -> mscratch,
    ADDR.mepc -> mepc,
    ADDR.mcause -> mcause,
    ADDR.mtval -> mtval,
    ADDR.mip -> mip,
    ADDR.sepc -> sepc,
    ADDR.scause -> scause,
    ADDR.uepc -> uepc,
    ADDR.ucause -> ucause
  ))

  when(io.mem.mode =/= CSRMODE.NOP) {
    switch(io.mem.addr) {
      is(ADDR.mstatus) {mstatus := io.mem.newVal.asTypeOf(new MStatus)}
      is(ADDR.misa) {misa := io.mem.newVal}
      is(ADDR.medeleg) {medeleg := io.mem.newVal}
      is(ADDR.mideleg) {mideleg := io.mem.newVal}
      is(ADDR.mie) {mie := io.mem.newVal}
      is(ADDR.mtvec) {mtvec := io.mem.newVal}
      is(ADDR.mcounteren) {mcounteren := io.mem.newVal}
      is(ADDR.mscratch) {mscratch := io.mem.newVal}
      is(ADDR.mepc) {mepc := io.mem.newVal}
      is(ADDR.mcause) {mcause := io.mem.newVal}
      is(ADDR.mtval) {mtval := io.mem.newVal}
      is(ADDR.mip) {mip := io.mem.newVal}
      is(ADDR.sepc) {sepc := io.mem.newVal}
      is(ADDR.scause) {scause := io.mem.newVal}
      is(ADDR.uepc) {uepc := io.mem.newVal}
      is(ADDR.ucause) {ucause := io.mem.newVal}

    }
  }

  //val pc = Wire(UInt(32.W))
  //val excep = Wire(Bool())
  val excep = RegInit(false.B)
  val xRet  = RegInit(false.B)
  val xRet_x = RegInit(PRV.M)

  excep := false.B
  xRet := false.B

  val newMode = PRV.M //TODO: S-mode Trap
  val ie = MuxLookup(prv, false.B, Seq(
    PRV.M  -> mstatus.MIE,
    PRV.S  -> mstatus.SIE,
    PRV.U  -> mstatus.UIE
    ))

  when(io.memExcep.en && ( newMode > prv || (newMode === prv && ie))) {
    excep  := true.B 
    
    val cause = Mux(io.memExcep.code === 8.U,
      io.memExcep.code + prv, 
      io.memExcep.code)
    val ePc = io.memExcep.pc + 4.U //TODO: May not +4
    
    switch(newMode) {
      is(PRV.M) {
        mstatus.MPP := prv 
        mepc := ePc
        mcause := cause
      }
      is(PRV.S) {
        mstatus.SPP := (prv === PRV.S)
        sepc := ePc
        scause := cause
      }
      is(PRV.U) {
        uepc := ePc
        ucause := cause
      }
    }
    prv := newMode 
    
  }
  .elsewhen(io.memXRet.valid && (io.memXRet.bits <= prv)) {
    xRet := true.B
    val x = io.memXRet.bits
    xRet_x := x
    // prv<- xPP
    // xIE <- xPIE
    // xPIE <- 1
    // xPP <- U
    prv := MuxLookup(x, 0.U, Seq(
      PRV.M  -> mstatus.MPP,
      PRV.S  -> mstatus.SPP,
      PRV.U  -> PRV.U
      ))
    switch(x) {
      is(PRV.M) {
        mstatus.MIE := mstatus.MPIE
        mstatus.MPIE := 1.U
        mstatus.MPP := PRV.U
      }
      is(PRV.S) {
        mstatus.SIE := mstatus.SPIE
        mstatus.SPIE := 1.U
        mstatus.SPP := 0.U
      }
      is(PRV.U) {
        mstatus.UIE := mstatus.MPIE
        mstatus.UPIE := 1.U
      }
    } 
  }


  io.csrNewPc := 0.U
  io.csrFlush := false.B

  when(excep) {
    io.csrFlush := true.B
    val pcA4 = Cat(mtvec(31,2), 0.U(2.W))
    io.csrNewPc := Mux(mtvec(1,0) === 0.U,
      pcA4,
      pcA4 + 4.U * mcause
      )
  }
  .elsewhen(xRet) {
    printf("xRet");
    io.csrFlush := true.B
    io.csrNewPc := MuxLookup(xRet_x, 0.U , Seq(
      PRV.M -> mepc,
      PRV.S -> sepc,
      PRV.U -> uepc
    ))
  }

}


