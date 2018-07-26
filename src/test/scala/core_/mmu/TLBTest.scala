package core_.mmu

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, PeekPokeTester}

class TLBTest(m: TLB) extends PeekPokeTester(m) {
  reset()
  poke(m.io.query.req.valid, false)
  poke(m.io.modify.mode, TLBOp.None)

  def insert(vpn: Int, ppn: Int): Unit = {
    poke(m.io.modify.mode, TLBOp.Insert)
    poke(m.io.modify.vpn.p2, vpn >> 10)
    poke(m.io.modify.vpn.p1, vpn & 0x3ff)
    poke(m.io.modify.ppn.p2, ppn >> 10)
    poke(m.io.modify.ppn.p1, ppn & 0x3ff)
    step(1)
  }

  def remove(vpn: Int): Unit = {
    poke(m.io.modify.mode, TLBOp.Remove)
    poke(m.io.modify.vpn.p2, vpn >> 10)
    poke(m.io.modify.vpn.p1, vpn & 0x3ff)
    step(1)
  }

  def clear(): Unit = {
    poke(m.io.modify.mode, TLBOp.Clear)
    step(1)
  }

  def check(vpn: Int, ppn: Option[Int]): Unit = {
    poke(m.io.query.req.valid, true)
    poke(m.io.query.req.bits.p2, vpn >> 10)
    poke(m.io.query.req.bits.p1, vpn & 0x3ff)
    expect(m.io.query.rsp.valid, ppn.isDefined)
    if(ppn.isDefined) {
      expect(m.io.query.rsp.bits.p2, ppn.get >> 10)
      expect(m.io.query.rsp.bits.p1, ppn.get & 0x3ff)
    }
  }

  // Insert
  insert(1, 2)
  check(0, None)
  check(1, Some(2))

  // Insert override
  insert(1, 0xfffff)
  check(1, Some(0xfffff))

  // Remove
  remove(1)
  check(1, None)

  // Insert to full
  insert(1, 4)
  insert(2, 3)
  insert(3, 2)
  insert(4, 1)
  check(1, Some(4))
  check(2, Some(3))
  check(3, Some(2))
  check(4, Some(1))
  check(5, None)

  // Insert replace
  insert(5, 5)
  check(1, None)
  check(2, Some(3))
  check(3, Some(2))
  check(4, Some(1))
  check(5, Some(5))

  // Remove and insert
  remove(2)
  insert(1, 1)
  check(1, Some(1))
  check(2, None)
  check(3, Some(2))
  check(4, Some(1))
  check(5, Some(5))

  // Clear
  clear()
  for(i <- 1 until 6) {
    check(i, None)
  }
}

class TLBTester extends ChiselFlatSpec {
  val args = Array[String]()
  "TLB module" should "pass test" in {
    iotesters.Driver.execute(args, () => new TLB(2)) {
      c => new TLBTest(c)
    } should be(true)
  }
}
