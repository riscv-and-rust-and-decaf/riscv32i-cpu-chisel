// still a dummy mmu
// TODO: change to real MMU when we add load/store instructions
import chisel3._
import bundles._


class IMemMMU extends Module {
  val io = IO(new Bundle {
    val ifRam = Flipped(new IFRAMOp())
    val _MEM  = Flipped(new RAMOp())
  })

  private val imem_dummy = VecInit(
    "h_0000_0013".U, 
    "h_0020_0093".U, // addui x1, x0, 2 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0030_8113".U, // addui x2, x1, 3
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0041_0093".U, // addui x1, x2, 4
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U, 
    "h_0000_0013".U 
    )

  io.ifRam.ifstall := false.B
  io.ifRam.rdata   := imem_dummy(io.ifRam.addr(5, 2))

  // discard all data from MEM: dont have load/store instructions yet
  io._MEM.rdata    := 0.U
}
