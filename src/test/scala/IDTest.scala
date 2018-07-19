import chisel3._
import bundles._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

/*
class IDTestModule extends Module {
  val io = IO(new Bundle {
    val iff = Flipped(new IF_ID())  // tester acts as ID
    val ex = new ID_EX()
    val wrRegOp = new WrRegOp
    val regw = Flipped(new MEM_Reg())
  })

  val reg = Module(new RegFile())
  reg.io.mem.addr := io.regw.addr
  reg.io.mem.data := io.regw.data

  val emptyWrRegOp = Wire(new WrRegOp())
  emptyWrRegOp.addr := 0.U
  emptyWrRegOp.data := 0.U
  emptyWrRegOp.rdy  := false.B

  val id = Module(new ID())
  id.io.exWrRegOp <> emptyWrRegOp
  id.io.memWrRegOp <> emptyWrRegOp
  id.io.wbWrRegOp <> emptyWrRegOp
  id.io.iff.pc := io.iff.pc
  id.io.iff.inst := io.iff.inst
  io.iff.if_branch := id.io.iff.if_branch
  io.iff.branch_tar := id.io.iff.branch_tar
  id.io.reg.read1 <> reg.io.id.read1
  id.io.reg.read2 <> reg.io.id.read2
  io.ex.oprd1 := id.io.ex.oprd1
  io.ex.oprd2 := id.io.ex.oprd2
  io.ex.opt := id.io.ex.opt
  io.wrRegOp <> id.io.wrRegOp
  io.ex.store_data := id.io.ex.store_data
}


class IDTest(t: IDTestModule) extends PeekPokeTester(t) {
  // initialize register values
  for (i <- 1 until 10) {
    poke(t.io.regw.addr, i.U)
    poke(t.io.regw.data, (0x1000 + i).U)
    step(1)
  }

  val testcases = Array(
    //               inst , num1 , num2 ,              aluop , wregaddr ,  br , brtgt ,         inst
    Array("h_00A0_0093".U ,  0.U , 10.U , OptCode.ADD ,      1.U , 0.U ,  0.U) , // addi r1 r0 1
    Array("h_FAA0_8193".U ,  0x1001.U , "h_ffff_ffaa".U , OptCode.ADD ,3.U , 0.U ,  0.U), // addi r3 r1=0x1001 0xFAA=0xFFFFFFAA
    Array("h_8003_0793".U ,  0x1006.U , "h_ffff_f800".U , OptCode.ADD , 15.U , 0.U ,  0.U) //, addi r15 r6=0x1006 0x800=0xFFFFF800
  )

  for (tc <- testcases) {
    poke(t.io.iff.inst, tc(0))
    step(1)
    expect(t.io.ex.oprd1, tc(1))
    expect(t.io.ex.oprd2, tc(2))
    expect(t.io.ex.opt, tc(3))
    expect(t.io.wrRegOp.addr, tc(4))
    expect(t.io.iff.if_branch, tc(5))
    expect(t.io.iff.branch_tar, tc(6))
  }
}


class IDTester extends ChiselFlatSpec {
    val args = Array[String]()
    "ID module" should "pass test" in {
      iotesters.Driver.execute(args, () => new IDTestModule()) {
        c => new IDTest(c)
      } should be (true)
    }
}
*/

class IDTest(t: ID) extends PeekPokeTester(t) {
  reset(10)
  poke(t.io.iff.inst, "b111111111110_00001_010_00010_0010011".U)
  poke(t.io.reg.read1.data,7)
  poke(t.io.reg.read2.data,8)
  step(1)

  expect(t.io.ex.oprd1, 7)
  expect(t.io.ex.oprd2, "h_ffff_fffe".U)
  expect(t.io.ex.opt, OptCode.SLT)

  poke(t.io.iff.inst, "h_fea0_9ce3".U)
  // 1111_1110_1010_0000_1001_1100_1110_0011
  step(1)
  poke(t.io.reg.read1.data, 1)
  poke(t.io.reg.read2.data, 10)
  expect(t.io.log_type, InstType.B)
  expect(t.io.log_opt, BType.BNE)
  expect(t.io.log_bt, "b0_1010".U)
  expect(t.io.log_l, true.B)
  expect(t.io.iff.if_branch, true.B)

}

class IDTester extends ChiselFlatSpec {
    val args = Array[String]()
    "new ID module" should "pass test" in {
      iotesters.Driver.execute(args, () => new ID()) {
        c => new IDTest(c)
      } should be (true)
    }
}
