package devices

import chisel3._
import core_._

class NullDev() extends Module {
  val io = IO(Flipped(new RAMOp))

  io.ok := false.B
  io.rdata := 0.U
}
