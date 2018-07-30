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
    val root = Input(new PN())
    val req  = DeqIO(new PN()) // Request
    val rsp = EnqIO(new PTE()) // Response
    val mem = new RAMOp() // Memory access
  })

  val sIdle :: sWait2 :: sWait1 :: Nil = Enum(3)
  val status = RegInit(sIdle)
  val req    = RegInit(0.U.asTypeOf(new PN))
  val pte    = io.mem.rdata.asTypeOf(new PTE)
  val mem_ok = io.mem.ok

  io.req.ready := status === sIdle

  // Default output
  io.mem.mode := RAMMode.NOP
  io.mem.addr := 0.U
  io.mem.wdata := 0.U
  io.rsp.valid := false.B
  io.rsp.bits := 0.U.asTypeOf(new PTE)

  switch(status) {
    is(sIdle) {
      when(io.req.valid) {
        // Memory access for P2
        io.mem.mode := RAMMode.LW
        io.mem.addr := io.root.toAddr(io.req.bits.p2 << 2.U)
        req := io.req.bits
        status := sWait2
      }
    }
    is(sWait2) {
      when(mem_ok) {
        // Check PTE
        when(!pte.V) { // error, response
          io.rsp.valid := true.B
          when(io.rsp.ready) { // ack
            status := sIdle
          }
        }.elsewhen(!pte.isPDE) { // Response huge page
          io.rsp.valid := true.B
          io.rsp.bits := pte
          io.rsp.bits.ppn.p1 := req.p1
          io.rsp.bits.V := !pte.ppn.p1.orR // Test error? : Not 4M aligned
          when(io.rsp.ready) { // ack
            status := sIdle
          }
        }.otherwise {
          // Memory access for P1
          io.mem.mode := RAMMode.LW
          io.mem.addr := pte.ppn.toAddr(req.p1 << 2.U)
          status := sWait1
        }
      }
    }
    is(sWait1) {
      when(mem_ok) { // Response
        io.rsp.valid := true.B
        io.rsp.bits := pte
        io.rsp.bits.V := pte.isLeaf
        when(io.rsp.ready) { // ack
          status := sIdle
        }
      }
    }
  }
}
