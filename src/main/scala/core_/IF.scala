import chisel3._
import bundles._


class IF extends Module {
  val io = IO(new Bundle {
    val ram = new RAMOp()
    val stall = Input(Bool())
    val id = new IF_ID()
  })

  // pc bookkeeping
  val pc  = RegInit(Const.PC_INIT)
  val npc = Mux(io.stall,
    pc,
    Mux(io.id.if_branch,
      io.id.branch_tar,
      pc + 4.U))
  pc := npc

  // instruction fetch
  io.ram.addr  := pc; // fetch current instruction
  io.ram.mode  := RAMMode.LW
  io.ram.wdata := 0.U

  // feed to ID
  io.id.pc   := pc
  io.id.inst := Mux(io.stall, Const.NOP_INST, io.ram.rdata)

  printf("[IF] pc=%d, inst=%x\n", io.id.pc, io.id.inst)
}
