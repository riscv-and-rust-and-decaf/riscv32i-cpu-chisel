package core_

import chisel3._
import chisel3.util._

class CSR extends Module {

  val io = IO(new Bundle {
    val id  = Flipped(new ID_CSR)
    val mem = Flipped(new MEM_CSR)

    val flush      = Output(Bool())
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

  val csrVal = io.mem.wrCSROp.newVal
  when(io.mem.wrCSROp.mode =/= CSRMODE.NOP) {
    switch(io.mem.wrCSROp.addr) {
      is(ADDR.mstatus) {mstatus := csrVal}
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
    }
  }

  val pc = Wire(UInt(32.W))
  //val excep = Wire(Bool())
  val excep = RegInit(false.B)

  excep := io.mem.excep.valid

  when(io.mem.excep.valid) {
    mepc   := io.mem.excep.pc
    mcause := Mux(io.mem.excep.code === Cause.ECallU,
      io.mem.excep.code + prv,
      io.mem.excep.code)
  }

  val pcA4 = Cat(mtvec(31,2), 0.U(2.W))
  pc := Mux(mtvec(1,0) === 0.U,
    pcA4,
    pcA4 + 4.U * mcause
    )

  io.csrExcepPc := pc
  io.flush := excep

}


