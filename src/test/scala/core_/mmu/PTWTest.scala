package core_.mmu

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, PeekPokeTester}
import chisel3.util._
import core_.RAMOp
import devices.MockRam
import java.io.File

class PTWTestModule(ramDataFile: String) extends Module {
  val io = IO(new Bundle {
    val root     = Input(new PN())
    val req      = DeqIO(new PN())
    val rsp      = EnqIO(new PTE())
  })

  val ptw = Module(new PTW)
  val ram = Module(new MockRam(ramDataFile))

  ptw.io.root <> io.root
  ptw.io.req <> io.req
  ptw.io.rsp <> io.rsp
  ptw.io.mem <> ram.io
}

class PTWTest(m: PTWTestModule) extends PeekPokeTester(m) {
  reset()
  // Init
  poke(m.io.req.valid, false)
  poke(m.io.rsp.ready, true)
  poke(m.io.root.p2, 0)
  poke(m.io.root.p1, 0)
  step(1)

  def check(vpn: Long, ppn: Option[Long]): Unit = {
    print("Testing %05x -> %s\n".format(vpn, if(ppn.isDefined) "%05x".format(ppn.get) else "None" ))
    // Request
    expect(m.io.req.ready, true)
    poke(m.io.req.valid, true)
    poke(m.io.req.bits.p2, (vpn >> 10) & 0x3ff)
    poke(m.io.req.bits.p1, vpn & 0x3ff)
    step(1)
    poke(m.io.req.valid, false)
    // Response
    while(peek(m.io.rsp.valid) != 1)
      step(1)
    expect(m.io.rsp.bits.V, ppn.isDefined)
    if(ppn.isDefined) {
      expect(m.io.rsp.bits.ppn.p2, (ppn.get >> 10) & 0x3ff)
      expect(m.io.rsp.bits.ppn.p1, ppn.get & 0x3ff)
    }
    step(1)
  }

  check(0x80000, Some(0x400)) // Valid huge page
  check(0x80400, None)        // Huge page not aligned
  check(0x40000, Some(2))     // Valid page
  check(0x40001, None)        // Invalid P1 entry
  check(0x40002, None)        // Zero P1 entry
  check(0x00000, None)        // Zero P2 entry
  check(0xffffe, Some(0))     // Recursive to root
  check(0xfffff, None)        // Recursive but invalid P1 entry
}

object PTE_ {
  val U = 1 << 4
  val X = 1 << 3
  val W = 1 << 2
  val R = 1 << 1
  val V = 1 << 0
  val VRW = V | R | W
  def make(ppn: Long, flags: Int) = ppn << 10 | flags
}

class PageTableMemBuilder {
  val mem = Array.fill(8, 1024)(0L)

  def setPTE(page: Int, idx: Int, pte: Long): PageTableMemBuilder = {
    mem(page)(idx) = pte
    this
  }
  def writeToFile(filePath: String) {
    import java.io._
    val file = new PrintWriter(new File(filePath))
    for(word <- mem.flatten) {
      for(i <- 0 until 4) {
        file.println("%02x".format((word >> (i * 8)) & 0xff))
      }
    }
    file.close()
  }
}

class PTWTester extends ChiselFlatSpec {
  val args = Array[String]()
  "PageTableWalker module" should "pass test" in {
    (new PageTableMemBuilder)
      .setPTE(0, 0x3ff, PTE_.make(0, PTE_.V))         // Map [0x3ff] to root as PDE
      .setPTE(0, 0x3fe, PTE_.make(0, PTE_.VRW))       // Map [0x3fe] to root as PTE
      .setPTE(0, 0x200, PTE_.make(0x400, PTE_.VRW))   // Map [0x200] to a huge page
      .setPTE(0, 0x201, PTE_.make(0x401, PTE_.VRW))   // Map [0x201] to a not aligned huge page
      .setPTE(0, 0x100, PTE_.make(1, PTE_.V))         // Map [0x100] to a next level page table
      .setPTE(1, 0x000, PTE_.make(2, PTE_.VRW))       // Map [0x100][0] to a page
      .setPTE(1, 0x001, PTE_.make(2, PTE_.V))         // Map [0x100][1] to invalid
      .writeToFile("PTWTest.hex")

    iotesters.Driver.execute(args, () => new PTWTestModule("PTWTest.hex")) {
      c => new PTWTest(c)
    } should be(true)
  }
}
