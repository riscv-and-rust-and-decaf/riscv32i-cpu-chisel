package devices

import chisel3._
import core_._


class MockSerial(printLog: Boolean = true) extends Module {
  val io = IO(Flipped(new RAMOp))

  val inputs_raw = "E 0x10000\n0x02a00593\n0x10000737\n0x00574783\n0x0207f793\n0x00079463\n0x0000006f\n0x00b70023\n0x00008067\n\nJ 0x10000\n"

  val inputs = VecInit(inputs_raw.map(x => x.U(8.W)))
  val next_input_num = RegInit(0.U(32.W))

  val canRead = next_input_num =/= inputs.length.U
  val canWrite = true.B
  val status = (canWrite << 5.U) | canRead

  val rdata = Wire(UInt(8.W))
  rdata := 0.U
  // test
  when(io.mode === RAMMode.LBU && io.addr(2,0) === 5.U) {
    rdata := status
  }
  // read
  when(RAMMode.isRead(io.mode) && io.addr(2,0) === 0.U) {
    when(canRead) {
      rdata := inputs(next_input_num)
      next_input_num := next_input_num + 1.U
    }
    if (printLog)
      printf("[Serial] Read: (%d/%d) 0x%x, '%c'\n", next_input_num, inputs_raw.length.U, rdata, rdata)
    else
      printf("%c", rdata)
  }
  // write
  when(io.mode === RAMMode.SB && io.addr(2,0) === 0.U) {
    if (printLog)
      printf("[Serial] Write: 0x%x, '%c'\n", io.wdata(7,0), io.wdata(7,0))
    else
      printf("%c", io.wdata(7,0))
  }

  io.rdata := Mux(rdata(7) && io.mode === RAMMode.LB, 0xffffff.U(24.W), 0.U(24.W)) ## rdata
  io.ok := io.mode =/= RAMMode.NOP
}
