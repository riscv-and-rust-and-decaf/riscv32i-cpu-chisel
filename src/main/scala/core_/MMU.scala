package core_
import chisel3._

class MMU extends Module {
  val io = IO(new Bundle {
    val iff = Flipped(new RAMOp())
    val mem = Flipped(new RAMOp())

    val dev = new Core_IO
  })

  // TODO: Translate address
  io.dev.if_ <> io.iff
  io.dev.mem <> io.mem
}
