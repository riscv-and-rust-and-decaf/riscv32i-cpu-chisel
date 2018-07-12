import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import OptCode._

class EXTest(ex: EX) extends PeekPokeTester(ex) {
  poke(ex.io._ID.oprd1, 7)
  poke(ex.io._ID.oprd2, 3)
  poke(ex.io._ID.opt, ADD)

  expect(ex.io._MEM.alu_out, 10)
  step(1)
  
  poke(ex.io._ID.oprd1, 5)
  poke(ex.io._ID.oprd2, 4)
  poke(ex.io._ID.opt, SUB)

  expect(ex.io._MEM.alu_out, 1)
}

class ExTester extends ChiselFlatSpec {
    val args = Array[String]()
    //EX
    "ex module" should "pass test" in {
      iotesters.Driver.execute(args, () => new EX()) {
        c => new EXTest(c)
      } should be (true)
    }
}
