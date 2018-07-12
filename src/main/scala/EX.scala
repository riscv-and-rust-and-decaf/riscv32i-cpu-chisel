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

  a := io._ID.oprd1
  b := io._ID.oprd2

  io._MEM.alu_out := MuxLookup(io._ID.opt,
    0.U(32.W),
    Seq(
      ADD -> (a + b),
      SUB -> (a - b)
    )
  )


  io._MEM.reg_w_add := io._ID.reg_w_add
  io._MEM.opt       := io._ID.opt

  io._MEM.store_data := io._ID.store_data
} 

