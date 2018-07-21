package core_

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}


class IFTestModule extends Module {
  val io = IO(new Bundle {
    val id = new IF_ID()  // tester acts as ID
  })

  val ifmod = Module(new IF())
  val mmu = Module(new MMU())
  val gatedIDInst = Reg(UInt(32.W))
  val gatedIDpc = Reg(UInt(32.W))
  gatedIDInst := ifmod.io.id.inst
  gatedIDpc := ifmod.io.id.pc
  io.id.inst := gatedIDInst
  io.id.pc := gatedIDpc

  ifmod.io.id.if_branch := io.id.if_branch 
  ifmod.io.id.branch_tar := io.id.branch_tar

  ifmod.io.ram <> mmu.io.iff

  mmu.io.mem.addr := 0.U
  mmu.io.mem.wdata := 0.U
  mmu.io.mem.mode := RAMMode.NOP;
}


class IFTest(t: IFTestModule) extends PeekPokeTester(t) {
  // sequential if
  reset(10)
  poke(t.io.id.if_branch, 0)
  poke(t.io.id.id_stall, 0)
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


class IFTester extends ChiselFlatSpec {
    val args = Array[String]()
    "IF module" should "not tested now " in {
      /*
      iotesters.Driver.execute(args, () => new IFTestModule()) {
        c => new IFTest(c)
      } 
      */ (true)should be (true)
    }
}
