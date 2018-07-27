package core_.mmu

import chisel3._
import core_._

class MMU extends Module {
  val io = IO(new Bundle {
    val iff = Flipped(new RAMOp())
    val mem = Flipped(new RAMOp())

    val dev = new Core_IO
  })

  val tlb = Module(new TLB(4))
  // Now TLB is disabled
  tlb.reset := true.B

  def bindTLB(vop: RAMOp, pop: RAMOp, tlb: TLBQuery): Unit = {
    // vop.in -> TLB.in
    tlb.req.valid := vop.mode =/= RAMMode.NOP
    tlb.req.bits := PN.fromAddr(vop.addr)
    // vop.in + TLB.out -> pop.in
    pop.mode := vop.mode
    pop.wdata := vop.wdata
    pop.addr := tlb.rsp.bits.toAddr(vop.addr)
    // pop.out -> vop.out
    vop.ok := pop.ok && tlb.rsp.valid
    vop.rdata := pop.rdata
  }
  bindTLB(io.iff, io.dev.if_, tlb.io.query)
  bindTLB(io.mem, io.dev.mem, tlb.io.query2)

  tlb.io.modify.mode := TLBOp.None
  tlb.io.modify.vpn := 0.U.asTypeOf(new PN)
  tlb.io.modify.ppn := 0.U.asTypeOf(new PN)

  // TODO: Translate address
}
