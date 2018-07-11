import chisel3._
import bundles._

/*
class RegFileIO extends Bundle {

  val raddr1 = Input(UInt(5.W))
  val raddr2 = Input(UInt(5.W))
  val rdata1 = Output(UInt(32.W))
  val rdata2 = Output(UInt(32.W))
  val waddr  = Input(UInt(5.W))   // set to 0 if no write is needed
  val wdata  = Input(UInt(32.W))
}
*/

class RegFile extends Module {
  val io = IO(new Bundle {
    val _ID  = Flipped(new ID_Reg())
    val _MEM = Flipped(new MEM_Reg())
  })

  val regs = Mem(32, UInt(32.W))

  regs(0.U) := 0.U

  io._ID.read1.data := regs(io._ID.read1.addr)
  io._ID.read2.data := regs(io._ID.read2.addr)

  when (io._MEM.addr.orR) {
    regs(io._MEM.addr) := io._MEM.data;
  }
}
