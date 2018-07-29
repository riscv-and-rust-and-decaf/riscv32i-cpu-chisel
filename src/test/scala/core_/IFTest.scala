package core_

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class IFTest(t: IF) extends PeekPokeTester(t) {
  def reset_poke_init(): Unit = {
    reset(10) // press rst for more than a while pls
    poke(t.io.id.branch.valid, 0)
    poke(t.io.id.ready, 1)
    poke(t.io.mmu.ok, 1)
  }

  val PC_BASE = 0x80000000L

  // sequential if
  reset_poke_init()
  for (i <- 0 until 7) {
    val some_inst = i*0x11110000 + (i+1)*0x00001111
    poke(t.io.mmu.rdata, some_inst)
    expect(t.io.id.pc, PC_BASE + i*4)
    expect(t.io.mmu.addr, PC_BASE + i*4)
    expect(t.io.mmu.mode, RAMMode.LW)
    expect(t.io.id.inst, some_inst)
    step(1)
  }

  // Stall
  reset_poke_init()
  poke(t.io.mmu.ok, 0)
  step(1)
  expect(t.io.id.pc, 0)
  expect(t.io.mmu.addr, PC_BASE)
  expect(t.io.mmu.mode, RAMMode.LW)
  step(1)
  expect(t.io.id.pc, 0)
  poke(t.io.id.ready, 0)
  step(1)
  expect(t.io.id.pc, 0)
  poke(t.io.mmu.ok, 1)
  step(1)
  expect(t.io.id.pc, 0)
  poke(t.io.id.ready, 1)
  step(1)
  expect(t.io.id.pc, PC_BASE + 4)

  // Branch (inst is ignored)
  reset_poke_init()
  //   Only branch
  poke(t.io.id.branch.valid, 1)
  poke(t.io.id.branch.bits, PC_BASE + 40)
  step(1)
  poke(t.io.id.branch.valid, 0)
  expect(t.io.id.pc, PC_BASE + 40)
  //   Branch with stall
  poke(t.io.id.branch.valid, 1)
  poke(t.io.id.branch.bits, PC_BASE + 80)
  poke(t.io.mmu.ok, 0)
  step(1)
  poke(t.io.id.branch.valid, 0)
  expect(t.io.id.pc, 0)
  poke(t.io.mmu.ok, 1)
  step(1)
  expect(t.io.id.pc, PC_BASE + 80)
}


class IFTester extends ChiselFlatSpec {
    val args = Array[String]()
    "IF module" should "not tested now " in {
      iotesters.Driver.execute(args, () => new IF()) {
        c => new IFTest(c)
      } should be (true)
    }
}
