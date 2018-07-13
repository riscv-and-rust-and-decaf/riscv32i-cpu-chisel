import chisel3._
import bundles._

class RegFile extends Module {
  val io = IO(new Bundle {
    val _ID  = Flipped(new ID_Reg())
    val _MEM = Flipped(new MEM_Reg())

    val log = Output(UInt(32.W))
  })

  val regs = Mem(32, UInt(32.W))

  regs(0.U) := 0.U

  io._ID.read1.data := regs(io._ID.read1.addr)
  io._ID.read2.data := regs(io._ID.read2.addr)

  when (io._MEM.addr.orR) {
    regs(io._MEM.addr) := io._MEM.data;
  }

  io.log := regs(1.U)
}
