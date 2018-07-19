import chisel3._
import bundles._

class RegFile extends Module {
  val io = IO(new Bundle {
    val _ID  = Flipped(new ID_Reg())
    val _MEM = Input(new WrRegOp())

    // fxxk the chisel people, can't they add a tester
    //  that is able to peek internal signals? crap!
    val log = Output(Vec(32, UInt(32.W)))
  })

  val regs = Mem(32, UInt(32.W))

  regs(0.U) := 0.U

  // reads are not clocked
  io._ID.read1.data := regs(io._ID.read1.addr)
  io._ID.read2.data := regs(io._ID.read2.addr)

  val addr = Wire(UInt())
  addr := io._MEM.addr
  val data = Wire(UInt())
  data := io._MEM.data
  when (addr.orR) { // write gate happens here
    regs(addr) := data
  }

  for (i <- 0 until 32)
    io.log(i) := regs(i.U)
}
