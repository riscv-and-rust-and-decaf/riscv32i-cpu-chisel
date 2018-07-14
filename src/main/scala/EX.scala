import chisel3._
import chisel3.util._
import bundles._
import OptCode._

class EX extends Module {
  val io = IO(new Bundle {
    val _ID  = Flipped(new ID_EX())
    val _MEM = new EX_MEM()
  })

  val a = Reg(UInt(32.W))
  a := io._ID.oprd1
  val b = Reg(UInt(32.W))
  b := io._ID.oprd2
  val opt = Reg(UInt())
  opt := io._ID.opt


  io._MEM.alu_out := MuxLookup(opt,
    0.U(32.W),
    Seq(
      ADD -> (a + b),
      SUB -> (a - b)
    )
  )

  val reg_w_add = Reg(UInt())
  reg_w_add := io._ID.reg_w_add
  io._MEM.reg_w_add := reg_w_add
  io._MEM.opt       := opt
  val store_data = Reg(UInt())
  store_data := io._ID.store_data
  io._MEM.store_data := store_data
} 

