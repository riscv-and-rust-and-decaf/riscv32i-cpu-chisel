// still a dummy mmu
// TODO: change to real MMU when we add load/store instructions
import chisel3._
import bundles._
import java.nio.file.{Files, Paths}
import scala.collection.mutable.ArrayBuffer

object SrcBinReader {
  var fname = ""

  def read_insts(): Seq[UInt] = {
    if (fname.isEmpty) {
      // this should not happen because it means
      //  either using simulational MMU in verilog generation
      //  or that fname is not set up
      return Seq(Const.NOP_INST, Const.NOP_INST)
    }
    val rv = ArrayBuffer.empty[UInt]
    val bytes = Files.readAllBytes(Paths.get(fname))
    for (i <- 0 until bytes.length-4 by 4) {
      val s = "h_%02x%02x_%02x%02x".format(
        bytes(i+3), bytes(i+2), bytes(i+1), bytes(i))
      rv += s.U(32.W)
    }
    return rv.seq.toIndexedSeq
  }
}


class MMU extends Module {
  val io = IO(new Bundle {
    val iff = Flipped(new RAMOp())
    val mem = Flipped(new RAMOp())
    val ifStall = Output(Bool())

    val ram = new RAMOp()
  })

  when (io.mem.mode === RAMMode.NOP) { // stall
    printf("[MMU] ok IF\n")
    io.ifStall   := false.B
    io.iff.rdata := io.ram.rdata
    io.mem.rdata := 0.U
    io.ram.addr  := io.iff.addr(8,2)
    io.ram.wdata := io.iff.wdata
    io.ram.mode  := io.iff.mode
  } .otherwise { // don't stall
    printf("[MMU] stalling IF; mem: addr=%x, wdata=%x, mode=%x, rdata=%x\n", io.mem.addr, io.mem.wdata, io.mem.mode, io.mem.rdata)
    io.ifStall   := true.B
    io.iff.rdata := Const.NOP_INST
    io.mem.rdata := io.ram.rdata
    io.ram.addr  := io.mem.addr(8,2)
    io.ram.wdata := io.mem.wdata
    io.ram.mode  := io.mem.mode
  }
}

class SimRAM extends Module {
  val io = IO(new Bundle {
    val core = Flipped(new RAMOp())
  })

  private val mem = RegInit(VecInit(SrcBinReader.read_insts()))

  io.core.rdata := 0.U
  when (io.core.mode === RAMMode.NOP) {
    // nop
  } .elsewhen (io.core.mode === RAMMode.LW) {
    printf("[SimRAM] lw:  [%x]->%x\n", io.core.addr, io.core.rdata)
    io.core.rdata := mem(io.core.addr)
  } .elsewhen (io.core.mode === RAMMode.SW) {
    printf("[SimRAM] sw:  [%x]=%x\n", io.core.addr, io.core.wdata)
    mem(io.core.addr) := io.core.wdata
  } .otherwise {
    printf("[SimRAM] bad mode %x!\n", io.core.mode)
  }
}
