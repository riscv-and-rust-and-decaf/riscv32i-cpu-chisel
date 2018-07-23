import chisel3._
import core_._
import devices.IOManager

class ChiselTop extends Module {
  val io = IO(new Bundle {
    val ram  = new RAMOp
    val flash = new RAMOp
    val serial = new RAMOp
  })

  val core    = Module(new Core())
  val ioCtrl  = Module(new IOManager())

  core.io.dev <> ioCtrl.io.core
  io.ram      <> ioCtrl.io.ram
  io.flash    <> ioCtrl.io.flash
  io.serial   <> ioCtrl.io.serial
}

object Gen {
  def main(args: Array[String]) : Unit = {
    chisel3.Driver.execute(args, () => new ChiselTop())
  }
}
