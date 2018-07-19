package devices

import core_._
import chisel3._

/// Do IO with a given physical address for Core
class IOManager extends Module {
  val io = IO(new Bundle {
    val core = Flipped(new Core_IO)
    val ram  = new RAMOp // Use addr(31) to indicate RAM1(0) or Serial(1)
//    val flash = new RAMOp
  })

  when(io.core.mem.mode === RAMMode.NOP) {
    printf("[IO] IF: ${io.core.if_}\n")
    io.core.if_.ok := true.B
    io.core.if_.rdata := io.ram.rdata
    io.core.mem.ok := false.B
    io.core.mem.rdata := 0.U
    io.ram.addr := io.core.if_.addr
    io.ram.wdata := io.core.if_.wdata
    io.ram.mode := io.core.if_.mode
  }.otherwise {
    printf(p"[IO] stalling IF; MEM: ${io.core.mem}\n")
    io.core.if_.ok := false.B
    io.core.if_.rdata := Const.NOP_INST
    io.core.mem.ok := true.B
    io.core.mem.rdata := io.ram.rdata
    io.ram.addr := io.core.mem.addr
    io.ram.wdata := io.core.mem.wdata
    io.ram.mode := io.core.mem.mode
  }
  // TODO: Impl
}

