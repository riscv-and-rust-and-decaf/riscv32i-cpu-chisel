package core_

import chisel3._
import chisel3.iotesters._
import devices._

class MulDivTest(c: CoreTestModule, fname: String) extends CoreTest(c, fname) {
  step(40)
  expect(c.d.reg(3), 2)
  expect(c.d.reg(4), 4)
  expect(c.d.reg(5), 1)
}

class MulDivTester extends ChiselFlatSpec {
  val args = Array[String]("-fiwv")
  "m extension" should "pass test" in {
    iotesters.Driver.execute(args, () => new CoreTestModule()) {
      c => new FenceInstTest(c, "test_asm/test_m_extension.bin")
    } should be (true)
  }
}
