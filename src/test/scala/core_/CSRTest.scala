package core_

import chisel3._
import chisel3.iotesters._
import devices._

class CoreCSRTester extends ChiselFlatSpec {
  val args = Array[String]("-fiwv")
  "Core simple csr test" should "pass test" in {
    iotesters.Driver.execute(args, () => new CoreTestModule("test_asm/test_csr.hex")) {
      c => new CoreTest(c) {
        step(17)
        expect(c.d.reg(2), 13)
        expect(c.d.reg(3), 11)
        expect(c.d.reg(4), 8)
        expect(c.d.reg(5), 10)
      }
    } should be (true)
  }
  "Core ecall test" should "pass test" in {
    iotesters.Driver.execute(args, () => new CoreTestModule("test_asm/test_ecall.hex")) {
      c => new CoreTest(c) {
        step(100)
        expect(c.d.reg(1), 13)
        expect(c.d.reg(8), 8)
        expect(c.d.reg(20), 11)
        expect(c.d.reg(30), 30)
      }
    } should be (true)
  }
  "eret" should "pass test" in {
    iotesters.Driver.execute(args, () => new CoreTestModule("test_asm/test_ret.hex")) {
      c => new CoreTestNew(c, 100)
    } should be (true)
  }
}

class TimeInterruptTester extends ChiselFlatSpec {
  val args = Array[String]("-fiwv")
  "time interupt" should "pass test" in {
    iotesters.Driver.execute(args, () => new CoreTestModule("test_asm/test_mtime.hex")) {
      c => new CoreTest(c) {
        step(249)
        expect(c.d.reg(1), 4)
        expect(c.d.reg(7), 2147483652L.U)
      }
    } should be (true)
  }
}

