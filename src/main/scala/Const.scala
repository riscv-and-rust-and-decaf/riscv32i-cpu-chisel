import chisel3._

object Const {
  val PC_INIT = 0.U(32.W)
  val NOP_INST = "h_0000_0013".U

  val MMU_MODE_NOP = "b0000".U
  val MMU_MODE_LW  = "b1000".U

  val MMU_MODE_SW  = "b0100".U
}

object OptCode {
  val ADD = 0.U(5.W)
  val SUB = 1.U(5.W)

  val LW  = 24.U(5.W) // 11000 
  val LH  = 26.U(5.W) // 11010
  val LHU = 27.U(5.W) // 11011
  val LB  = 28.U(5.W) // 11100
  val LBU = 29.U(5.W) // 11101

  val _isL = 24.U(5.W) // 11000

  val SW = "b10100".U
  val SH = "b10101".U
  val SB = "b10110".U

}
