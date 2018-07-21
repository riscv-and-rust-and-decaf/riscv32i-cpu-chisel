package devices

import java.nio.ByteBuffer
import java.nio.file.{Files, Paths}

object DataHelper {

  def read_insts(fname: String): Seq[Long] = {
    if (fname.isEmpty) {
      // this should not happen because it means
      //  either using simulational MMU in verilog generation
      //  or that fname is not set up
      val NOP = 0x13
      return Seq(NOP, NOP)
    }
    Files.readAllBytes(Paths.get(fname))
      .grouped(4).map(b => ByteBuffer.wrap(Array.fill(4)(0.toByte) ++ b.reverse).getLong)
      .toSeq
  }
}