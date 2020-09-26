package core_

import chisel3._
import chisel3.iotesters.PeekPokeTester

object TestUtil {
  def bindRAM(ready: Bool,
              ram_init: RAMOp, ram_origin: RAMOp, ram: RAMOp) {
    when(ready) {
      ram.mode := ram_origin.mode
      ram.addr := ram_origin.addr
      ram.wdata := ram_origin.wdata
    }.otherwise {
      ram.mode := ram_init.mode
      ram.addr := ram_init.addr
      ram.wdata := ram_init.wdata
    }
    ram_origin.rdata := ram.rdata
    ram_origin.ok := ram.ok
    ram_init.rdata := ram.rdata
    ram_init.ok := ram.ok
  }

  def loadRAM[T <: MultiIOModule](tester: PeekPokeTester[T], ready: Bits,
                                  ram_init: RAMOp, data: Seq[Long]) {
    tester.poke(ready, 0)
    tester.poke(ram_init.mode, 9) // SW

    for(i <- data.indices) {
      if(data(i) != 0) {
        tester.poke(ram_init.addr, i * 4)
        tester.poke(ram_init.wdata, data(i))
        tester.step(1)
      }
    }
    tester.poke(ram_init.mode, 0)
    tester.step(5)
    tester.poke(ready, 1)
  }
}
