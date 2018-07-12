import chisel3._


class Core extends Module {
  val io =IO(new Bundle {
    val led = Vec(16, Output(Bool()))
  })
  
  io.led(0) := true.B
}
