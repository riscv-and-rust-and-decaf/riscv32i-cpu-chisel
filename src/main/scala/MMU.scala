// still a dummy mmu
// TODO: change to real MMU when we add load/store instructions
import chisel3._
import bundles._
import java.nio.file.{Files, Paths}
import scala.collection.mutable.ArrayBuffer

object SrcBinReader {
  var fname = ""

  def read_insts(): Seq[UInt] = {
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

class IMemMMU extends Module {
  val io = IO(new Bundle {
    val iff = Flipped(new IFRAMOp())
    val _MEM  = Flipped(new RAMOp())
  })

  private val imem_dummy = VecInit(SrcBinReader.read_insts())

  io.iff.ifstall := false.B
  io.iff.rdata   := imem_dummy(io.iff.addr(7, 2))

  // discard all data from MEM: dont have load/store instructions yet
  io._MEM.rdata    := 0.U
}
