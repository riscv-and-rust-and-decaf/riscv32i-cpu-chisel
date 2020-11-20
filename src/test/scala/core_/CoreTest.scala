package core_

import java.io.File

import chisel3._
import chisel3.iotesters._
import devices._

class CoreTestModule(ramDataFile: String, trace: Boolean = false, serialInputs: String = "\0") extends Module {
  val io = IO(new Bundle {
    val debug    = new CoreState()
    val d_ram    = new RAMOp_Output()
  })
  val d  = io.debug

  val core   = Module(new Core())
  val ioCtrl = Module(new IOManager())
  val ram    = Module(new MockRam(ramDataFile, trace))
  val flash  = Module(new NullDev())
  val serial = Module(new MockSerial(serialInputs, trace))

  val cycle = RegInit(0.U(32.W))
  if (trace) printf(p"Cycle $cycle\n")
  cycle := cycle + 1.U

  core.io.dev       <> ioCtrl.io.core
  ioCtrl.io.ram     <> ram.io
  ioCtrl.io.flash   <> flash.io
  ioCtrl.io.serial  <> serial.io
  d                 <> core.d
  io.d_ram.addr := ram.io.addr
  io.d_ram.mode := ram.io.mode
  io.d_ram.wdata := ram.io.wdata
}

class CoreTest(c: CoreTestModule) extends PeekPokeTester(c) {
  reset()
}

// x31 = 0xdead000 : Fail. reason = (char*)a0
// x31 = 0xcafe000 : Pass
class CoreTestNew(c: CoreTestModule, max_cycles: Int) extends CoreTest(c) {

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
  // val traceFile = new PrintWriter(new File(fname + ".run"))

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
        // traceFile.println("%08x".format(pc))
      }
    }
  }
  // traceFile.close()
}

// [0x80001000] = 1 : Pass
//              > 1 : Fail. Code = value / 2
class RiscvTest(c: CoreTestModule, max_cycles: Int) extends CoreTestNew(c, max_cycles) {
  override def isFinished(): Boolean = {
    // Workaround for verilator backend:
    //    val v = peekAt(c.ram.mem, 0x1000).toInt
    val addr = peek(c.io.d_ram.addr).toLong
    val mode = peek(c.io.d_ram.mode).toInt
    if(addr != 0x80001000L || mode < 8) {
      return false
    }
    val v = peek(c.io.d_ram.wdata).toInt
    val code = v / 2
    if(code != 0) {
      expect(false, s"Error code: $code")
    }
    return v != 0
  }
}

class CoreTestWithoutFw(c: CoreTestModule) extends CoreTest(c) {
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


class CoreTestWithFw(c: CoreTestModule) extends CoreTest(c) {
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

class CoreTest6(c: CoreTestModule) extends CoreTest(c) {
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
    iotesters.Driver.execute(args, () => new CoreTestModule("test_asm/test2.hex")) {
      c => new CoreTestWithoutFw(c)
    } should be(true)
  }
  "Core module fwyes" should "pass test" in {
    iotesters.Driver.execute(args, () => new CoreTestModule("test_asm/test3.hex")) {
      c => new CoreTestWithFw(c)
    } should be(true)
  }
  for((name, timeout) <- Seq(
    ("test5", 50),
    ("test4", 250),
    ("hello", 500)
  )) {
    name should "pass test" in {
      iotesters.Driver.execute(args, () => new CoreTestModule(s"test_asm/$name.hex")) {
        c => new CoreTestNew(c, timeout)
      } should be (true)
    }
  }
}

class MonitorTester extends ChiselFlatSpec {
  val args = Array[String]("-fiwv", "-tbn", "verilator")
  "monitor" should "pass test" in {
    val serialInputs = "G%c%c%c%cR".format(0x00, 0x10, 0x00, 0x80)
    iotesters.Driver.execute(args, () => new CoreTestModule("test_asm/kernel.hex", false, serialInputs)) {
      c => new CoreTestNew(c, 15000)
    } should be (true)
  }
}

class MMUTester extends ChiselFlatSpec {
  val args = Array[String]("-fiwv", "-tbn", "verilator")
  "page table" should "pass test" in {
    iotesters.Driver.execute(args, () => new CoreTestModule("test_asm/pagetable.hex")) {
      c => new CoreTestNew(c, 1500)
    } should be (true)
  }
}

class RiscvTester extends ChiselFlatSpec {
  val args = Array[String]("-fiwv", "-tbn", "verilator")
  val names = new File("test_asm/riscv-test/obj").listFiles().map(f => f.getName)
//  Not passed: rv32mi-p-illegal, rv32si-p-dirty, ?
  for(name <- names) {
    name should "pass test" in {
      iotesters.Driver.execute(args, () => new CoreTestModule(s"test_asm/riscv-test/$name.hex")) {
        c => new RiscvTest(c, 30000)
      } should be (true)
    }
  }
}

// runMain core_.Repl
object Repl extends App {
  iotesters.Driver.executeFirrtlRepl(args, () => new CoreTestModule(""))
}
