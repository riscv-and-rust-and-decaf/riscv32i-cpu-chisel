import chisel3._
import chisel3.util._
import bundles._
import OptCode._

class EX extends Module {
  val io = IO(new Bundle {
    val _ID  = Flipped(new ID_EX())
    val _MEM = new EX_MEM()
  })

  val a = Wire(UInt(32.W))
  val b = Wire(UInt(32.W))
  val low5 = Wire(UInt(5.W))

  a := io._ID.oprd1
  b := io._ID.oprd2
  low5 := b(4, 0)
  
  //NOTICE: SLL,SRL,SRA only use lower 5 bits of b
  io._MEM.alu_out := MuxLookup(io._ID.opt,
    (a + b),
    Seq(
      ADD  -> (a + b),
      SUB  -> (a - b),
      SLT ->  Mux(a.asSInt < b.asSInt, 1.U, 0.U),
      SLTU -> Mux(a < b, 1.U, 0.U),
      XOR  -> (a ^ b),
      OR   -> (a | b),
      AND  -> (a & b),
      SLL  -> (a << low5),
      SRL  -> (a >> low5),
      SRA  -> (a.asSInt >> low5).asUInt
/*
      LW   -> (a + b),
      LB   -> (a + b),
      LH   -> (a + b),
      LBU  -> (a + b),
      LHU  -> (a + b),

      SB   -> ()
*/ //not necessary, all rest (a+b)
    )
  )


  io._MEM.reg_w_add := io._ID.reg_w_add
  io._MEM.opt       := io._ID.opt

  io._MEM.store_data := io._ID.store_data
} 

