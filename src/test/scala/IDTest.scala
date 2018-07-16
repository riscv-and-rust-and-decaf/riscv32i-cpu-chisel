import chisel3._
import bundles._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}


class IDTest(t: ID) extends PeekPokeTester(t) {
  reset(10)
  poke(t.io.iff.inst, "b111111111110_00001_010_00010_0010011".U)
  poke(t.io.reg.read1.data,7)
  poke(t.io.reg.read2.data,8)
  step(1)

  expect(t.io.ex.oprd1, 7)
  expect(t.io.ex.oprd2, "h_ffff_fffe".U)
  expect(t.io.ex.opt, OptCode.SLT)

  poke(t.io.iff.inst, "h_fea0_9ce3".U)
  // 1111_1110_1010_0000_1001_1100_1110_0011
  step(1)
  poke(t.io.reg.read1.data, 1)
  poke(t.io.reg.read2.data, 10)
  expect(t.io.log_type, InstType.B)
  expect(t.io.log_opt, BType.BNE)
  expect(t.io.log_bt, "b0_1010".U)
  expect(t.io.log_l, true.B)
  expect(t.io.iff.if_branch, true.B)

}

class IDTest_CSR(t: ID) extends PeekPokeTester(t) {
  reset(10)
  poke(t.io.iff.inst, "b010010001001_01010_001_10000_1110011".U)
  poke(t.io.reg.read1.data, 19)
  poke(t.io.csr.rdata, 42)
  step(1)
  expect(t.io.log_fct3, 1.U)
  expect(t.io.csr.mode, 1.U)
  expect(t.io.csr.addr, "b010010001001".U)
  expect(t.io.csr.wdata, 19)
  expect(t.io.ex.oprd1, 42)
}

class IDTester extends ChiselFlatSpec {
    val args = Array[String]()
    "new ID module" should "pass test" in {
      iotesters.Driver.execute(args, () => new ID()) {
        c => new IDTest(c)
      } should be (true)
    }
    
    "ID module with CSR inst" should "pass test" in {
      iotesters.Driver.execute(args, () => new ID()) {
        c => new IDTest_CSR(c)
      } should be (true)
    }
}
