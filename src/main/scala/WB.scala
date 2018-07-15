import chisel3._
import chisel3.util._
import bundles._

import Const._


class WB extends Module {
  val io = IO(new Bundle {
    val memWrRegOp = Input(new WrRegOp())
    val wrRegOp = Output(new WrRegOp())
  })

  val wregAddr = RegInit(0.U(32.W))
  wregAddr := io.memWrRegOp.addr
  io.wrRegOp.addr := wregAddr

  val wregData = RegInit(0.U(32.W))
  wregData := io.memWrRegOp.data
  io.wrRegOp.data := wregData

  val wregRdy = RegInit(false.B)
  wregRdy := io.memWrRegOp.rdy
  io.wrRegOp.rdy := wregRdy
}
