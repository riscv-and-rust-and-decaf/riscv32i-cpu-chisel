import chisel3._
import core_._
import devices.IOManager

class ChiselTop extends Module {
  val io = IO(new Bundle {
    val ram  = new RAMOp
    val flash = new RAMOp
    val serial = new RAMOp
    // If debug is unused, the synthesiser will automatically remove
    //  redundant circuits.
    // So just let it be here.
    val debug = new CoreState
  })

  val core    = Module(new Core())
  val ioCtrl  = Module(new IOManager())

  core.io.dev <> ioCtrl.io.core
  core.io.debug <> io.debug
  io.ram      <> ioCtrl.io.ram
  io.flash    <> ioCtrl.io.flash
  io.serial   <> ioCtrl.io.serial
}

object Gen {
  def main(args: Array[String]) : Unit = {
    chisel3.Driver.execute(args, () => new ChiselTop())
  }
}
