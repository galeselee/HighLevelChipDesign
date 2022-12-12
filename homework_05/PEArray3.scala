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

class Bufferab_in[T <: Data](dtype: T) extends Module{
  val din = IO(DeqIO(dtype))
  val dout = IO(EnqIO(dtype))
  val fullReg = RegInit(0.B)
  val dataReg = Reg(dtype)
  val data_out = IO(Output(dtype))
  val a_valid = IO(Input(Bool()))
  val b_valid = IO(Input(Bool()))
  val c_valid = IO(Input(Bool()))
  when (dout.ready || !fullReg){
    fullReg := din.valid
    dataReg := din.bits
  }
  din.ready := !fullReg || (dout.ready && a_valid && b_valid && c_valid)  //TODO
  dout.valid := fullReg
  dout.bits := dataReg
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
  dout.bits := dataReg
}

// copy from csdn https://blog.csdn.net/weixin_44639164/article/details/123722937
class Bufferc_out[T <: Data](gen: T, n: Int) extends Module {
    val io = IO(new Bundle{
        val enq = Flipped(new DecoupledIO(gen)) // Flipped是反转接口
        val deq = new DecoupledIO(gen)
    })
    
    val enqPtr = RegInit(0.U(log2Up(n).W)) // 栈尾
    val deqPtr = RegInit(0.U(log2Up(n).W)) // 栈首
    val isFull = RegInit(false.B)  // 是否满了
    
    val doEnq = io.enq.ready && io.enq.valid // 需要执行入栈，入栈操作开启且入栈的元素有效
    val doDeq = io.deq.ready && io.deq.valid // 执行出栈
    
    val isEmpty = !isFull && (enqPtr === deqPtr) // 栈空
    
    val deqPtrInc = (deqPtr + 1.U) % n
    val enqPtrInc = (enqPtr + 1.U ) % n
    
    // 判断接下来是否会满
    val isFullNext = Mux(doEnq && !doDeq && (enqPtrInc === deqPtr),  // 入栈，且不出栈，且栈接下会满 
                                             true.B , Mux(doDeq && isFull, // 要出栈，且满了
                                                         false.B, isFull))
    enqPtr := Mux(doEnq, enqPtrInc, enqPtr) // 入栈，改变尾，向后加一个元素
    deqPtr := Mux(doDeq, deqPtrInc, deqPtr) // 出栈，改变首，头向后移一个
    
    isFull := isFullNext
    val ram = Mem(n, gen)
    when(doEnq){
        ram(enqPtr) := io.enq.bits
    }
    io.enq.ready := !isFull
    io.deq.valid := !isEmpty
    
    ram(deqPtr) <> io.deq.bits
}

class PE[T<:Bits with Num[T]](dtype: T, n: Int) extends Module{
  // Buffer
  val a_data = Module(New Bufferab_in(dtype))
  val b_data = Module(New Bufferab_in(dtype))
  val c_data_in = Module(New Bufferc_in(PData(dtype)))
  val c_data_out = Module(New Bufferc_out(PData(dtype), 4))
  val a_valid = RegInit(0.B)
  val b_valid = RegInit(0.B)
  val for_one = RegInit(1.B)
  val j_pos = RegInit(n.U(2.W))

  val c_val = Reg(PData(dtype))
  val c_val_out = IO(Output(PData(dtype)))
  val c_valid = RegInit(0.B)
  val c_valid_out = IO(Output(Bool()))

  c_valid_out := c_valid
  c_val_out := c_val

  a_valid := a_data.dout.valid
  b_valid := b_data.dout.valid
  a_data.a_valid := a_valid
  a_data.b_valid := b_valid
  a_data.c_valid := c_valid
  b_data.a_valid := a_valid
  b_data.b_valid := b_valid
  b_data.c_valid := c_valid

  when (c_valid) {
    c_valid := c_valid
  }.otherwise{
    when (c_data_in.c_in.pos == j_pos) {
      c_valid := for_one
    }.otherwise {
      c_valid := c_valid
    }
  }
  
  when (c_data_in.c_in.pos == j_pos) {  
    c_val := c_data_in.c_in.bits
  }.otherwise{
    when (a_valid && b_valid && c_valid && c_data_out.io.enq.ready) {
      c_val := c_val + a_data_in.data_out * b_data_in.data_out
    }.otherwise {
      c_val := c_val
    }
  }

}

class PEArray[T<:Bits with Num[T]](dtype:T) extends Module{
  val a_in = IO(Vec(4, DeqIO(dtype)))
  val b_in = IO(Vec(4, DeqIO(dtype)))
  val c_in = IO(Vec(4, DeqIO(PData(dtype))))
  val c_out = IO(Vec(4, EnqIO(PData(dtype))))

