package core_.mmu

import chisel3._
import chisel3.util._
import core_._
import devices.NullDev

class MMU extends Module {
  val io = IO(new Bundle {
    val iff = Flipped(new MMUOp())
    val mem = Flipped(new MMUOp())
    val csr = Flipped(new CSR_MMU())

    val dev = new Core_IO
  })

  val ptw = Module(new PTW())
  val tlb = Module(new TLB(4))

  // Enable MMU?
  tlb.reset := !io.csr.satp(31)
  ptw.io.root := io.csr.satp(19, 0).asTypeOf(new PN)

  val null_device = Module(new NullDev)
  ptw.io.mem <> null_device.io

  // TLB query path
  def bindTLB(vop: MMUOp, pop: RAMOp, tlb: TLBQuery): Unit = {
    // vop.in -> TLB.in
    tlb.req.valid := vop.mode =/= RAMMode.NOP
    tlb.req.bits := PN.fromAddr(vop.addr)
    // vop.in + TLB.out -> pop.in
    pop.mode := vop.mode
    pop.wdata := vop.wdata
    pop.addr := tlb.rsp.bits.ppn.toAddr(vop.addr)
    // pop.out -> vop.out
    vop.ok := pop.ok && tlb.rsp.valid
    vop.rdata := pop.rdata
    // If TLB miss, lend IO port to PTW
    val miss = tlb.req.valid && !tlb.rsp.valid
    when(miss) {
      // PTW.mem <-> pop
      ptw.io.mem <> pop
      vop.ok := false.B
    }.elsewhen(vop.pageFault) {
      pop.mode := 0.U
      pop.wdata := 0.U
      pop.addr := 0.U
      vop.ok := true.B
    }
  }
  bindTLB(io.iff, io.dev.if_, tlb.io.query)
  bindTLB(io.mem, io.dev.mem, tlb.io.query2)

  // Handle page fault
  { // IF
    val rsp = tlb.io.query.rsp
    io.iff.pageFault := rsp.valid && io.iff.mode =/= RAMMode.NOP && (
      // Not (valid and executable)
      !(rsp.bits.V && rsp.bits.X) ||
      // User restriction
      io.csr.priv === Priv.U && !rsp.bits.U
    )
  }
  { // MEM
    val rsp = tlb.io.query2.rsp
    io.mem.pageFault := rsp.valid && io.mem.mode =/= RAMMode.NOP && (
      // Read
      RAMMode.isRead(io.mem.mode) && !(rsp.bits.V && (rsp.bits.R || rsp.bits.X && io.csr.mxr)) ||
      // Write
      RAMMode.isWrite(io.mem.mode) && !(rsp.bits.V && rsp.bits.W) ||
      // User restriction
      io.csr.priv === Priv.U && !rsp.bits.U ||
      // Access user in supervisor
      io.csr.priv === Priv.S && !io.csr.sum && rsp.bits.U
    )
  }

  // Detect TLB miss and refill
  //
  // Status:
  // - Ready:
  //   - 2 TLB queries is (always) open
  //   - If TLB miss is detected, send request to PTW, then go to Walking.
  // - Walking:
  //   - Wait for PTW response, then refill TLB.
  val sReady :: sWalking :: Nil = Enum(2)
  val status = RegInit(sReady)
  val ptw_vpn = RegInit(0.U.asTypeOf(new PN))

  // Default output
  ptw.io.req.valid := false.B
  ptw.io.req.bits := 0.U.asTypeOf(new PN)
  ptw.io.rsp.ready := false.B
  tlb.io.modify.mode := TLBOp.None
  tlb.io.modify.vpn := 0.U.asTypeOf(new PN)
  tlb.io.modify.pte := 0.U.asTypeOf(new PTE)

  switch(status) {
    is(sReady) {
      def detect(query: TLBQuery): Unit = {
        val miss = query.req.valid && !query.rsp.valid
        when(miss) {
          ptw.io.req.valid := true.B
          ptw.io.req.bits := query.req.bits
          when(ptw.io.req.ready) {
            status := sWalking
            ptw_vpn := query.req.bits
          }
        }
      }
      detect(tlb.io.query)
      detect(tlb.io.query2)
    }
    is(sWalking) {
      ptw.io.rsp.ready := true.B
      when(ptw.io.rsp.valid) {
        tlb.io.modify.mode := TLBOp.Insert
        tlb.io.modify.vpn := ptw_vpn
        tlb.io.modify.pte := ptw.io.rsp.bits
        status := sReady
      }
    }
  }
}
