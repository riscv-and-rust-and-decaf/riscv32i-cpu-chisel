import chisel3._

/*
class MyTop extends Module {
  val io = IO(new Bundle {
    val sw_num1 = Input(UInt(16.W))
    val sw_num2 = Input(UInt(16.W))
    val loadingValues = Input(Bool())
    val btn1    = Input(Bool()) 
    val btn2    = Input(Bool()) 

    val num = Output(UInt(16.W))
  })

  val gcd = Module(new GCD())
  gcd.io.value1 := io.sw_num1
  gcd.io.value2 := io.sw_num2
  gcd.io.loadingValues := io.loadingValues

  when(io.btn1)      { io.num := io.sw_num1 }
  .elsewhen(io.btn2) { io.num := io.sw_num2 }
  .otherwise         { io.num := gcd.io.outputGCD }

}
*/

object Gen {
  def main(args: Array[String]) : Unit = {
    chisel3.Driver.execute(args, () => new Core())
  }
}
