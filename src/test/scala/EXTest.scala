import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import OptCode._

class EXTest(ex: EX) extends PeekPokeTester(ex) {
  poke(ex.io.id.oprd1, 7)
  poke(ex.io.id.oprd2, 3)
  poke(ex.io.id.opt, ADD)
  step(1)
  expect(ex.io.mem.alu_out, 10)
  
  poke(ex.io.id.oprd1, 5)
  poke(ex.io.id.oprd2, 4)
  poke(ex.io.id.opt, SUB)
  step(1)
  expect(ex.io.mem.alu_out, 1)
}

class EXTester extends ChiselFlatSpec {
    val args = Array[String]()
    //EX
    "EX module" should "pass test" in {
      iotesters.Driver.execute(args, () => new EX()) {
        c => new EXTest(c)
      } should be (true)
    }
}
