import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}


class CoreTest(c: Core) extends PeekPokeTester(c) {
  for (i <- 1 to 30) {
    println(s"\n\n\ncycle: $i")
    step(1)
  }
  expect(c.io.log, 9)
}

class CoreTester extends ChiselFlatSpec {
    val args = Array[String]()
    "Core module" should "pass test" in {
      iotesters.Driver.execute(args, () => new Core()) {
        c => new CoreTest(c)
      } should be (true)
    }
}

