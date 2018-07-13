import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import firrtl_interpreter.InterpretiveTester
// holy shxt the default tester does not allow peeking internal signals?!


class MyRegFileTest(rf: RegFile) extends PeekPokeTester(rf) {
  poke(rf.io._ID.read1.addr, 0)
  poke(rf.io._ID.read2.addr, 0)
  poke(rf.io._MEM.addr, 0)
  poke(rf.io._MEM.data, 9)
  step(1)
  expect(rf.io._ID.read1.data, 0)
  expect(rf.io._ID.read2.data, 0)

  poke(rf.io._MEM.addr, 1)
  poke(rf.io._MEM.data, 11111)
  step(1)

  poke(rf.io._MEM.addr, 2)
  poke(rf.io._MEM.data, 22222)
  step(1)

  poke(rf.io._MEM.addr, 3)
  poke(rf.io._MEM.data, 33333)
  step(1)

  poke(rf.io._ID.read1.addr, 1)
  poke(rf.io._ID.read2.addr, 2)
  expect(rf.io._ID.read1.data, 11111)
  expect(rf.io._ID.read2.data, 22222)
  poke(rf.io._ID.read1.addr, 3)
  poke(rf.io._ID.read2.addr, 1)
  expect(rf.io._ID.read1.data, 33333)
  expect(rf.io._ID.read2.data, 11111)

  poke(rf.io._MEM.addr, 31)
  poke(rf.io._MEM.data, 12345)
  step(1)
  poke(rf.io._ID.read1.addr, 31)
  expect(rf.io._ID.read1.data, 12345)
}

class MyIFTest(t: IFTestModule) extends PeekPokeTester(t) {
  // sequential if
  reset(10)
  poke(t.io.id.if_branch, 0)
  expect(t.io.id.pc, 0)
  for (i <- 0 until 7) {
    step(1)
    expect(t.io.id.pc, i*4)
    expect(t.io.id.inst, i*0x11110000 + (i+1)*0x00001111)
  }

  // branch
  reset(10)   // press rst for more than a while pls
  poke(t.io.id.if_branch, 0)
  expect(t.io.id.pc, 0)
  for (i <- 0 until 4) {
    step(1)
    expect(t.io.id.pc, i*4)
    expect(t.io.id.inst, i*0x11110000 + (i+1)*0x00001111)
  }
  // 3 instr left IF
  poke(t.io.id.if_branch, 1)
  poke(t.io.id.branch_tar, 40)
  step(1)
  expect(t.io.id.pc, 4*4)
  expect(t.io.id.inst, 0x44445555)
  step(1)
  expect(t.io.id.pc, 40)
  expect(t.io.id.inst, "h_aaaa_bbbb".U) // fxxk jvm
}


class MyIDTest(t: IDTestModule) extends PeekPokeTester(t) {
  poke(t.io.regw.addr, 1.U)
  poke(t.io.regw.data, 0x1001.U)
  step(1)
  poke(t.io.regw.addr, 2.U)
  poke(t.io.regw.data, 0x1002.U)
  step(1)
  poke(t.io.regw.addr, 3.U)
  poke(t.io.regw.data, 0x1003.U)
  step(1)
  poke(t.io.regw.addr, 4.U)
  poke(t.io.regw.data, 0x1004.U)
  step(1)
  poke(t.io.regw.addr, 5.U)
  poke(t.io.regw.data, 0x1005.U)
  step(1)
  poke(t.io.regw.addr, 6.U)
  poke(t.io.regw.data, 0x1006.U)
  step(1)
  poke(t.io.regw.addr, 7.U)
  poke(t.io.regw.data, 0x1007.U)
  step(1)
  poke(t.io.regw.addr, 8.U)
  poke(t.io.regw.data, 0x1008.U)
  step(1)
  poke(t.io.regw.addr, 9.U)
  poke(t.io.regw.data, 0x1009.U)
  step(1)

  val testcases = Array(
    //               inst , num1 , num2 ,              aluop , wregaddr ,  br , brtgt ,         inst
    Array("h_00A0_0093".U ,  0.U , 10.U , DecTable.ALUOP_ADD ,      1.U , 0.U ,  0.U) , // addi r1 r0 1
    Array("h_FAA0_8193".U ,  0x1001.U , "h_ffff_ffaa".U , DecTable.ALUOP_ADD ,3.U , 0.U ,  0.U), // addi r3 r1=0x1001 0xFAA=0xFFFFFFAA
    Array("h_8003_0793".U ,  0x1006.U , "h_ffff_f800".U , DecTable.ALUOP_ADD , 15.U , 0.U ,  0.U) //, addi r15 r6=0x1006 0x800=0xFFFFF800
  )

  for (tc <- testcases) {
    poke(t.io.iff.inst, tc(0))
    expect(t.io.ex.oprd1, tc(1))
    expect(t.io.ex.oprd2, tc(2))
    expect(t.io.ex.opt, tc(3))
    expect(t.io.ex.reg_w_add, tc(4))
    expect(t.io.iff.if_branch, tc(5))
    expect(t.io.iff.branch_tar, tc(6))
  }
}


class  Mytester extends ChiselFlatSpec {
  val args = Array[String]()
  // Regfile
  "Regfile" should "pass test" in {
    iotesters.Driver.execute(args, () => new RegFile()) {
      c => new MyRegFileTest(c)
    } should be (true)
  }
  // IF
  "IF module" should "pass test" in {
    iotesters.Driver.execute(args, () => new IFTestModule()) {
      c => new MyIFTest(c)
    } should be (true)
  }
  // ID
  "ID module" should "pass test" in {
    iotesters.Driver.execute(args, () => new IDTestModule()) {
      c => new MyIDTest(c)
    } should be (true)
  }
}


