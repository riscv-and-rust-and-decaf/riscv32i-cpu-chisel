package core_.mmu

import chisel3._
import chisel3.util._

class TLBQuery extends Bundle {
  val req = Input(Valid(new PN))
  val rsp = Output(Valid(new PTE))
  def miss = req.valid && !rsp.valid
}

class TLBModify extends Bundle {
  val mode = Input(UInt(2.W))
  val vpn  = Input(new PN)
  val pte  = Input(new PTE)    // Only used when insert
}

object TLBOp {
  val None   = 0.U(2.W)
  val Insert = 1.U(2.W)   // Insert (vpn, ppn)
  val Remove = 2.U(2.W)   // Remove vpn
  val Clear  = 3.U(2.W)   // Remove all
}

class TLB(val SIZE_LOG2: Int) extends Module {
  val io = IO(new Bundle {
    val query = new TLBQuery    // | Response at the same cycle
    val query2 = new TLBQuery   // | If reset = 1, rsp = req
    val modify = new TLBModify  // Do at rising edge
  })
  val SIZE = 1 << SIZE_LOG2

  class TLBEntry extends Bundle {
    val valid = Bool()
    val vpn   = new PN
    val pte   = new PTE
  }
  val entries = Mem(SIZE, new TLBEntry)

  // Debug
//  for(i <- 0 until SIZE)
//    printf("%x: %x 0x%x->0x%x\n", i.U, entries(i).valid, entries(i).vpn.asUInt, entries(i).ppn.asUInt)

  // Reset
  when(reset.toBool || io.modify.mode === TLBOp.Clear) {
    for(i <- 0 until SIZE) {
      entries(i).valid := false.B
    }
  }

  def toIndex(vpn: PN) = (vpn.p2 ^ vpn.p1)(SIZE_LOG2-1, 0)

  // Handle query
  def handleQuery(q: TLBQuery): Unit = {
    when(reset.toBool) {
      q.rsp.valid := q.req.valid
      q.rsp.bits := 0.U.asTypeOf(new PTE)
      q.rsp.bits.ppn := q.req.bits
      q.rsp.bits.V := true.B
      q.rsp.bits.X := true.B
      q.rsp.bits.R := true.B
      q.rsp.bits.W := true.B
      q.rsp.bits.U := true.B
    }.otherwise {
      val id = toIndex(q.req.bits)
      val entry = entries(id)
      q.rsp.valid := q.req.valid && entry.valid && entry.vpn.asUInt === q.req.bits.asUInt
      q.rsp.bits := entry.pte
    }
  }
  handleQuery(io.query)
  handleQuery(io.query2)

  // Handle modify, at rising edge

  // + insert
  val id = toIndex(io.modify.vpn)
  when(io.modify.mode === TLBOp.Insert) {
    // WARNING: Don't define `entry = entries(id)`
    //          then use `entry` as a l-value.
    //          It makes writing delay a cycle.
    entries(id).valid := true.B
    entries(id).vpn := io.modify.vpn
    entries(id).pte := io.modify.pte
  }

  // - remove
  when(io.modify.mode === TLBOp.Remove) {
    when(entries(id).vpn.asUInt === io.modify.vpn.asUInt) {
      entries(id).valid := false.B
    }
  }
}
