package core_

import chisel3._
import chisel3.iotesters._
import devices._

class CSRInstTest(c: CoreTestModule, fname: String) extends CoreTest(c, fname) {
  step(17)
  expect(c.d.reg(2), 13)
  expect(c.d.reg(3), 11)
  expect(c.d.reg(4), 8)
  expect(c.d.reg(5), 10)
}

class ECALLTest(c: CoreTestModule, fname: String) extends CoreTest(c, fname) {
  step(100)
  expect(c.d.reg(1), 13)
  expect(c.d.reg(8), 8)
  expect(c.d.reg(20), 11)
  expect(c.d.reg(30), 30)
}

class TimeTest(c: CoreTestModule, fname: String) extends CoreTest(c, fname) {
  step(249)
  expect(c.d.reg(1), 4)
  expect(c.d.reg(7), 2147483652L.U)
}

class CoreCSRTester extends ChiselFlatSpec {
  val args = Array[String]("-fiwv")
  "Core simple csr test" should "pass test" in {
    iotesters.Driver.execute(args, () => new CoreTestModule()) {
      c => new CSRInstTest(c, "test_asm/test_csr.bin")
    } should be (true)
  }
  "Core ecall test" should "pass test" in {
    iotesters.Driver.execute(args, () => new CoreTestModule()) {
      c => new ECALLTest(c, "test_asm/test_ecall.bin")
    } should be (true)
  }
  "eret" should "pass test" in {
    iotesters.Driver.execute(args, () => new CoreTestModule(false)) {
      c => new CoreTestNew(c, "test_asm/test_ret.bin", 100)
    } should be (true)
  }
}

class TimeInterruptTester extends ChiselFlatSpec {
  val args = Array[String]("-fiwv")
  "time interupt" should "pass test" in {
    iotesters.Driver.execute(args, () => new CoreTestModule()) {
      c => new TimeTest(c, "test_asm/test_mtime.bin")
    } should be (true)
  }
}

