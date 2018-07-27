package core_.mmu

import chisel3._
import chisel3.util._

class TLBQuery extends Bundle {
  val req = Input(Valid(new PN))
  val rsp = Output(Valid(new PN))
}

class TLBModify extends Bundle {
  val mode = Input(UInt(2.W))
  val vpn = Input(new PN)
  val ppn = Input(new PN)    // Only used when insert
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
    val vpn = new PN
    val ppn = new PN
  }
  val entries = Mem(new TLBEntry, SIZE)
  // TODO: Change to direct mapping. Fully associative mapping makes compiling very slow.

  // Debug
//  for(i <- 0 until SIZE)
//    printf("%x: %x 0x%x->0x%x\n", i.U, entries(i).valid, entries(i).vpn.asUInt, entries(i).ppn.asUInt)

  // Reset
  when(reset.toBool || io.modify.mode === TLBOp.Clear) {
    for(i <- 0 until SIZE) {
      entries(i).valid := false.B
    }
  }

  // Handle query
  when(reset.toBool) {
    io.query.rsp := io.query.req
  }.otherwise {
    io.query.rsp.valid := io.query.req.valid && MuxLookup(io.query.req.bits.asUInt, false.B,
      (0 until SIZE).map(i => (entries(i).vpn.asUInt, entries(i).valid)))
    io.query.rsp.bits := MuxLookup(io.query.req.bits.asUInt, 0.U,
      (0 until SIZE).map(i => (entries(i).vpn.asUInt, entries(i).ppn.asUInt))).asTypeOf(new PN)
  }

  // Handle query2
  when(reset.toBool) {
    io.query2.rsp := io.query2.req
  }.otherwise {
    io.query2.rsp.valid := io.query2.req.valid && MuxLookup(io.query2.req.bits.asUInt, false.B,
      (0 until SIZE).map(i => (entries(i).vpn.asUInt, entries(i).valid)))
    io.query2.rsp.bits := MuxLookup(io.query2.req.bits.asUInt, 0.U,
      (0 until SIZE).map(i => (entries(i).vpn.asUInt, entries(i).ppn.asUInt))).asTypeOf(new PN)
  }

  // Handle modify, at rising edge

  val randId = RegInit(1.U(SIZE_LOG2.W))
  randId := randId + 1.U

  // + insert
  val nextFreeId = MuxLookup(false.B, randId,
    (0 until SIZE).map(i => (entries(i).valid, i.U)))
  val insertId = MuxLookup(io.modify.vpn.asUInt, nextFreeId,
    (0 until SIZE).map(i => (entries(i).vpn.asUInt, i.U)))
  when(io.modify.mode === TLBOp.Insert) {
    val entry = entries(insertId)
    entry.valid := true.B
    entry.vpn := io.modify.vpn
    entry.ppn := io.modify.ppn
  }

  // - remove
  when(io.modify.mode === TLBOp.Remove) {
    for(i <- 0 until SIZE) {
      when(entries(i).vpn.asUInt === io.modify.vpn.asUInt) {
        entries(i).valid := false.B
      }
    }
  }
}
