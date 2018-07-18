import chisel3._
import chisel3.util._
import bundles._


class CSR extends Module {
  val io = IO(new Bundle {
    val id = Flipped(new _CSR()) 
  })

  object PRV {
    val U = 0.U(2.W)
    val S = 1.U(2.W)
    val M = 3.U(2.W)
  }

  object MODE {
    val RW = 1.U(2.W)
    val RS = 2.U(2.W)
    val RC = 3.U(2.W)
  }

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


  val rsV = Wire(UInt(32.W))
  io.id.rdata := rsV

  val mode = Wire(UInt(2.W)) // 01:CSRRW 10:CSRRS 11:CSRRC 
  mode := io.id.mode
  when(io.id.addr(11,10) === "b11".U) {
    when(io.id.mode === MODE.RW || io.id.wdata =/=0.U) {
      mode := 0.U
      //TODO: cause exception
    }
  }

  rsV := 0.U
  val newV = MuxLookup(mode, 0.U, 
    Seq(
      MODE.RW -> io.id.wdata,
      MODE.RS -> (rsV | io.id.wdata),
      MODE.RC -> (rsV & ~io.id.wdata)
      ))

  when(mode.orR) {
    switch(io.id.addr) {
      is(ADDR.mvendorid) {
        rsV := mvendorid
      }
      is(ADDR.marchid) {
        rsV := marchid
      }
      is(ADDR.mimpid) {
        rsV := mimpid
      }
      is(ADDR.mhartid) {
        rsV := mhartid
      }

      is(ADDR.mstatus) {
        rsV := mstatus
        mstatus := newV
      }
      is(ADDR.misa) {
        rsV := misa
        misa := newV
      }
      is(ADDR.medeleg) {
        rsV := medeleg
        medeleg := newV
      }
      is(ADDR.mideleg) {
        rsV := mideleg
        mideleg := newV
      }
      is(ADDR.mie) {
        rsV := mie
        mie := newV
      }
      is(ADDR.mtvec) {
        rsV := mtvec
        mtvec := newV
      }
      is(ADDR.mcounteren) {
        rsV := mcounteren
        mcounteren := newV
      }
      is(ADDR.mscratch) {
        rsV := mscratch
        mscratch := newV
      }
      is(ADDR.mepc) {
        rsV := mepc
        mepc := newV
      }
      is(ADDR.mcause) {
        rsV := mcause
        mcause := newV
      }
      is(ADDR.mtval) {
        rsV := mtval
        mtval := newV
      }
      is(ADDR.mip) {
        rsV := mip
        mip := newV
      }

    }

  }
}


