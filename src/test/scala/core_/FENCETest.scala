
package core_

import chisel3._
import chisel3.iotesters._
import devices._

class FenceInstTest(c: CoreTestModule, fname: String) extends CoreTest(c, fname) {
  step(100)
  expect(c.d.reg(1), 5)
}

class CoreFenceTester extends ChiselFlatSpec {
  val args = Array[String]("-fiwv")
  "fence and fence.i test" should "pass test" in {
    iotesters.Driver.execute(args, () => new CoreTestModule()) {
      c => new FenceInstTest(c, "test_asm/test_fence.bin")
    } should be (true)
  }
}