  val PEs = for (i<-0 until 4) yield
              for (j<-0 until 4) yield
                Module(new PE(dtype, j))

  val for_one = RegInit(1.B)
  val for_zero = RegInit(0.B)


  def AInputConnect(PEs:Seq[Seq[PE[T, Int]]], dx: Int, dy: Int): Unit = {
    for (i<-0 until 4; j<-0 until 4) {
      if (0 <= i+dx && i+dx < 4 && 0 <= j+dy && j+dy < 4) {
        PEs(i+dx)(j+dy).a_data.din.valid := PEs(i)(j).a_data.dout.valid
        PEs(i+dx)(j+dy).a_data.din.bits := PEs(i)(j).a_data.dout.bits
        PEs(i)(j).a_data.dout.ready := PEs(i+dx)(j+dy).a_data.din.ready
      }
    }
    for (i<-0 until 4) {
      PEs(0)(i).a_data.din.valid := a_in(i).valid
      PEs(0)(i).a_data.din.bits := a_in(i).bits
      a_in(i).ready := PEs(0)(i).a_data.din.ready
      PEs(3)(i).a_data.dout.ready := for_one
    }
  }

  def BInputConnect(PEs:Seq[Seq[PE[T, Int]]], dx: Int, dy: Int): Unit = { 
    for (i<-0 until 4; j<-0 until 4) {
      if (0 <= i+dx && i+dx < 4 && 0 <= j+dy && j+dy < 4) {
        PEs(i+dx)(j+dy).b_data.din.valid := PEs(i)(j).b_data.dout.valid
        PEs(i+dx)(j+dy).b_data.din.bits := PEs(i)(j).b_data.dout.bits
        PEs(i)(j).b_data.dout.ready := PEs(i+dx)(j+dy).b_data.din.ready
      }
    }
    for (i<-0 until 4) {
      PEs(i)(0).b_data.din.valid := b_in(i).valid
      PEs(i)(0).b_data.din.bits := b_in(i).bits
      b_in(i).ready := PEs(i)(0).b_data.din.ready
      PEs(i)(3).b_data.dout.ready := for_one
    }
  }

  def CInputConnect(PEs:Seq[Seq[PE[T, Int]]], dx: Int, dy: Int): Unit = {
    for (i<-0 until 4; j<-0 until 4) {
      if (0 <= i+dx && i+dx < 4 && 0 <= j+dy && j+dy < 4) {
        PEs(i+dx)(j+dy).c_data.din.valid := PEs(i)(j).c_data.dout.valid
        PEs(i+dx)(j+dy).c_data.din.bits := PEs(i)(j).c_data.dout.bits
        PEs(i)(j).c_data.dout.ready := PEs(i+dx)(j+dy).c_data.din.ready
      }
    }
    for (i<-0 until 4) {
      PEs(i)(0).c_data.din.valid := c_in(i).valid
      PEs(i)(0).c_data.din.bits := c_in(i).bits
      c_in(i).ready := PEs(i)(0).c_data.din.ready
      PEs(i)(3).c_data.dout.ready := for_one
    }
  }

  def COutputConnect(PEs:Seq[Seq[PE[T, Int]]], dx: Int, dy: Int): Unit = {
    val pos_soft = for (i<-0 until 4) yield i.U(2.W)
    val j_pos = RegInit(Vec(pos_soft))    
    for (i<-dx until 4; j<-dy until 4) {
      
      when(PEs(i)(j).c_data_in.c_in.pos == j_pos && PEs(i)(j).c_valid_out) {
        PEs(i)(j).c_data_out.din.bits := c_val_out
        PEs(i)(j).c_data_out.din.valid := for_one
        PEs(i-dx)(j-dy).c_data_out.dout.ready := for_zero
      }.otherwise {
        PEs(i)(j).c_data_out.din.bits := PEs(i-dx)(j-dy).c_data_out.dout.bits
        PEs(i)(j).c_data_out.din.valid := PEs(i-dx)(j-dy).c_data_out.dout.valid
        PEs(i-dx)(j-dy).c_data_out.dout.ready := PEs(i)(j).c_data_out.din.ready
      }
    }
    for (i<-0 until 4) {
      PEs(i)(3).c_data_out.dout.ready := c_in(i).ready
      c_in(i).bits := PEs(i)(3).c_data_out.dout.bits
      c_in(i).valid := PEs(i)(3).c_data_out.dout.valid
    }
  }
  AInputConnect(PEs, 1, 0)
  BInputConnect(PEs, 0, 1)
  CInputConnect(PEs, 0, 1)
  COutputConnect(PEs, 0, 1)
}