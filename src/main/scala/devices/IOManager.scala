package devices

import core_._
import chisel3._
import chisel3.core.stop
import chisel3.util._

object MemoryRegionExt {
  val RAM_BEGIN    = 0x80000000L.U(32.W)
  val RAM_END      = 0x80800000L.U(32.W)
  val FLASH_BEGIN  = 0x00800000L.U(32.W)
  val FLASH_END    = 0x01000000L.U(32.W)
  val SERIAL_BEGIN = 0x10000000L.U(32.W)
  val SERIAL_END   = 0x10000008L.U(32.W)

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

  val waitNone :: waitRAM :: waitFlash :: waitSerial :: Nil = Enum(4)
  val ifWait  = RegInit(waitNone)
  val memWait = RegInit(waitNone)

  // Handle output
  def bindOutput(user: RAMOp, device: RAMOp): Unit = {
    user.rdata := device.rdata
    user.ok := device.ok
  }
  def bindInput(user: RAMOp, device: RAMOp): Unit = {
    device.mode := user.mode
    device.addr := user.addr
    device.wdata := user.wdata
  }
  def handleOutput(status: UInt, user: RAMOp): Unit = {
    switch(status) {
      is(waitRAM)     { bindOutput(user, io.ram) }
      is(waitFlash)   { bindOutput(user, io.flash) }
      is(waitSerial)  { bindOutput(user, io.serial) }
    }
  }
  handleOutput(ifWait, if_)
  handleOutput(memWait, mem)

  // Status after output
  val flashFree = ifWait =/= waitFlash && memWait =/= waitFlash
  ifWait := Mux(ifWait =/= waitNone && !if_.ok, ifWait, waitNone)
  memWait := Mux(memWait =/= waitNone && !mem.ok, memWait, waitNone)

  // Handle input. MEM first.
  when(memWait === waitNone && mem.mode =/= RAMMode.NOP) {
    when(mem.addr.atRAM) {
      bindInput(mem, io.ram)
      when(RAMMode.isWrite(mem.mode)) {
        memWait := waitNone
        mem.ok := true.B
      }.otherwise {
        memWait := waitRAM
        mem.ok := false.B
      }
    }.elsewhen(mem.addr.atFlash) {
      when(flashFree) {
        bindInput(mem, io.flash)
        memWait := waitFlash
      }
    }.elsewhen(mem.addr.atSerial) {
      bindInput(mem, io.serial)
      when(RAMMode.isWrite(mem.mode)) {
        memWait := waitNone
        mem.ok := true.B
      }.otherwise {
        memWait := waitSerial
        mem.ok := false.B
      }
    }.otherwise {
      printf("[IO] MEM access invalid address: %x\n", mem.addr)
      stop(1)
    }
  }

  // Handle IF only when MEM is none
  when(ifWait === waitNone && memWait === waitNone && mem.mode === RAMMode.NOP && if_.mode =/= RAMMode.NOP) {
    when(if_.addr.atRAM) {
      bindInput(if_, io.ram)
      ifWait := waitRAM     // Readonly, always wait
      if_.ok := false.B
    }.elsewhen(if_.addr.atFlash) {
      bindInput(if_, io.flash)
      ifWait := waitFlash
      if_.ok := false.B
    }.otherwise {
      printf("[IO] IF access invalid address: %x\n", if_.addr)
      stop(1)
    }
  }
}

