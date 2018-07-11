package gcd

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class MyGCDTest(gcd: GCD ) extends PeekPokeTester(gcd) {
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

object tester {
  def main(args: Array[String]): Unit = {
    assert(
      iotesters.Driver.execute(args, () => new GCD()) {
        c => new MyGCDTest(c)
      })
  }
}


