package devices

import chisel3._
import core_._


class MockSerial extends Module {
  val io = IO(Flipped(new RAMOp))

  val inputs_raw = "Hello CPU!"
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
    printf("[Serial] Read: (%d/%d) 0x%x, '%c'\n", next_input_num, inputs_raw.length.U, rdata, rdata)
  }
  // write
  when(io.mode === RAMMode.SB && io.addr(2,0) === 0.U) {
    printf("[Serial] Write: 0x%x, '%c'\n", io.wdata(7,0), io.wdata(7,0))
  }

  io.rdata := Mux(rdata(7) && io.mode === RAMMode.LB, 0xffffff.U(24.W), 0.U(24.W)) ## rdata
  io.ok := io.mode =/= RAMMode.NOP
}
