import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}


class CoreTest(c: Core) extends PeekPokeTester(c) {
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

class CoreTester extends ChiselFlatSpec {
  SrcBinReader.fname = "test_asm/test2.bin"
  val args = Array[String]()
  "Core module" should "pass test" in {
    iotesters.Driver.execute(args, () => new Core()) {
      c => new CoreTest(c)
    } should be (true)
  }
}

