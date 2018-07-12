import chisel3._


class Core extends Module {
  val io =IO(new Bundle {
    val log = Output(UInt(32.W))
  })

  val ifModule  = Module(new IF())
  val idModule  = Module(new DummyID())
  val exModule  = Module(new EX())
  val memModule = Module(new MEM())
  
  val regModule = Module(new RegFile)

  val mmuModule = Module(new DummyIMemMMU())


  when(true.B) {
    ifModule.io.ram   <> mmuModule.io.ifRam
    ifModule.io.id    <> idModule.io._IF
    
    idModule.io._EX   <> exModule.io._ID
    idModule.io._Reg  <> regModule.io._ID

    exModule.io._MEM  <> memModule.io._EX
    
    memModule.io._MMU <> mmuModule.io._MEM 
    memModule.io._Reg <> regModule.io._MEM
  }


}
