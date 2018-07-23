package core_

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import Const._
import OptCode._

class MEMTest(mem: MEM) extends PeekPokeTester(mem) {
  poke(mem.io.exExcep.en, false.B)
  poke(mem.io.csrExcepEn, false.B)
  
  poke(mem.io.ex.opt, SUB)
  poke(mem.io.exWrRegOp.addr, 10)
  poke(mem.io.exWrRegOp.data, 44)
  step(1)
  expect(mem.io.wrRegOp.addr, 10)
  expect(mem.io.wrRegOp.data, 44)
  expect(mem.io.mmu.mode, RAMMode.NOP)

  poke(mem.io.ex.opt, SW)
  poke(mem.io.ex.alu_out, 44)
  poke(mem.io.ex.store_data, 9901)
  step(1)
  expect(mem.io.mmu.mode, RAMMode.SW)
  expect(mem.io.mmu.addr, 44)
  expect(mem.io.mmu.wdata, 9901)

  poke(mem.io.ex.opt, LW)
  poke(mem.io.mmu.rdata, 4321)
  step(1)
  expect(mem.io.mmu.mode, RAMMode.LW)
  expect(mem.io.wrRegOp.addr, 10)
  expect(mem.io.wrRegOp.data, 4321)
}

class MEMTester extends ChiselFlatSpec {
    val args = Array[String]()
    "MEM module" should "pass test" in {
      iotesters.Driver.execute(args, () => new MEM()) {
        c => new MEMTest(c)
      } should be (true)
    }
}
