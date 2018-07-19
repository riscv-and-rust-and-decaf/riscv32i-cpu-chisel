import chisel3._
import bundles._

class RegFile extends Module {
  val io = IO(new Bundle {
    val id  = Flipped(new ID_Reg())
    val mem = Flipped(new WrRegOp)

    // debug stuff below
    val log = Output(Vec(32, UInt(32.W)))
  })

  val regs = Mem(32, UInt(32.W))
  regs(0.U) := 0.U

  // reads are not clocked
  val raddr1 = io.id.read1.addr
  val raddr2 = io.id.read2.addr
  io.id.read1.data := regs(raddr1)
  io.id.read2.data := regs(raddr2)

  // write happens on ff
  val addr = io.mem.addr
  val data = io.mem.data
  when (addr.orR) { // write gate happens here
    regs(addr) := data
  }

  // debug stuff below
  for (i <- 0 until 32)
    io.log(i) := regs(i.U)
}
