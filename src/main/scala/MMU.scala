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
      rv += s.U
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

  io.ifStall := false.B
  io.iff.rdata := io.ram.rdata
  io.mem.rdata := 0.U

  io.ram.addr := io.iff.addr(7,2)
  io.ram.wdata := io.iff.wdata
  io.ram.mode := io.iff.mode
}


class SimRAM extends Module {
  val io = IO(new Bundle {
    val core = Flipped(new RAMOp())
  })

  private val mem = VecInit(SrcBinReader.read_insts())

  io.core.rdata := 0.U
  when (io.core.mode === Const.MMU_MODE_NOP) {
    // nop
  } .elsewhen (io.core.mode === Const.MMU_MODE_LW) {
    io.core.rdata := mem(io.core.addr)
  } .otherwise {
    printf("[SimRAM] bad mode %x!\n", io.core.mode)
  }
}
