import core_.Core

object Gen {
  def main(args: Array[String]) : Unit = {
    chisel3.Driver.execute(args, () => new Core())
  }
}
