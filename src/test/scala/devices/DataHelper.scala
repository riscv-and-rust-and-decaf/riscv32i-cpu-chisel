package devices

import java.nio.file.{Files, Paths}

import chisel3._
import core_._

import scala.collection.mutable.ArrayBuffer

object DataHelper {

  def read_insts(fname: String): Seq[UInt] = {
    if (fname.isEmpty) {
      // this should not happen because it means
      //  either using simulational MMU in verilog generation
      //  or that fname is not set up
      return Seq(Const.NOP_INST, Const.NOP_INST)
    }
    val rv = ArrayBuffer.empty[UInt]
    val bytes = Files.readAllBytes(Paths.get(fname))
    for (i <- 0 until bytes.length)
      rv += "h_%02x".format(bytes(i)).U(8.W)
    return rv.seq.toIndexedSeq
  }
}