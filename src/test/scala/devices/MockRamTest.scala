package devices

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, PeekPokeTester}

class MockRamTest(ram: MockRam) extends PeekPokeTester(ram) {
  reset()

  def width2mask(width: Int): UInt =
    width match {
      case 8 => "b1110".U
      case 16 => "b1100".U
      case 32 => "b0000".U
      case _ => throw new IllegalArgumentException
    }

  def read(addr: Int, width: Int, value: Int) {
    poke(ram.io.isRead, true)
    poke(ram.io.isWrite, false)
    poke(ram.io.addr, addr)
    poke(ram.io.wdata, 0)
    poke(ram.io.mask, width2mask(width))
    step(1)
    expect(ram.io.ok, true)
    expect(ram.io.rdata, value)
  }

  def write(addr: Int, width: Int, value: Int) {
    poke(ram.io.isRead, false)
    poke(ram.io.isWrite, true)
    poke(ram.io.addr, addr)
    poke(ram.io.wdata, value)
    poke(ram.io.mask, width2mask(width))
    step(1)
    expect(ram.io.ok, true)
    expect(ram.io.rdata, 0)
  }

  write(0, 32, 0x12345678)
  read(0, 32, 0x12345678)
  read(0, 16, 0x00005678)
  read(0, 8, 0x00000078)
  read(2, 8, 0x00000034)

  write(2, 16, 0x4321)
  read(0, 32, 0x43215678)
}

class MockRamTester extends ChiselFlatSpec {
  val args = Array[String]()
  "MockRam module" should "pass test" in {
    iotesters.Driver.execute(args, () => new MockRam()) {
      c => new MockRamTest(c)
    } should be(true)
  }
}
