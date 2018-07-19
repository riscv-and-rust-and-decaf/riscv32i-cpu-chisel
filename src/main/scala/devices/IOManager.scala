package devices

import chisel3._

/*
  Interface from Core to IOManager

  # Conflict
  If both read & write for the same address occurs at the same cycle, write first.

  # Address Map
  Compatible with QEMU virt board device tree:
  https://www.sifive.com/blog/2017/12/20/risc-v-qemu-part-1-privileged-isa-hifive1-virtio/

  * [0x10000000, 0x10000008) 8B : UART
  * [0x10001000, 0x10002000) 1K : CGA  (Not std. Compatible with x86 CGA at 0xB8000.)
  * [0x80000000, 0x80400000) 4M : RAM1
  * [0x80400000, 0x80800000) 4M : RAM2
  * [0x80800000, 0x81000000) 8M : Flash

 */
class Core_IO extends Bundle {
  val if_ = new ReadPort
  val mem = new IOPort
  val mmu = new ReadPort
}

/// Generic IO Interface (Direction is for user)
class IOPort extends ReadPort {
  val isWrite = Output(Bool())    // R = W = 1 is invalid
  val wdata = Output(UInt(32.W))
}

class ReadPort extends Bundle {
  val isRead = Output(Bool())
  val addr = Output(UInt(32.W))
  val mask = Output(UInt(4.W))    // 0:Enable, 1:Ignore  e.g: sb=1110, sh=1100, sw=0000
  val rdata = Input(UInt(32.W))
  val ok = Input(Bool())          // Whether the last input operation is finished.
}

/// Do IO with a given physical address for Core
class IOManager extends Module {
  val io = IO(new Bundle {
    val core  = Flipped(new Core_IO)
    val ram1  = new IOPort        // Use addr(31) to indicate RAM1(0) or Serial(1)
    val ram2  = new IOPort
    val flash = new IOPort
  })

  // TODO: Impl
}

