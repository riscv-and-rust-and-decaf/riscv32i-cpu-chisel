package core_

import chisel3._
import chisel3.util._

class CSR extends Module {

  val io = IO(new Bundle {
    val id  = Flipped(new ID_CSR)
    val mem = Flipped(new WrCSROp)

    val memExcep = Flipped(new ExcepStatus)

    val csrExcepEn = Output(Bool())
    val csrExcepPc = Output(UInt(32.W))
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
  }

  // read-only m info
  val mvendorid = 2333.U(32.W)
  val marchid   = "h8fffffff".U(32.W)
  val mimpid    = 2333.U(32.W)
  val mhartid   = 0.U(32.W)

  // read-write m Trap Setup
  val mstatus = RegInit(0.U(32.W))
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

  io.id.rdata := MuxLookup(io.id.addr, 0.U, Seq(
    ADDR.mvendorid -> mvendorid,
    ADDR.marchid -> marchid,
    ADDR.mimpid -> mimpid,
    ADDR.mhartid -> mhartid,
    ADDR.mstatus -> mstatus,
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
    ADDR.mip -> mip
  ))

  when(io.mem.mode =/= 0.U) {
    switch(io.mem.addr) {
      is(ADDR.mstatus) {mstatus := io.mem.newVal}
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
    }
  }

  val pc = Wire(UInt(32.W))
  val excep = RegInit(false.B)

  excep := io.memExcep.en

  when(io.memExcep.en) {
    mepc   := io.memExcep.pc
    mcause := Mux(io.memExcep.code === 8.U,
      io.memExcep.code + prv, 
      io.memExcep.code)
  }

  pc := Mux(mtvec(1,0) === 0.U,
    mtvec(31,2),
    mtvec(31,2) + 4.U * mcause)

  io.csrExcepPc := pc
  io.csrExcepEn := excep

}


