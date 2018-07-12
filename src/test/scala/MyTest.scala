import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import gcd.GCD

class MyGCDTest(gcd: GCD) extends PeekPokeTester(gcd) {
  val a = 6
  val b = 9

  poke(gcd.io.value1, a)
  poke(gcd.io.value2, b)
  poke(gcd.io.loadingValues, 1)
  step(1)
  poke(gcd.io.loadingValues, 0)

  step(100)

  expect(gcd.io.outputGCD, 3)
  expect(gcd.io.outputValid, 1)
}


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
  reset()
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


object tester {
  def main(args: Array[String]): Unit = {
    // regfile
    assert(
      iotesters.Driver.execute(args, () => new RegFile()) {
        c => new MyRegFileTest(c)
      })
    // traditional GCD
    assert(
      iotesters.Driver.execute(args, () => new GCD()) {
        c => new MyGCDTest(c)
      })
    // if
    assert(
      iotesters.Driver.execute(args, () => new IFTestModule()) {
        c => new MyIFTest(c)
      })
  }
}


