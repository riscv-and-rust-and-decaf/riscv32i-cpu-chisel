package devices

import chisel3._
import chisel3.util._
import core_._


class MockRam(printLog: Boolean = true) extends Module {
  val io = IO(Flipped(new RAMOp))

  private val mem = Mem(0x800000, UInt(8.W))

  io.ok := io.mode =/= RAMMode.NOP
  val data = Cat((0 until 4).reverse.map(i => mem(io.addr + i.U)))

  switch(io.mode) {
    is(RAMMode.SW) {
      for (i <- 0 until 4)
        mem(io.addr + i.U) := io.wdata(i * 8 + 7, i * 8)
      if (printLog)
        printf("[RAM] SW: [%x]=%x\n", io.addr, io.wdata)
    }
    is(RAMMode.SH) {
      mem(io.addr + 1.U) := io.wdata(15, 8)
      mem(io.addr) := io.wdata(7, 0)
      if (printLog)
        printf("[RAM] SH: [%x]=%x\n", io.addr, io.wdata)
    }
    is(RAMMode.SB) {
      mem(io.addr) := io.wdata(7, 0)
      if (printLog)
        printf("[RAM] SB: [%x]=%x\n", io.addr, io.wdata)
    }
  }

  io.rdata := 0.U
  switch(io.mode) {
    is(RAMMode.LW) {
      io.rdata := data
      if (printLog)
        printf("[RAM] LW: [%x]->%x\n", io.addr, io.rdata)
    }
    is(RAMMode.LH) {
      io.rdata := Cat(Mux(data(15), 0xff.U, 0.U), data(15, 0))
      if (printLog)
        printf("[RAM] LH: [%x]->%x\n", io.addr, io.rdata)
    }
    is(RAMMode.LB) {
      io.rdata := Cat(Mux(data(7), 0xfff.U, 0.U), data(7, 0))
      if (printLog)
        printf("[RAM] LB: [%x]->%x\n", io.addr, io.rdata)
    }
    is(RAMMode.LHU) {
      io.rdata := data(15, 0).zext.asUInt
      if (printLog)
        printf("[RAM] LHU: [%x]->%x\n", io.addr, io.rdata)
    }
    is(RAMMode.LBU) {
      io.rdata := data(7, 0).zext.asUInt
      if (printLog)
        printf("[RAM] LBU: [%x]->%x\n", io.addr, io.rdata)
    }
  }
}
