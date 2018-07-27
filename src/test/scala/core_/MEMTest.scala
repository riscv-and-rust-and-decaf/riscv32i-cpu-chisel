package core_

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import Const._
import OptCode._

class MEMTest(mem: MEM) extends PeekPokeTester(mem) {
  poke(mem.io.ex.excep.valid, false.B)
  poke(mem.io.flush, false.B)
  
  // No IO
  poke(mem.io.ex.ramOp.mode, RAMMode.NOP)
  poke(mem.io.ex.wrRegOp.addr, 10)
  poke(mem.io.ex.wrRegOp.data, 44)
  poke(mem.io.mmu.ok, 0)
  
  step(1)
  expect(mem.io.wrRegOp.addr, 10)
  expect(mem.io.wrRegOp.data, 44)
  expect(mem.io.mmu.mode, RAMMode.NOP)
  expect(mem.io.ex.ready, true)

  // Write on 1 cycle
  poke(mem.io.ex.ramOp.mode, RAMMode.SW)
  poke(mem.io.ex.ramOp.addr, 44)
  poke(mem.io.ex.ramOp.wdata, 9901)
  step(1)
  expect(mem.io.mmu.mode, RAMMode.SW)
  expect(mem.io.mmu.addr, 44)
  expect(mem.io.mmu.wdata, 9901)
  poke(mem.io.mmu.ok, 1)
  expect(mem.io.ex.ready, true)

  // Read on 2 cycle
  poke(mem.io.ex.ramOp.mode, RAMMode.LW)
  poke(mem.io.ex.ramOp.addr, 44)
  step(1)
  poke(mem.io.mmu.ok, 0)
  expect(mem.io.wrRegOp.addr, 0)
  expect(mem.io.ex.ready, false)

  //   Should keep last input when stall
  poke(mem.io.ex.ramOp.mode, 0)
  poke(mem.io.ex.ramOp.addr, 0)
  step(1)
  expect(mem.io.mmu.mode, RAMMode.LW)
  expect(mem.io.mmu.addr, 44)

  poke(mem.io.mmu.ok, 1)
  poke(mem.io.mmu.rdata, 4321)
  expect(mem.io.wrRegOp.addr, 10)
  expect(mem.io.wrRegOp.data, 4321)
  expect(mem.io.ex.ready, true)
}

class MEMTester extends ChiselFlatSpec {
    val args = Array[String]()
    "MEM module" should "pass test" in {
      iotesters.Driver.execute(args, () => new MEM()) {
        c => new MEMTest(c)
      } should be (true)
    }
}
