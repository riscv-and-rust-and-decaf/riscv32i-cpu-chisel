package devices

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, PeekPokeTester}

class MockRamTest(ram: MockRam) extends PeekPokeTester(ram) {
  reset()

  object RAMMode {
    val NOP = 0
    val LW  = 1
    val LH  = 2
    val LHU = 3
    val LB  = 4
    val LBU = 5
    val SW  = 9
    val SH  = 10
    val SB  = 12

    def isRead(x: Int): Boolean = x < 8 && x != 0
    def isWrite(x: Int): Boolean = x >= 8
  }

  def check(mode: Int, addr: Int, value: Int) {
    poke(ram.io.mode, mode)
    poke(ram.io.addr, addr)
    poke(ram.io.wdata, if(RAMMode.isWrite(mode)) value else 0)
    step(1)
    expect(ram.io.ok, mode != RAMMode.NOP)
    expect(ram.io.rdata, if(RAMMode.isRead(mode)) value else 0)
  }

  check(RAMMode.SW, 0, 0x12345678)
  check(RAMMode.LW, 0, 0x12345678)
  check(RAMMode.LH, 0, 0x00005678)
  check(RAMMode.LB, 0, 0x00000078)
  check(RAMMode.LB, 0, 0x00000078)
}

class MockRamTester extends ChiselFlatSpec {
  val args = Array[String]()
  "MockRam module" should "pass test" in {
    iotesters.Driver.execute(args, () => new MockRam()) {
      c => new MockRamTest(c)
    } should be(true)
  }
}
