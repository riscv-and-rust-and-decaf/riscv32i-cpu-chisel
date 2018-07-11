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


object tester {
  def main(args: Array[String]): Unit = {
    println("MyRegFileTest")
    assert(
      iotesters.Driver.execute(args, () => new RegFile()) {
        c => new MyRegFileTest(c)
      })
    println("MyGCDTest")
    assert(
      iotesters.Driver.execute(args, () => new GCD()) {
        c => new MyGCDTest(c)
      })
  }
}


