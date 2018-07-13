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
    "h_0000_1111".U, "h_1111_2222".U, "h_2222_3333".U, "h_3333_4444".U,
    "h_4444_5555".U, "h_5555_6666".U, "h_6666_7777".U, "h_7777_8888".U,
    "h_8888_9999".U, "h_9999_aaaa".U, "h_aaaa_bbbb".U, "h_bbbb_cccc".U,
    "h_cccc_dddd".U, "h_dddd_eeee".U, "h_eeee_ffff".U, "h_ffff_0000".U)

  io.ifRam.ifstall := false.B
  io.ifRam.rdata   := imem_dummy(io.ifRam.addr(5, 2))

  // discard all data from MEM: dont have load/store instructions yet
  io._MEM.rdata    := 0.U
}
