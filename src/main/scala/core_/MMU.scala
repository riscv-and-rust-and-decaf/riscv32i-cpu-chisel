package core_
import chisel3._

class MMU extends Module {
  val io = IO(new Bundle {
    val iff = Flipped(new RAMOp())
    val mem = Flipped(new RAMOp())

    val dev = new Core_IO
  })

  class StateIO extends Module {
    val io = IO(new Bundle {
      val from = Flipped(new RAMOp())
      val to = new RAMOp()
    })
    val ready = RegInit(true.B)

    val from = io.from
    val to = io.to
    to.mode := from.mode
    to.wdata := from.wdata
    to.addr := from.addr
    from.rdata := to.rdata
    from.ok := false.B
    when(ready && from.mode =/= RAMMode.NOP) {
      // We have no cache now, so send to IO and wait a cycle
//      from.ok := RAMMode.isWrite(from.mode)
//      ready := RAMMode.isWrite(from.mode)
      ready := false.B
    }.elsewhen(!ready) {
      // Get IO result. Wait until IO ok.
      from.ok := to.ok
      ready := to.ok
    }
  }

  val if_ = Module(new StateIO)
  if_.io.from <> io.iff
  if_.io.to <> io.dev.if_

  val mem = Module(new StateIO)
  mem.io.from <> io.mem
  mem.io.to <> io.dev.mem

  // TODO: Translate address
}
