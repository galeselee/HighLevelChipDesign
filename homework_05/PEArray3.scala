package PEArray3

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import chisel3.util.log2Up

class PData[T<:Data](dtype:T) extends Bundle{
  val data = dtype.cloneType
  val pos = UInt(2.W)
}
object PData{
  def apply[T<:Data](dtype:T) = new PData(dtype)
}

class Bufferab_in[T <: Data](dtype: T, n: UInt, m: UInt) extends Module{
  val din = IO(DeqIO(dtype))
  val dout = IO(EnqIO(dtype))
  val fullReg = RegInit(0.B)
  val dataReg = Reg(dtype)
  val data_out = IO(Output(dtype))
  val ab_in_valid = IO(Input(Bool()))
  val ab_out_valid = IO(Output(Bool()))
  val ab_in_ready = IO(Input(Bool()))
  val ab_out_ready = IO(Output(Bool()))
  val c_in_valid = IO(Input(Bool()))

  val c_valid = IO(Input(Bool()))

  val count = IO(Input(UInt(32.W)))

  data_out := dataReg
  when ((dout.ready && ab_in_valid && c_valid && c_in_valid && ab_in_ready) || !fullReg){ // print fullReg
    fullReg := din.valid
    dataReg := din.bits
  }
  din.ready := !fullReg || (dout.ready && ab_in_valid && c_valid && c_in_valid && ab_in_ready)  //TODO
  val for_zero = RegInit(0.B)
  dout.valid := for_zero
  dout.bits := dataReg
  ab_out_valid := fullReg
  ab_out_ready := dout.ready
}

class Bufferc_in[T <: Data](dtype: T) extends Module{
  val din = IO(DeqIO(dtype))
  val dout = IO(EnqIO(dtype))
  val fullReg = RegInit(0.B)
  val passReg = Reg(dtype)

  when (dout.ready || !fullReg){
    fullReg := din.valid
    passReg := din.bits
  }
  din.ready := !fullReg || dout.ready
  dout.valid := fullReg
  dout.bits := passReg
}

// copy from csdn https://blog.csdn.net/weixin_44639164/article/details/123722937
class Bufferc_out[T <: Data](gen: T) extends Module {
    val io = IO(new Bundle{
        val enq = Flipped(new DecoupledIO(gen)) // Flipped是反转接口
        val deq = new DecoupledIO(gen)
    })
    
    val enqPtr = RegInit(0.U(2.W)) // 栈尾
    val deqPtr = RegInit(0.U(2.W)) // 栈首
    val isFull = RegInit(false.B)  // 是否满了
    val one_reg = RegInit(1.U(2.W))
    
    val doEnq = io.enq.ready && io.enq.valid // 需要执行入栈，入栈操作开启且入栈的元素有效
    val doDeq = io.deq.ready && io.deq.valid // 执行出栈
    
    val isEmpty = !isFull && (enqPtr === deqPtr) // 栈空
    
    val deqPtrInc = (deqPtr + one_reg)
    val enqPtrInc = (enqPtr + one_reg)
    
    // 判断接下来是否会满
    val isFullNext = Mux(doEnq && !doDeq && (enqPtrInc === deqPtr),  // 入栈，且不出栈，且栈接下会满 
                                             true.B , Mux(doDeq && isFull, // 要出栈，且满了
                                                         false.B, isFull))
    enqPtr := Mux(doEnq, enqPtrInc, enqPtr) // 入栈，改变尾，向后加一个元素
    deqPtr := Mux(doDeq, deqPtrInc, deqPtr) // 出栈，改变首，头向后移一个
    
    isFull := isFullNext
    val ram = Reg(Vec(4, gen))
    when(doEnq){
        ram(enqPtr) := io.enq.bits
    }
    io.enq.ready := !isFull
    io.deq.valid := !isEmpty
    
    ram(deqPtr) <> io.deq.bits

    val ramio = IO(Output(Vec(4, gen)))
    ramio := ram
}

class PE[T<:Bits with Num[T]](dtype: T, n: UInt, m: UInt) extends Module{
  // Buffer
  val a_data = Module(new Bufferab_in(dtype, n, m))
  val b_data = Module(new Bufferab_in(dtype, n, m))
  val c_data_in = Module(new Bufferc_in(PData(dtype)))
  val c_data_out = Module(new Bufferc_out(PData(dtype)))
  val for_one = RegInit(1.B)
  val j_pos = RegInit(n)

