package core_

import java.io.File

import chisel3._
import chisel3.iotesters._
import devices._

/*
  After reset, tester should first set `ready` to false,
  and load init data to RAM through `ram_init`.
 */
class CoreTestModule(trace: Boolean = true) extends Module {
  val io = IO(new Bundle {
    val ready    = Input(Bool())
    val ram_init = Flipped(new RAMOp())
    val debug    = new CoreState()
  })
  val d  = io.debug

  val core   = Module(new Core())
  val ioCtrl = Module(new IOManager())
  val ram    = Module(new MockRam(trace))
  val flash  = Module(new NullDev())
  val serial = Module(new MockSerial(trace))

  val cycle = RegInit(0.U(32.W))
  when(io.ready) {
    if (trace) printf(p"Cycle $cycle\n")
    cycle := cycle + 1.U
  }

  core.io.dev       <> ioCtrl.io.core
  ioCtrl.io.ram     <> ram.io
  ioCtrl.io.flash   <> flash.io
  ioCtrl.io.serial  <> serial.io
  d                 <> core.d
  core.reset := !io.ready
  ioCtrl.reset := !io.ready
  TestUtil.bindRAM(io.ready, io.ram_init, ioCtrl.io.ram, ram.io)
}

class CoreTest(c: CoreTestModule, fname: String) extends PeekPokeTester(c) {
  reset()
  private val data = DataHelper.read_insts(fname)
  print("Loading data to RAM ...\n")
  TestUtil.loadRAM(this, c.io.ready, c.io.ram_init, data)
}

// x31 = 0xdead000 : Fail. reason = (char*)a0
// x31 = 0xcafe000 : Pass
class CoreTestNew(c: CoreTestModule, fname: String, max_cycles: Int) extends CoreTest(c, fname) {

  def isFinished(): Boolean = {
    val x31 = peek(c.d.reg(31)).toInt
    if(x31 == 0xdead000) {
      val ptr = (peek(c.d.reg(10)) - 0x80000000).toInt
      val reason = (ptr until ptr + 100)
        .map(i => peekAt(c.ram.mem, i).toChar)
        .takeWhile(x => x != 0)
        .mkString
      expect(false, reason)
      return true
    } else if(x31 == 0xcafe000) {
      return true
    }
    return false
  }

  import java.io._
  val traceFile = new PrintWriter(new File(fname + ".run"))

  import scala.util.control.Breaks
  val loop = new Breaks
  var cycle = 0
  loop.breakable {
    while(true) {
      if(isFinished()) {
        loop.break
      } else if(cycle >= max_cycles) {
        expect(false, s"Timeout: $max_cycles cycles")
        loop.break
      }
      step(1)
      cycle += 1
      // print PC
      if(peek(c.d.finish_pc.valid) == 1) {
        val pc = peek(c.d.finish_pc.bits).toLong
        traceFile.println("%08x".format(pc))
      }
    }
  }
  traceFile.close()
}

// [0x80001000] = 1 : Pass
//              > 1 : Fail. Code = value / 2
class RiscvTest(c: CoreTestModule, fname: String, max_cycles: Int) extends CoreTestNew(c, fname, max_cycles) {
  override def isFinished(): Boolean = {
    val v = peekAt(c.ram.mem, 0x1000).toInt
    val code = v / 2
    if(code != 0) {
      expect(false, s"Error code: $code")
    }
    return v != 0
  }
}

class CoreTestWithoutFw(c: CoreTestModule, fname: String) extends CoreTest(c, fname) {
  step(10)  // CPI=2, Skip 5 insts
  expect(c.d.reg(1), 20)

  step(10)
  expect(c.d.reg(1), 20)
  expect(c.d.reg(2), 30)

  step(10)
  expect(c.d.reg(1), 20)
  expect(c.d.reg(2), 30)
  expect(c.d.reg(3), 10)

  step(10)
  expect(c.d.reg(1), 20)
  expect(c.d.reg(2), 30)
  expect(c.d.reg(3), 10)
  expect(c.d.reg(4), "h_ffff_ffff".U)

  step(10)
  expect(c.d.reg(1), 20)
  expect(c.d.reg(2), 30)
  expect(c.d.reg(3), 10)
  expect(c.d.reg(4), "h_ffff_ffff".U)
  expect(c.d.reg(5), "h_ffff_fff5".U)

  step(10)
  expect(c.d.reg(1), "h_ffff_fff5".U)
  expect(c.d.reg(2), 30)
  expect(c.d.reg(3), 10)
  expect(c.d.reg(4), "h_ffff_ffff".U)
  expect(c.d.reg(5), "h_ffff_fff5".U)
}


