package gcd

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}


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
  poke(rf.io.raddr1, 0)
  poke(rf.io.raddr2, 0)
  poke(rf.io.waddr, 0)
  step(1)
  expect(rf.io.rdata1, 0)
  expect(rf.io.rdata2, 0)

  poke(rf.io.waddr, 1)
  poke(rf.io.wdata, 11111)
  step(1)

  poke(rf.io.waddr, 2)
  poke(rf.io.wdata, 22222)
  step(1)

  poke(rf.io.waddr, 3)
  poke(rf.io.wdata, 33333)
  step(1)

  poke(rf.io.raddr1, 1)
  poke(rf.io.raddr2, 2)
  expect(rf.io.rdata1, 11111)
  expect(rf.io.rdata2, 22222)
  poke(rf.io.raddr1, 3)
  poke(rf.io.raddr2, 1)
  expect(rf.io.rdata1, 33333)
  expect(rf.io.rdata2, 11111)

  poke(rf.io.waddr, 31)
  poke(rf.io.wdata, 12345)
  step(1)
  poke(rf.io.raddr1, 31)
  expect(rf.io.rdata1, 12345)
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


