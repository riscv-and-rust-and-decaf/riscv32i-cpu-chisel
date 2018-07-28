package core_

import chisel3._
import chisel3.util._

class CSR extends Module {

  val io = IO(new Bundle {
    val id  = Flipped(new ID_CSR)
    val mem = Flipped(new MEM_CSR)

    val flush = Output(Bool())
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

  val csrVal = io.mem.wrCSROp.newVal
  when(io.mem.wrCSROp.mode =/= CSRMODE.NOP) {
    switch(io.mem.wrCSROp.addr) {
      is(ADDR.mstatus) {mstatus := csrVal.asTypeOf(new MStatus)}
      is(ADDR.misa) {misa := csrVal}
      is(ADDR.medeleg) {medeleg := csrVal}
      is(ADDR.mideleg) {mideleg := csrVal}
      is(ADDR.mie) {mie := csrVal}
      is(ADDR.mtvec) {mtvec := csrVal}
      is(ADDR.mcounteren) {mcounteren := csrVal}
      is(ADDR.mscratch) {mscratch := csrVal}
      is(ADDR.mepc) {mepc := csrVal}
      is(ADDR.mcause) {mcause := csrVal}
      is(ADDR.mtval) {mtval := csrVal}
      is(ADDR.mip) {mip := csrVal}
      is(ADDR.sepc) {sepc := csrVal}
      is(ADDR.scause) {scause := csrVal}
      is(ADDR.uepc) {uepc := csrVal}
      is(ADDR.ucause) {ucause := csrVal}

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

  when(io.mem.excep.valid && ( newMode > prv || (newMode === prv && ie))) {
    excep  := true.B 
    
    val cause = Mux(io.mem.excep.code === 8.U,
      io.mem.excep.code + prv, 
      io.mem.excep.code)
    val ePc = io.mem.excep.pc + 4.U //TODO: May not +4
    
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
  .elsewhen(io.mem.xRet.valid && (io.mem.xRet.bits <= prv)) {
    xRet := true.B
    val x = io.mem.xRet.bits
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
    printf("xRet");
    io.flush := true.B
    io.csrNewPc := MuxLookup(xRet_x, 0.U , Seq(
      PRV.M -> mepc,
      PRV.S -> sepc,
      PRV.U -> uepc
    ))
  }

}


