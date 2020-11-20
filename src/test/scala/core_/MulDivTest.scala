package core_

import chisel3._
import chisel3.iotesters._
import devices._

class MulDivTester extends ChiselFlatSpec {
  val args = Array[String]("-fiwv")
  "m extension" should "pass test" in {
    iotesters.Driver.execute(args, () => new CoreTestModule("test_asm/test_m_extension.hex")) {
      c => new CoreTestNew(c, 100)
    } should be (true)
  }
}
