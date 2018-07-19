package devices

import chisel3._
import chisel3.util.Cat

class MockRam extends Module {
  val io = IO(Flipped(new IOPort))

  private val mem = Mem(0x800000, UInt(8.W))

  io.ok := io.isRead ^ io.isWrite
  val rbytes = Array(Wire(UInt(8.W)), Wire(UInt(8.W)), Wire(UInt(8.W)), Wire(UInt(8.W)))
  io.rdata := Cat(rbytes.reverse)

  for (i <- 0 until 4) {
    val bitEnable = !io.mask(i)
    val memByte = mem(io.addr + i.U)
    rbytes(i) := 0.U
    when(bitEnable) {
      when(io.isRead) {
        rbytes(i) := memByte
      }
      when(io.isWrite) {
        memByte := io.wdata(i * 8 + 7, i * 8)
      }
    }
  }
}
