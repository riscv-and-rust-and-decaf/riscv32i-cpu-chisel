import chisel3._
import chisel3.util._
import bundles._
import OptCode._

class EX extends Module {
  val io = IO(new Bundle {
    val _ID       = Flipped(new ID_EX())
    val _MEM      = new EX_MEM()
    val idWrRegOp = Input(new WrRegOp())
    val wrRegOp   = Output(new WrRegOp())
  })

  val a   = RegInit(0.U(32.W))
  val b   = RegInit(0.U(32.W))
  val opt = RegInit(OptCode.ADD)

  a := io._ID.oprd1
  b := io._ID.oprd2
  opt := io._ID.opt

  val low5 = b(4, 0)

  // NOTICE: SLL,SRL,SRA only use lower 5 bits of b
  val aluRes = MuxLookup(opt,
    a + b,
    Seq(
      ADD -> (a + b),
      SUB -> (a - b),
      SLT -> Mux(a.asSInt < b.asSInt, 1.U, 0.U),
      SLTU -> Mux(a < b, 1.U, 0.U),
      XOR -> (a ^ b),
      OR -> (a | b),
      AND -> (a & b),
      SLL -> (a << low5),
      SRL -> (a >> low5),
      SRA -> (a.asSInt >> low5).asUInt
      /*
      LW   -> (a + b),
      LB   -> (a + b),
      LH   -> (a + b),
      LBU  -> (a + b),
      LHU  -> (a + b),

      SB   -> ()
      */
      //not necessary, all rest (a+b)
    )
  )
  io._MEM.alu_out := aluRes

  val wregAddr = RegInit(0.U(5.W))
  wregAddr := io.idWrRegOp.addr
  io.wrRegOp.addr := wregAddr
  io.wrRegOp.data := aluRes
  io.wrRegOp.rdy := (opt & OptCode.LW) =/= OptCode.LW

  io._MEM.opt := opt
  val store_data = RegInit(0.U(32.W))
  store_data := io._ID.store_data
  io._MEM.store_data := store_data
}
