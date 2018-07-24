package devices

import core_._
import chisel3._

object MemoryRegionExt {
  val RAM_BEGIN = 0x00000000L.U(32.W)
  val RAM_END = 0x00400000L.U(32.W)
  val FLASH_BEGIN = 0x00800000L.U(32.W)
  val FLASH_END = 0x01000000L.U(32.W)
  val SERIAL_BEGIN = 0x10000000L.U(32.W)
  val SERIAL_END = 0x10000008L.U(32.W)

  implicit def region(addr: UInt) = new {
    def atRAM = addr >= RAM_BEGIN && addr < RAM_END
    def atFlash = addr >= FLASH_BEGIN && addr < FLASH_END
    def atSerial = addr >= SERIAL_BEGIN && addr < SERIAL_END
  }
}

/// Do IO with a given physical address for Core
class IOManager extends Module {
  val io = IO(new Bundle {
    val core = Flipped(new Core_IO)
    val ram  = new RAMOp
    val flash = new RAMOp
    val serial = new RAMOp
  })

//  printf(p"[IO] IF: ${io.core.if_}\n")
//  printf(p"[IO] MEM: ${io.core.mem}\n")
//  printf(p"[IO] RAM: ${io.ram}\n")
//  printf(p"[IO] Flash: ${io.flash}\n")
//  printf(p"[IO] Serial: ${io.serial}\n")

  // Alias & Import
  private val mem = io.core.mem
  private val if_ = io.core.if_
  import MemoryRegionExt.region

  // Null IO
  val null_device = Module(new NullDev)
  val null_user   = Module(new Module {
    val io = IO(new RAMOp)
    io.addr := 0.U
    io.mode := 0.U
    io.wdata := 0.U
  })
  // Connect to here if the target device is being used
  val wait_device = Module(new Module {
    val io = IO(Flipped(new RAMOp))
    io.ok := false.B
    io.rdata := 0.U
  })
  wait_device.io <> null_user.io

  // Connect to null for all by default
  io.ram <> null_user.io
  io.flash <> null_user.io
  io.serial <> null_user.io
  mem <> null_device.io
  if_ <> null_device.io

  // Route for MEM
  when(mem.mode =/= RAMMode.NOP) {
    when(mem.addr.atRAM) {
      mem <> io.ram
    }.elsewhen(mem.addr.atFlash) {
      mem <> io.flash
    }.elsewhen(mem.addr.atSerial) {
      mem <> io.serial
    }
  }

  private val ramUsed    = mem.mode =/= RAMMode.NOP && mem.addr.atRAM
  private val flashUsed  = mem.mode =/= RAMMode.NOP && mem.addr.atFlash

  // Route for IF
  when(if_.mode =/= RAMMode.NOP) {
    when(if_.addr.atRAM) {
      when(ramUsed) {
        if_ <> wait_device.io
      }.otherwise {
        if_ <> io.ram
      }
    }.elsewhen(if_.addr.atFlash) {
      when(flashUsed) {
        if_ <> wait_device.io
      }.otherwise {
        if_ <> io.flash
      }
    }
  }
}

