import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import Const._
import OptCode._

class MEMTest(mem: MEM) extends PeekPokeTester(mem) {
  poke(mem.io._EX.alu_out, 44)
  poke(mem.io._EX.opt, SUB)
  poke(mem.io._EX.reg_w_add, 10)
  step(1)
  expect(mem.io._Reg.addr, 10)
  expect(mem.io._Reg.data, 44)
  expect(mem.io._MMU.mode, MMU_MODE_NOP)

  poke(mem.io._EX.opt, SW)
  poke(mem.io._EX.store_data, 9901)
  step(1)
  expect(mem.io._MMU.mode, MMU_MODE_SW)
  expect(mem.io._MMU.addr, 44)
  expect(mem.io._MMU.wdata, 9901)

  poke(mem.io._EX.opt, LW)
  poke(mem.io._MMU.rdata, 4321)
  step(1)
  expect(mem.io._MMU.mode, MMU_MODE_LW)
  expect(mem.io._Reg.addr, 10)
  expect(mem.io._Reg.data, 4321)
}

class MEMTester extends ChiselFlatSpec {
    val args = Array[String]()
    "MEM module" should "pass test" in {
      iotesters.Driver.execute(args, () => new MEM()) {
        c => new MEMTest(c)
      } should be (true)
    }
}
