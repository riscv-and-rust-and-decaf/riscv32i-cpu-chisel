import chisel3._


class Core extends Module {
  val io =IO(new Bundle {
    val log = Output(UInt(32.W))
  })

  val ifModule  = Module(new IF())
  val idModule  = Module(new ID())
  val exModule  = Module(new EX())
  val memModule = Module(new MEM())
  val regModule = Module(new RegFile)
  val mmuModule = Module(new IMemMMU())

  when(true.B) {
    ifModule.io.ram   <> mmuModule.io.ifRam
    ifModule.io.id    <> idModule.io.iff

    idModule.io.ex   <> exModule.io._ID
    idModule.io.reg  <> regModule.io._ID

    exModule.io._MEM  <> memModule.io._EX

    memModule.io._MMU <> mmuModule.io._MEM
    memModule.io._Reg <> regModule.io._MEM
  }
  .otherwise {
    ifModule.io.ram   <> mmuModule.io.ifRam
    ifModule.io.id    <> idModule.io.iff

    idModule.io.ex   <> exModule.io._ID
    idModule.io.reg  <> regModule.io._ID

    exModule.io._MEM  <> memModule.io._EX

    memModule.io._MMU <> mmuModule.io._MEM
    memModule.io._Reg <> regModule.io._MEM
  }
 
  io.log := 5.U(32.W)

}
