package devices

import core_._
import chisel3._
import chisel3.util._

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

/*
 Do IO with a given physical address for Core

 Assume:
 - IF will never write.
 - IF will never read serial.

 Guarantee:
 - Write op from MEM will OK after 1 cycle.

 TODO: Optimize and rewrite
  */
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

  // IF status
  val sReady :: sRetry :: sWait :: Nil = Enum(3)
  val if_status                        = RegInit(sReady)
  val if_lock_addr                     = RegInit(0.U)
  val if_lock_mode                     = RegInit(0.U)
  val if_conflict                      = Wire(Bool())
  if_conflict := false.B  // test later

  // Status => Next status
  switch(if_status) {
    is(sReady) {
      if_status := PriorityMux(Seq(
        (if_.mode === RAMMode.NOP, sReady),
        (if_conflict, sRetry),
        (true.B, sWait)
      ))
      when(if_.mode =/= RAMMode.NOP) {
        if_lock_addr := if_.addr
        if_lock_mode := if_.mode
      }
    }
    is(sRetry) {
      if_status := Mux(if_conflict, sRetry, sWait)
    }
    is(sWait) {
      if_status := Mux(if_.ok, sReady, sWait)
    }
  }

  // IF Status => IF op this cycle
  val if_mode = Mux(if_status === sReady, if_.mode, if_lock_mode)
  val if_addr = Mux(if_status === sReady, if_.addr, if_lock_addr)

  // Route for IF. Test if conflict.
  when(if_mode =/= RAMMode.NOP) {
    when(if_addr.atRAM) {
      when(ramUsed) {
        if_conflict := true.B
      }.otherwise {
        io.ram.mode := if_mode
        io.ram.addr := if_addr
        if_.rdata := io.ram.rdata
        if_.ok := io.ram.ok
      }
    }.elsewhen(if_addr.atFlash) {
      when(flashUsed) {
        if_conflict := true.B
      }.otherwise {
        io.flash.mode := if_mode
        io.flash.addr := if_addr
        if_.rdata := io.flash.rdata
        if_.ok := io.flash.ok
      }
    }
    // Otherwise: NullDev, ok = 1
  }

  when(if_status =/= sWait) {
    if_.ok := false.B
  }
}

