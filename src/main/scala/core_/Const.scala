package core_

import chisel3._
import chisel3.util._


object Const {
  val PC_INIT = "h_8000_0000".U(32.W)
  val NOP_INST = "h_0000_0013".U(32.W)
}

object RAMMode {
  val NOP = "b0000".U
  val LW  = "b0001".U
  val LH  = "b0010".U
  val LHU = "b0011".U
  val LB  = "b0100".U
  val LBU = "b0101".U
  val SW  = "b1001".U
  val SH  = "b1010".U
  val SB  = "b1100".U

  def isRead(x: UInt): Bool  = !x(3) && x.orR
  def isWrite(x: UInt): Bool = x(3)
  def is32(x: UInt): Bool    = x(2,0) === "b001".U
  def is16(x: UInt): Bool    = x(1)
  def is8(x: UInt): Bool     = x(2)
}

object CSRMODE {
  val NOP = 0.U(2.W)
  val RW = 1.U(2.W)
  val RS = 2.U(2.W)
  val RC = 3.U(2.W)
}

object OptCode {
  val ADD = 0.U(5.W)
  val SUB = 1.U(5.W)
  val SLT = 2.U(5.W)
  val SLTU = 3.U(5.W)
  val XOR = 4.U(5.W)
  val OR  = 5.U(5.W)
  val AND = 6.U(5.W)
  val SLL = 7.U(5.W)
  val SRL = 8.U(5.W)
  val SRA = 9.U(5.W)

  val JALR = 10.U(5.W)

  val MUL = 11.U(5.W)
  val MULH = 12.U(5.W)
  val MULHSU = 13.U(5.W)
  val MULHU = 14.U(5.W)
  val DIV = 15.U(5.W)
  val DIVU = 16.U(5.W)
  val REM = 17.U(5.W)
  val REMU = 18.U(5.W)

  val LW  = 24.U(5.W) // 11000
  val LH  = 26.U(5.W) // 11010
  val LHU = 27.U(5.W) // 11011
  val LB  = 28.U(5.W) // 11100
  val LBU = 29.U(5.W) // 11101

  val SW = "b10100".U // 20
  val SH = "b10101".U
  val SB = "b10110".U
}

object BType {
  //          0 < = > SInt?
  val BEQ = "b0_0100".U
  val BNE = "b0_1010".U
  val BLT = "b0_1000".U
  val BGE = "b0_0110".U
  val BLTU ="b0_1001".U
  val BGEU ="b0_0111".U
}

object UType {
  val LUI   = "b0000_0".U
  val AUIPC = "b0000_1".U
}

object Cause {
  val Interrupt = 1.U(1.W)
  val Exception = 0.U(1.W)

  // software interrupt
  val USI = 0.U
  val SSI = 1.U
  val HSI = 2.U
  val MSI = 3.U

  // timer interrupt
  val UTI = 4.U
  val STI = 5.U
  val HTI = 6.U
  val MTI = 7.U

  // External interrupt
  val UEI = 8.U
  val SEI = 9.U
  val HEI = 10.U
  val MEI = 11.U

  val InstAddressMisaligned  = 0.U
  val InstAccessFault        = 1.U
  val IllegalInstruction     = 2.U
  val BreakPoint             = 3.U
  val LoadAddressMisaligned  = 4.U
  val LoadAccessFault        = 5.U
  val StoreAddressMisaligned = 6.U
  val StoreAccessFault       = 7.U
  val ECallU                 = 8.U
  val ECallS                 = 9.U
  val ECallM                 = 11.U
  val InstPageFault          = 12.U
  val LoadPageFault          = 13.U
  val StorePageFault         = 15.U

  // Internal use
  val URet      = 16.U
  val SRet      = 17.U
  val MRet      = 19.U
  val SFenceOne = 20.U
  val SFenceAll = 21.U

  // Helper
  def ecallX(prv: UInt) = 8.U | prv(1, 0)
  def xRet(prv: UInt) = 16.U | prv(1, 0)
  def isRet(cause: UInt) = cause(31,2) === 16.U(32.W)(31,2)
  def retX(cause: UInt) = cause(1,0)
}

