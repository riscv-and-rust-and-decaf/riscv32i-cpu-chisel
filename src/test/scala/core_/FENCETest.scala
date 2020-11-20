
package core_

import chisel3._
import chisel3.iotesters._
import devices._

class FenceInstTest(c: CoreTestModule) extends CoreTest(c) {
  step(100)
  expect(c.d.reg(1), 5)
}

class CoreFenceTester extends ChiselFlatSpec {
  val args = Array[String]("-fiwv")
  "fence and fence.i test" should "pass test" in {
    iotesters.Driver.execute(args, () => new CoreTestModule("test_asm/test_fence.hex")) {
      c => new FenceInstTest(c)
    } should be (true)
  }
}
