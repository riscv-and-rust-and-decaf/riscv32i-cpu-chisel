import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import bundles._

class CoreTestModule extends Module {
  val io = IO(new Bundle {
    // debug things below
    val debug = new CoreState()
  })
  val d = io.debug

  val core = Module(new Core())
  val ram = Module(new SimRAM())

  core.io.ram <> ram.io.core
  d           <> core.d
}

class CoreTestWithoutFw(c: CoreTestModule) extends PeekPokeTester(c) {
  reset(10)

  step(5)
  expect(c.d.reg(1), 20)

  step(5)
  expect(c.d.reg(1), 20)
  expect(c.d.reg(2), 30)

  step(5)
  expect(c.d.reg(1), 20)
  expect(c.d.reg(2), 30)
  expect(c.d.reg(3), 10)

  step(5)
  expect(c.d.reg(1), 20)
  expect(c.d.reg(2), 30)
  expect(c.d.reg(3), 10)
  expect(c.d.reg(4), "h_ffff_ffff".U)

  step(5)
  expect(c.d.reg(1), 20)
  expect(c.d.reg(2), 30)
  expect(c.d.reg(3), 10)
  expect(c.d.reg(4), "h_ffff_ffff".U)
  expect(c.d.reg(5), "h_ffff_fff5".U)

  step(5)
  expect(c.d.reg(1), "h_ffff_fff5".U)
  expect(c.d.reg(2), 30)
  expect(c.d.reg(3), 10)
  expect(c.d.reg(4), "h_ffff_ffff".U)
  expect(c.d.reg(5), "h_ffff_fff5".U)
}


class CoreTestWithFw(c: CoreTestModule) extends PeekPokeTester(c) {
  reset(10)
  step(4) // pipeline entry
  expect(c.d.reg(1), 20)
  step(1)
  expect(c.d.reg(1), 20)
  expect(c.d.reg(2), 30)
  step(1)
  expect(c.d.reg(1), 20)
  expect(c.d.reg(2), 30)
  expect(c.d.reg(3), 10)
  step(1)
  expect(c.d.reg(1), 20)
  expect(c.d.reg(2), 30)
  expect(c.d.reg(3), 10)
  expect(c.d.reg(4), "h_ffff_ffff".U)
  step(1)
  expect(c.d.reg(1), 20)
  expect(c.d.reg(2), 30)
  expect(c.d.reg(3), 10)
  expect(c.d.reg(4), "h_ffff_ffff".U)
  expect(c.d.reg(5), "h_ffff_fff5".U)
  step(1)
  expect(c.d.reg(1), "h_ffff_fff5".U)
  expect(c.d.reg(2), 30)
  expect(c.d.reg(3), 10)
  expect(c.d.reg(4), "h_ffff_ffff".U)
  expect(c.d.reg(5), "h_ffff_fff5".U)
}

class NaiveInstTest(c: CoreTestModule) extends PeekPokeTester(c) {
  reset(10)
  step(4)
  expect(c.d.ifpc, 16)
  expect(c.d.reg(10), 10)
  step(1)
  expect(c.d.ifpc, 20)
  expect(c.d.ifinst, "h_fea09ce3".U)
  step(1)
  expect(c.d.id.pc, 20)
  expect(c.d.id.imm, -8)
  expect(c.d.id_branch, 12)
  step(45)
  expect(c.d.reg(7), 55)
  step(1000)
  expect(c.d.reg(4), 1000)
  expect(c.d.reg(5), 1597)
  expect(c.d.reg(6), 987)
  expect(c.d.reg(7), 1597)
  expect(c.d.reg(1), 597)
}

class LoadStoreInstTest(c: CoreTestModule) extends PeekPokeTester(c) {
  reset(10)
  step(4)
  expect(c.d.ifpc, 16)
  expect(c.d.reg(1), "h_87654000".U)
  step(1)
  expect(c.d.ifpc, 20)
  expect(c.d.reg(1), "h_87654000".U)
  expect(c.d.reg(2), "h_1".U)
  step(1)
  expect(c.d.ifpc, 24) // now the fourth inst (store) is in MEM
  expect(c.d.reg(1), "h_87654321".U)
  expect(c.d.reg(2), "h_1".U)
  step(1)
  expect(c.d.ifpc, 24) // not advancing because of the store inst
  expect(c.d.reg(1), "h_87654321".U)
  expect(c.d.reg(2), "h_1".U)
  step(1)
  expect(c.d.ifpc, 24) // not advancing because of the load inst
  expect(c.d.reg(1), "h_87654321".U)
  expect(c.d.reg(2), "h_87654321".U)
//  step(1)
//  expect(c.d.ifpc, 28) // now advancing
//  expect(c.d.log(1), "h_87654321".U)
//  expect(c.d.log(2), "h_87654543".U)
}


class CoreTester extends ChiselFlatSpec {
  val args = Array[String]()
//  "Core module fwno" should "pass test" in {
//    SrcBinReader.fname = "test_asm/test2.bin"
//    iotesters.Driver.execute(args, () => new CoreTestModule()) {
//      c => new CoreTestWithoutFw(c)
//    } should be (true)
//  }
//  "Core module fwyes" should "pass test" in {
//    SrcBinReader.fname = "test_asm/test3.bin"
//    iotesters.Driver.execute(args, () => new CoreTestModule()) {
//      c => new CoreTestWithFw(c)
//    } should be (true)
//  }
//  "Core test 1+2+..10" should "eq to 55" in {
//    SrcBinReader.fname = "test_asm/test4.bin"
//    iotesters.Driver.execute(args, () => new CoreTestModule()) {
//      c => new NaiveInstTest(c)
//    } should be (true)
//  }
  "Core test simple load/store" should "pass test" in {
    SrcBinReader.fname = "test_asm/test5.bin"
    iotesters.Driver.execute(args, () => new CoreTestModule()) {
      c => new LoadStoreInstTest(c)
    } should be (true)
  }
}

