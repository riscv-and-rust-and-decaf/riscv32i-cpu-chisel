package core_.mmu

import chisel3._
import chisel3.util._
import core_.{RAMMode, RAMOp}

/// Virtual Page Number
class PN extends Bundle {
  val p2 = UInt(10.W)
  val p1 = UInt(10.W)

  def toAddr[T <: Bits](offset: T): UInt = p2 ## p1 ## offset(11, 0)
}

object PN {
  val ZERO = 0.U(20.W).asTypeOf(new PN)
  def fromAddr[T <: Bits](addr: T): PN = addr(31, 12).asTypeOf(new PN)
}

// Page Table Entry
class PTE extends Bundle {
  val zero    = UInt(2.W)
  val ppn     = new PN
  val reserve = UInt(2.W)

  val D = Bool()
  val A = Bool()
  val G = Bool()
  val U = Bool()
  val X = Bool()
  val W = Bool()
  val R = Bool()
  val V = Bool()

  def isPDE: Bool = V && !R && !W
  def isLeaf: Bool = V && (R || W)
}

object PTE {
  val ZERO = 0.U(32.W).asTypeOf(new PTE)
}

/// Translate VPN to PPN by reading page table at memory
class PTW extends Module {
  val io = IO(new Bundle {
    val set_root = Input(Valid(new PN()))
    val req = DeqIO(new PN()) // Request
    val rsp = EnqIO(Valid(new PTE())) // Response
    val mem = new RAMOp() // Memory access
  })

  val sIdle :: sWait2 :: sWait1 :: Nil = Enum(3)

  val status   = RegInit(sIdle)
  val root_ppn = RegInit(PN.ZERO)

  val pte      = io.mem.rdata.asTypeOf(new PTE)

  // Reset root PPN
  when(io.set_root.valid) {
    root_ppn := io.set_root.bits
    status := sIdle
  }

  io.req.ready := status === sIdle

  // Default output
  io.mem.mode := RAMMode.NOP
  io.mem.addr := 0.U
  io.mem.wdata := 0.U
  io.rsp.valid := false.B
  io.rsp.bits.valid := false.B
  io.rsp.bits.bits := PTE.ZERO

  switch(status) {
    is(sIdle) {
      when(io.req.valid) {
        // Memory access for P2
        io.mem.mode := RAMMode.LW
        io.mem.addr := root_ppn.toAddr(io.req.bits.p2 << 2.U)
        status := sWait2
      }
    }
    is(sWait2) {
      when(io.mem.ok) {
        // Check PTE
        when(!pte.V) { // error, response
          io.rsp.valid := true.B
          io.rsp.bits.valid := false.B
          when(io.rsp.ready) { // ack
            status := sIdle
          }
        }.elsewhen(!pte.isPDE) { // Response huge page
          io.rsp.valid := true.B
          io.rsp.bits.valid := !pte.ppn.p1.orR // Test error? : Not 4M aligned
          io.rsp.bits.bits := pte
          when(io.rsp.ready) { // ack
            status := sIdle
          }
        }.otherwise {
          // Memory access for P1
          io.mem.mode := RAMMode.LW
          io.mem.addr := pte.ppn.toAddr(io.req.bits.p1 << 2.U)
          status := sWait1
        }
      }
    }
    is(sWait1) {
      when(io.mem.ok) { // Response
        io.rsp.valid := true.B
        io.rsp.bits.valid := pte.isLeaf
        io.rsp.bits.bits := pte
        when(io.rsp.ready) { // ack
          status := sIdle
        }
      }
    }
  }
}