object InstType {
  // R,I,S,B,U,J
  val BAD = 0.U(4.W)
  val R = 1.U(4.W)
  val I = 2.U(4.W)
  val S = 3.U(4.W)
  val B = 4.U(4.W)
  val U = 5.U(4.W)
  val J = 6.U(4.W)

  val SYS = 7.U(4.W) // CSR or ECALL
  val FENCE = 8.U(4.W)
}

object Priv {
  val M = 3.U(2.W)
  val S = 1.U(2.W)
  val U = 0.U(2.W)
}


object DecTable {
  // default decode signals
  val defaultDec =
                   List(InstType.BAD, OptCode.ADD)

  val decMap = Array(
    Insts.ADDI  -> List(InstType.I, OptCode.ADD),
    Insts.SLTI  -> List(InstType.I, OptCode.SLT),
    Insts.SLTIU -> List(InstType.I, OptCode.SLTU),
    Insts.XORI  -> List(InstType.I, OptCode.XOR),
    Insts.ORI   -> List(InstType.I, OptCode.OR),
    Insts.ANDI  -> List(InstType.I, OptCode.AND),
    Insts.SLLI  -> List(InstType.I, OptCode.SLL),
    Insts.SRLI  -> List(InstType.I, OptCode.SRL),
    Insts.SRAI  -> List(InstType.I, OptCode.SRA),

    Insts.ADD   -> List(InstType.R, OptCode.ADD),
    Insts.SUB   -> List(InstType.R, OptCode.SUB),
    Insts.SLL   -> List(InstType.R, OptCode.SLL),
    Insts.SLT   -> List(InstType.R, OptCode.SLT),
    Insts.SLTU  -> List(InstType.R, OptCode.SLTU),
    Insts.XOR   -> List(InstType.R, OptCode.XOR),
    Insts.SRL   -> List(InstType.R, OptCode.SRL),
    Insts.SRA   -> List(InstType.R, OptCode.SRA),
    Insts.OR    -> List(InstType.R, OptCode.OR),
    Insts.AND   -> List(InstType.R, OptCode.AND),

    Insts.LB    -> List(InstType.I, OptCode.LB),
    Insts.LH    -> List(InstType.I, OptCode.LH),
    Insts.LW    -> List(InstType.I, OptCode.LW),
    Insts.LBU   -> List(InstType.I, OptCode.LBU),
    Insts.LHU   -> List(InstType.I, OptCode.LHU),

    Insts.SB    -> List(InstType.S, OptCode.SB),
    Insts.SH    -> List(InstType.S, OptCode.SH),
    Insts.SW    -> List(InstType.S, OptCode.SW),

    Insts.BEQ   -> List(InstType.B, BType.BEQ),
    Insts.BNE   -> List(InstType.B, BType.BNE),
    Insts.BLT   -> List(InstType.B, BType.BLT),
    Insts.BGE   -> List(InstType.B, BType.BGE),
    Insts.BLTU  -> List(InstType.B, BType.BLTU),
    Insts.BGEU  -> List(InstType.B, BType.BGEU),

    Insts.LUI   -> List(InstType.U, UType.LUI),
    Insts.AUIPC -> List(InstType.U, UType.AUIPC),

    Insts.JAL   -> List(InstType.J, OptCode.ADD),
    Insts.JALR  -> List(InstType.I, OptCode.JALR),

    Insts.SYS   -> List(InstType.SYS, OptCode.ADD), //Maybe ecall maybe csr
    
    Insts.FENCE -> List(InstType.FENCE, OptCode.ADD),
    Insts.FENCE_I -> List(InstType.FENCE, OptCode.ADD),

    Insts.MUL  -> List(InstType.R, OptCode.MUL),
    Insts.MULH -> List(InstType.R, OptCode.MULH),
    Insts.MULHSU -> List(InstType.R, OptCode.MULHSU),
    Insts.DIV  -> List(InstType.R, OptCode.DIV),
    Insts.DIVU  -> List(InstType.R, OptCode.DIVU),
    Insts.REM   -> List(InstType.R, OptCode.REM),
    Insts.REMU  -> List(InstType.R, OptCode.REMU)
  )

