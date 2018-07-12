import chisel3._

object Const {
  val PC_INIT = 0.U(32.W)
  val NOP_INST = "h_0000_0013".U

  val MMU_MODE_NOP = 0.U
  val MMU_MODE_LW = 1.U
  val MMU_MODE_SW = 2.U
}