  // IO
  val ramio = IO(Output(Vec(4, PData(dtype))))
  ramio := c_data_out.ramio
  val count = IO(Input(UInt(32.W)))
  a_data.count := count
  b_data.count := count

  val a_din = IO(DeqIO(dtype))
  val a_dout = IO(EnqIO(dtype))
  a_data.din.valid := a_din.valid
  a_data.din.bits := a_din.bits
  a_din.ready := a_data.din.ready
  a_dout.valid := a_data.dout.valid
  a_dout.bits := a_data.dout.bits
  a_data.dout.ready := a_dout.ready

  val b_din = IO(DeqIO(dtype))
  val b_dout = IO(EnqIO(dtype))
  b_data.din.valid := b_din.valid
  b_data.din.bits := b_din.bits
  b_din.ready := b_data.din.ready
  b_dout.valid := b_data.dout.valid
  b_dout.bits := b_data.dout.bits
  b_data.dout.ready := b_dout.ready

  val c_in_din = IO(DeqIO(PData(dtype)))
  val c_in_dout = IO(EnqIO(PData(dtype)))
  c_data_in.din.valid := c_in_din.valid
  c_data_in.din.bits := c_in_din.bits
  c_in_din.ready := c_data_in.din.ready
  c_in_dout.valid := c_data_in.dout.valid
  c_in_dout.bits := c_data_in.dout.bits
  c_data_in.dout.ready := c_in_dout.ready

  val c_out_din = IO(DeqIO(PData(dtype)))
  val c_out_dout = IO(EnqIO(PData(dtype)))
  c_data_out.io.enq.valid := c_out_din.valid
  c_data_out.io.enq.bits := c_out_din.bits
  c_out_din.ready := c_data_out.io.enq.ready
  c_out_dout.valid := c_data_out.io.deq.valid
  c_out_dout.bits := c_data_out.io.deq.bits
  c_data_out.io.deq.ready := c_out_dout.ready


  val c_val = Reg(PData(dtype))
  val c_valid = RegInit(0.B)
  val c_valid_out = IO(Output(Bool()))

  c_valid_out := c_valid

  a_data.ab_in_valid := b_data.ab_out_valid
  b_data.ab_in_valid := a_data.ab_out_valid
  a_data.ab_in_ready := b_data.ab_out_ready
  b_data.ab_in_ready := a_data.ab_out_ready

  a_data.c_valid := c_valid
  b_data.c_valid := c_valid

  c_val.pos := j_pos

  when (c_data_in.din.bits.pos === j_pos && c_data_in.din.valid) {
    c_valid := for_one
  }.otherwise {
    c_valid := c_valid
  }
  
  val reg_0 = RegInit(0.U(2.W))
  a_data.c_in_valid := c_data_out.io.enq.ready
  b_data.c_in_valid := c_data_out.io.enq.ready
  when (c_data_in.din.bits.pos === j_pos && c_data_in.din.valid) {  
    c_val.data := c_data_in.din.bits.data
  }.otherwise{
    when (b_data.ab_out_valid && a_data.ab_out_valid && c_valid && c_data_out.io.enq.ready && a_data.dout.ready && b_data.dout.ready) {
      c_val.data := c_val.data + a_data.data_out * b_data.data_out
      a_dout.valid := for_one
      b_dout.valid := for_one
    }.otherwise {
      c_val.data := c_val.data
    }
  }
  
  val c_val_out = IO(Output(PData(dtype)))
  c_val_out := c_val

}

class PEArray[T<:Bits with Num[T]](dtype:T) extends Module{
  val a_in = IO(Vec(4, DeqIO(dtype)))
  val b_in = IO(Vec(4, DeqIO(dtype)))
  val c_in = IO(Vec(4, DeqIO(PData(dtype))))
  val c_out = IO(Vec(4, EnqIO(PData(dtype))))

  val count = RegInit(1.U(32.W))
  val acc = RegInit(1.U(32.W))
  count := count + acc


  val PEs = for (i<-0 until 4) yield
              for (j<-0 until 4) yield
                Module(new PE(dtype, j.U(2.W), i.U(2.W)))