  // fields
  val TYPE = 0
  val OPT = 1
}


object Insts { // idea from mini riscv
//  // Loads
  def LB     = BitPat("b?????????????????000?????0000011")
  def LH     = BitPat("b?????????????????001?????0000011")
  def LW     = BitPat("b?????????????????010?????0000011")
  def LBU    = BitPat("b?????????????????100?????0000011")
  def LHU    = BitPat("b?????????????????101?????0000011")
//  // Stores
  def SB     = BitPat("b?????????????????000?????0100011")
  def SH     = BitPat("b?????????????????001?????0100011")
  def SW     = BitPat("b?????????????????010?????0100011")
//  // Shifts
  def SLL    = BitPat("b0000000??????????001?????0110011")
  def SLLI   = BitPat("b0000000??????????001?????0010011")
  def SRL    = BitPat("b0000000??????????101?????0110011")
  def SRLI   = BitPat("b0000000??????????101?????0010011")
  def SRA    = BitPat("b0100000??????????101?????0110011")
  def SRAI   = BitPat("b0100000??????????101?????0010011")
//  // Arithmetic
  def ADD    = BitPat("b0000000??????????000?????0110011")
  def ADDI   = BitPat("b?????????????????000?????0010011")
  def SUB    = BitPat("b0100000??????????000?????0110011")
  def LUI    = BitPat("b?????????????????????????0110111")
  def AUIPC  = BitPat("b?????????????????????????0010111")
//  // Logical
  def XOR    = BitPat("b0000000??????????100?????0110011")
  def XORI   = BitPat("b?????????????????100?????0010011")
  def OR     = BitPat("b0000000??????????110?????0110011")
  def ORI    = BitPat("b?????????????????110?????0010011")
  def AND    = BitPat("b0000000??????????111?????0110011")
  def ANDI   = BitPat("b?????????????????111?????0010011")
//  // Compare
  def SLT    = BitPat("b0000000??????????010?????0110011")
  def SLTI   = BitPat("b?????????????????010?????0010011")
  def SLTU   = BitPat("b0000000??????????011?????0110011")
  def SLTIU  = BitPat("b?????????????????011?????0010011")
//  // Branches
  def BEQ    = BitPat("b?????????????????000?????1100011")
  def BNE    = BitPat("b?????????????????001?????1100011")
  def BLT    = BitPat("b?????????????????100?????1100011")
  def BGE    = BitPat("b?????????????????101?????1100011")
  def BLTU   = BitPat("b?????????????????110?????1100011")
  def BGEU   = BitPat("b?????????????????111?????1100011")
//  // Jump & Link
  def JAL    = BitPat("b?????????????????????????1101111")
  def JALR   = BitPat("b?????????????????000?????1100111")
// SYS
  def SYS    = BitPat("b?????????????????????????1110011")
// FENCE
  def FENCE  = BitPat("b0000????????00000000000000001111")
  def FENCE_I= BitPat("b00000000000000000001000000001111")

// M-extension
  def MUL    = BitPat("b0000001??????????000?????0110011")
  def MULH   = BitPat("b0000001??????????001?????0110011")
  def MULHSU = BitPat("b0000001??????????010?????0110011")
  def MULHU  = BitPat("b0000001??????????011?????0110011")
  def DIV    = BitPat("b0000001??????????000?????0110011")
  def DIVU   = BitPat("b0000001??????????000?????0110011")
  def REM    = BitPat("b0000001??????????000?????0110011")
  def REMU   = BitPat("b0000001??????????000?????0110011")

}

object SYS_INST_P2 { // bits(24:20)
  def ECALL  = "b00000".U
  def EBREAK = "b00001".U
  def xRET   = "b00010".U
/*
  def URET   = "b0000000000100000000000000".U
  def SRET   = "b0001000000100000000000000".U
  def MRET   = "b0011000000100000000000000".U
*/
}

object SYS_INST_P1 { //bits(31:25)
  def SFENCE_VMA = "b0001001".U

}
