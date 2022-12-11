package PEArray2

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

class PE[T<:Bits with Num[T]](dtype: T) extends Module{
  val a_in = IO(Input(dtype))
  val a_out = IO(Output(dtype))
  val b_in = IO(Input(dtype))
  val b_out = IO(Output(dtype))
  val c_in = IO(Input(dtype))
  val c_out = IO(Output(dtype))
  val control = IO(Input(Bool()))

  def systolicInput(din: T, dout: T):T = {
      val r = Reg(dtype)
      r := din
      dout := r
      r
  }
  def multicastInput(din: T, dout: T):T = {
      dout := din
      din
  }
  def stationaryInput(din: T, dout: T):T = {
      val r = Reg(dtype)
      when (control) { // control
        r := din
      }.otherwise {
        r := r
      }
      dout := r
      r
  }

  val data_A = 
        stationaryInput(a_in, a_out)

  val data_B = 
        multicastInput(b_in, b_out)

  //val data_C = 
  //      multicastInput(c_in, c_out)

  // val tmp = Reg(dtype)
  // val cin_tmp = Reg(dtype)
  val compute_result = IO(Output((dtype)))
  compute_result :=  data_A * data_B
  //cin_tmp := c_in
  c_out := c_in
  
  // def multicastOutput(din: T, dout: T):Unit = {
  //     dout := din
  // }

  // //multicastOutput(c_in, c_out)
}


class PEArray[T<:Bits with Num[T]](dtype:T) extends Module{
  val a_in = IO(Input(Vec(4, dtype)))
  val b_in = IO(Input(Vec(4, dtype)))
  val c_in = IO(Input(Vec(4, dtype)))
  val c_out = IO(Output(Vec(4, dtype)))
  val stationaryCtrl = IO(Input(Bool()))
  val PEs = for (i<-0 until 4) yield
              for (j<-0 until 4) yield
                Module(new PE(dtype))

  def connectSystolicControl(PEs:Seq[Seq[PE[T]]]):Unit = {
    for (i<-0 until 4; j<-0 until 4) {
      PEs(i)(j).control := stationaryCtrl
    }
  }

  def stationaryInputConnectA(PEs: Seq[Seq[PE[T]]]): Unit = {
    for (i<-0 until 3; j<-0 until 4) {
      PEs(i+1)(j).a_in := PEs(i)(j).a_out
    }
    for (i<-0 until 4) {
      PEs(0)(i).a_in := a_in(i)
    }
  }

  def multicastInputConnectB(PEs:Seq[Seq[PE[T]]], dx: Int, dy: Int):Unit = {
    for (i<-0 until 4; j<-0 until 4) {
      if (0 <= i+dx && i+dx < 4 && 0 <= j+dy && j+dy < 4) {
        PEs(i+dx)(j+dy).b_in := PEs(i)(j).b_out
      }
    }
    for (i<-0 until 4) {
      PEs(0)(i).b_in := b_in(i)
    }
  }

  def multicastInputConnectC(PEs:Seq[Seq[PE[T]]], dx: Int, dy: Int):Unit = {
    for (i<-0 until 4; j<-0 until 4) {
      if (0 <= i+dx && i+dx < 4 && 0 <= j+dy && j+dy < 4) {
        PEs(i+dx)(j+dy).c_in := PEs(i)(j).c_out
      }
    }
    for (i<-0 until 4) {
      PEs(i)(0).c_in := c_in(i)
    }
  }

  def multicastOutputConnectC(PEs:Seq[Seq[PE[T]]]): Unit = {
    for (i <- 0 until 4) {
      val level0 = Reg(Vec(4, dtype))
      val level1 = Reg(Vec(2, dtype))
      val level2 = Reg(dtype)
      val cin_tmp = Reg(dtype)
      cin_tmp := PEs(i)(0).c_out
      for (j<-0 until 4) {
        level0(j) := PEs(i)(j).compute_result
      }
      for (j<-0 until 2) {
        level1(j) := level0(2*j) + level0(2*j+1)
      }
      level2 := level1(0) + level1(1) 
      c_out(i) := level2 + cin_tmp
    }
  }


  connectSystolicControl(PEs)
  stationaryInputConnectA(PEs)
  multicastInputConnectB(PEs, 1, 0)
  multicastInputConnectC(PEs, 0, 1)
  multicastOutputConnectC(PEs)
}
