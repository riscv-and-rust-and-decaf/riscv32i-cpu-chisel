import chisel3._
import bundles._

class CSR extends Module {
  val io = IO(new Bundle {
    val id = Flipped(new _CSR()) 
  })

  // Fake CSR now
  io.id.rdata := 42.U
}