class CoreTestWithFw(c: CoreTestModule, fname: String) extends CoreTest(c, fname) {
  step(5) // pipeline entry: IF2 + ID1 + EX1 + MEM1 = 5
  expect(c.d.reg(1), 20)
  step(2)
  expect(c.d.reg(1), 20)
  expect(c.d.reg(2), 30)
  step(2)
  expect(c.d.reg(1), 20)
  expect(c.d.reg(2), 30)
  expect(c.d.reg(3), 10)
  step(2)
  expect(c.d.reg(1), 20)
  expect(c.d.reg(2), 30)
  expect(c.d.reg(3), 10)
  expect(c.d.reg(4), "h_ffff_ffff".U)
  step(2)
  expect(c.d.reg(1), 20)
  expect(c.d.reg(2), 30)
  expect(c.d.reg(3), 10)
  expect(c.d.reg(4), "h_ffff_ffff".U)
  expect(c.d.reg(5), "h_ffff_fff5".U)
  step(2)
  expect(c.d.reg(1), "h_ffff_fff5".U)
  expect(c.d.reg(2), 30)
  expect(c.d.reg(3), 10)
  expect(c.d.reg(4), "h_ffff_ffff".U)
  expect(c.d.reg(5), "h_ffff_fff5".U)
}

class CoreTest6(c: CoreTestModule, fname: String) extends CoreTest(c, fname) {
  step(3)                                      // all before l1 has entered pipeline
  for (_ <- 0 until 0x40) {
    step(1)
    expect(c.d.ifpc, 0x10)                     // first instruction in l1
    step(3)
  }
  expect(c.d.ifpc, 0x1c)                       // leave l1
  step(1)
  expect(c.d.ifpc, 0x1c)                       // ram conflict.
  step(1)                                      // intermediate instructions entered pipeline
  for (i <- 0 until 0x40) {
    println("\n")
    step(1)
    expect(c.d.ifpc, 0x24)                 // first of l2
    if (i > 0) {
      expect(c.d.reg(29), "h_c0ff_ee00".U(32.W))
    }
    for (_ <- 0 until 4) {
      println("\n")
      step(1)
    }
    expect(c.d.reg(29), 0x0)                    // to understand, draw picture yourself
  }
  step(1)
  expect(c.d.ifpc, 0x34)
  step(1)
  expect(c.d.ifpc, 0x38)
  step(1)
  expect(c.d.ifpc, 0x34)
}


class CoreTester extends ChiselFlatSpec {
  val args = Array[String]("-fiwv") // output .vcd wave file
  "Core module fwno" should "pass test" in {
    iotesters.Driver.execute(args, () => new CoreTestModule()) {
      c => new CoreTestWithoutFw(c, "test_asm/test2.bin")
    } should be(true)
  }
  "Core module fwyes" should "pass test" in {
    iotesters.Driver.execute(args, () => new CoreTestModule()) {
      c => new CoreTestWithFw(c, "test_asm/test3.bin")
    } should be(true)
  }
  for((name, timeout, trace) <- Seq(
    ("test5", 50, true),
    ("test4", 250, true),
    ("hello", 500, false)
  )) {
    name should "pass test" in {
      iotesters.Driver.execute(args, () => new CoreTestModule(trace)) {
        c => new CoreTestNew(c, s"test_asm/$name.bin", timeout)
      } should be (true)
    }
  }
}

class MonitorTester extends ChiselFlatSpec {
  val args = Array[String]("-fiwv")
  "monitor" should "pass test" in {
    iotesters.Driver.execute(args, () => new CoreTestModule(false)) {
      c => new CoreTestNew(c, "test_asm/monitor/monitor.bin", 10000)
    } should be (true)
  }
}

class MMUTester extends ChiselFlatSpec {
  val args = Array[String]("-fiwv")
  "page table" should "pass test" in {
    iotesters.Driver.execute(args, () => new CoreTestModule(false)) {
      c => new CoreTestNew(c, "test_asm/pagetable.bin", 1500)
    } should be (true)
  }
}

class RiscvTester extends ChiselFlatSpec {
  val args = Array[String]("")
  val names = new File("test_asm/riscv-test/obj").listFiles().map(f => f.getName)
  for(name <- names) {
    name should "pass test" in {
      iotesters.Driver.execute(args, () => new CoreTestModule(false)) {
        c => new RiscvTest(c, s"test_asm/riscv-test/$name.bin", 1000)
      } should be (true)
    }
  }
}

// runMain core_.Repl
object Repl extends App {
  iotesters.Driver.executeFirrtlRepl(args, () => new CoreTestModule)
}
