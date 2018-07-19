import chisel3._
import java.io.{File, FileWriter}

object Gen {
  def main(args: Array[String]) : Unit = {
    val dirname = if(args.length == 0) "out" else args(0)
    val dir = new File(dirname); dir.mkdirs
    val chirrtl = firrtl.Parser.parse(chisel3.Driver.emit(() => new Core))
    val writer = new FileWriter(new File(dir, s"${chirrtl.main}.fir"))
    writer write chirrtl.serialize
    writer.close

    val verilog = new FileWriter(new File(dir, s"${chirrtl.main}.v"))
    verilog write (new firrtl.VerilogCompiler).compileAndEmit(firrtl.CircuitState(chirrtl, firrtl.ChirrtlForm)).getEmittedCircuit.value
    verilog.close
  }
}
