import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}


class CoreTestWithoutFw(c: Core) extends PeekPokeTester(c) {
  reset(10)
  step(5)
  expect(c.io.log(1), 20)
  step(5)
  expect(c.io.log(1), 20)
  expect(c.io.log(2), 30)
  step(5)
  expect(c.io.log(1), 20)
  expect(c.io.log(2), 30)
  expect(c.io.log(3), 10)
  step(5)
  expect(c.io.log(1), 20)
  expect(c.io.log(2), 30)
  expect(c.io.log(3), 10)
  expect(c.io.log(4), "h_ffff_ffff".U)
  step(5)
  expect(c.io.log(1), 20)
  expect(c.io.log(2), 30)
  expect(c.io.log(3), 10)
  expect(c.io.log(4), "h_ffff_ffff".U)
  expect(c.io.log(5), "h_ffff_fff5".U)
  step(5)
  expect(c.io.log(1), "h_ffff_fff5".U)
  expect(c.io.log(2), 30)
  expect(c.io.log(3), 10)
  expect(c.io.log(4), "h_ffff_ffff".U)
  expect(c.io.log(5), "h_ffff_fff5".U)
}


class CoreTestWithFw(c: Core) extends PeekPokeTester(c) {
  reset(10)
  step(4) // pipeline entry
  expect(c.io.log(1), 20)
  step(1)
  expect(c.io.log(1), 20)
  expect(c.io.log(2), 30)
  step(1)
  expect(c.io.log(1), 20)
  expect(c.io.log(2), 30)
  expect(c.io.log(3), 10)
  step(1)
  expect(c.io.log(1), 20)
  expect(c.io.log(2), 30)
  expect(c.io.log(3), 10)
  expect(c.io.log(4), "h_ffff_ffff".U)
  step(1)
  expect(c.io.log(1), 20)
  expect(c.io.log(2), 30)
  expect(c.io.log(3), 10)
  expect(c.io.log(4), "h_ffff_ffff".U)
  expect(c.io.log(5), "h_ffff_fff5".U)
  step(1)
  expect(c.io.log(1), "h_ffff_fff5".U)
  expect(c.io.log(2), 30)
  expect(c.io.log(3), 10)
  expect(c.io.log(4), "h_ffff_ffff".U)
  expect(c.io.log(5), "h_ffff_fff5".U)
}


class CoreTester extends ChiselFlatSpec {
  val args = Array[String]()
//  "Core module fwno" should "pass test" in {
//    SrcBinReader.fname = "test_asm/test2.bin"
//    iotesters.Driver.execute(args, () => new Core()) {
//      c => new CoreTestWithoutFw(c)
//    } should be (true)
//  }
  "Core module fwyes" should "pass test" in {
    SrcBinReader.fname = "test_asm/test3.bin"
    iotesters.Driver.execute(args, () => new Core()) {
      c => new CoreTestWithFw(c)
    } should be (true)
  }
}