  val for_one = RegInit(1.B)
  val for_zero = RegInit(0.B)


  def AInputConnect(PEs:Seq[Seq[PE[T]]], dx: Int, dy: Int): Unit = {
    for (i<-0 until 4; j<-0 until 4) {
      if (0 <= i+dx && i+dx < 4 && 0 <= j+dy && j+dy < 4) {
        PEs(i+dx)(j+dy).a_din <> PEs(i)(j).a_dout
      }
    }
    for (i<-0 until 4) {
      PEs(0)(i).a_din.valid := a_in(i).valid
      PEs(0)(i).a_din.bits := a_in(i).bits
      a_in(i).ready := PEs(0)(i).a_din.ready
      PEs(3)(i).a_dout.ready := for_one
    }
  }

  def BInputConnect(PEs:Seq[Seq[PE[T]]], dx: Int, dy: Int): Unit = { 
    for (i<-0 until 4; j<-0 until 4) {
      if (0 <= i+dx && i+dx < 4 && 0 <= j+dy && j+dy < 4) {
        PEs(i+dx)(j+dy).b_din <> PEs(i)(j).b_dout
      }
    }
    for (i<-0 until 4) {
      PEs(i)(0).b_din.valid := b_in(i).valid
      PEs(i)(0).b_din.bits := b_in(i).bits
      b_in(i).ready := PEs(i)(0).b_din.ready
      PEs(i)(3).b_dout.ready := for_one
    }
  }

  def CInputConnect(PEs:Seq[Seq[PE[T]]], dx: Int, dy: Int): Unit = {
    for (i<-0 until 4; j<-0 until 4) {
      if (0 <= i+dx && i+dx < 4 && 0 <= j+dy && j+dy < 4) {
        PEs(i+dx)(j+dy).c_in_din <> PEs(i)(j).c_in_dout
      }
    }
    for (i<-0 until 4) {
      PEs(i)(0).c_in_din.valid := c_in(i).valid
      PEs(i)(0).c_in_din.bits := c_in(i).bits
      c_in(i).ready := PEs(i)(0).c_in_din.ready
      PEs(i)(3).c_in_dout.ready := for_one
    }
  }

  def COutputConnect(PEs:Seq[Seq[PE[T]]], dx: Int, dy: Int): Unit = {
    val pos_soft = for (i<-0 until 4) yield i.U(2.W)
    val j_pos = RegInit(VecInit(pos_soft))  
    for (i<-dx until 4; j<-dy until 4) {
      when(PEs(i)(j).c_in_din.bits.pos === j_pos(j) && PEs(i)(j).c_in_din.valid && PEs(i)(j).c_valid_out) {
          PEs(i)(j).c_out_din.bits := PEs(i)(j).c_val_out
          PEs(i)(j).c_out_din.valid := for_one
          PEs(i-dx)(j-dy).c_out_dout.ready := PEs(i)(j).c_out_din.ready
        }.otherwise {
          PEs(i)(j).c_out_din <> PEs(i-dx)(j-dy).c_out_dout
        }
    }
    for (i <- 0 until 4) {
      PEs(i)(0).c_out_din.bits := PEs(i)(0).c_val_out
      when (PEs(i)(0).c_in_din.bits.pos === j_pos(0) && PEs(i)(0).c_in_din.valid && PEs(i)(0).c_valid_out) {
        PEs(i)(0).c_out_din.valid := for_one
      }.otherwise{
        PEs(i)(0).c_out_din.valid := for_zero
      }
    }
    for (i<-0 until 4) {
      PEs(i)(3).c_out_dout.ready := c_out(i).ready
      c_out(i).bits := PEs(i)(3).c_out_dout.bits
      c_out(i).valid := PEs(i)(3).c_out_dout.valid
    }
  }

  def CountConnect(PEs:Seq[Seq[PE[T]]]): Unit = {
    for (i<-0 until 4; j<-0 until 4) {
      PEs(i)(j).count := count
    } 
  }
  CountConnect(PEs)


  AInputConnect(PEs, 1, 0)
  BInputConnect(PEs, 0, 1)
  CInputConnect(PEs, 0, 1)
  COutputConnect(PEs, 0, 1)
}

