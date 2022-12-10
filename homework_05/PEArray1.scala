package PEArray1

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

object ReuseType {
  val Systolic = 0
  val Multicast = 1
  val Stationary = 2
}

class PE[T<:Bits with Num[T]](dtype: T,
                              aType: Int,
                              bType: Int,
                              cType: Int) extends Module{
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
        systolicInput(a_in, a_out)

  val data_B = 
        stationaryInput(b_in, b_out)

  val data_C = 
        systolicInput(c_in, c_out)

  val compute_result = Wire(dtype)
  compute_result := data_A * data_B
  
  def systolicOutput(din: T, dout: T):Unit = {
      val r = Reg(dtype)
      r := din + compute_result
      dout := r
  }
  def multicastOutput(din: T, dout: T):Unit = {
      dout := din
  }
  def stationaryOutput(din: T, dout: T):Unit = {
      val r = Reg(dtype)
      when (control) {
        r := din
      }.otherwise {
        r := r + compute_result
      }
      dout := r
  }

  if (cType == ReuseType.Systolic)
    systolicOutput(c_in, c_out)
  else if (cType == ReuseType.Multicast)
    multicastOutput(c_in, c_out)
  else if (cType == ReuseType.Stationary)
    stationaryOutput(c_in, c_out)
}

class PEArray[T<:Bits with Num[T]](dtype:T) extends Module{
  val a_in = IO(Input(Vec(4, dtype)))
  val b_in = IO(Input(Vec(4, dtype)))
  val c_in = IO(Input(Vec(4, dtype)))
  val c_out = IO(Output(Vec(4, dtype)))
  val stationaryCtrl = IO(Input(Bool()))
  
  val aType = 0
  val bType = 2
  val cType = 0

  val PEs = for (i<-0 until 4) yield
              for (j<-0 until 4) yield
                Module(new PE(dtype, aType, bType, cType))

  def connectSystolicControl(PEs:Seq[Seq[PE[T]]]):Unit = {
    for (i<-0 until 4; j<-0 until 4) {
      PEs(i)(j).control := stationaryCtrl
    }
  }

  def systolicInputConnectA(PEs:Seq[Seq[PE[T]]], dx: Int, dy: Int):Unit = {
    for (i<-0 until 4; j<-0 until 4) {
      if (0 <= i+dx && i+dx < 4 && 0 <= j+dy && j+dy < 4) {
        PEs(i+dx)(j+dy).a_in := PEs(i)(j).a_out
      }
    }
    for (i<-0 until 4) {
      PEs(0)(i).a_in := a_in(i)
    }
  }

  def systolicInputConnectC(PEs:Seq[Seq[PE[T]]], dx: Int, dy: Int):Unit = {
    for (i<-0 until 4; j<-0 until 4) {
      if (0 <= i+dx && i+dx < 4 && 0 <= j+dy && j+dy < 4) {
        PEs(i+dx)(j+dy).c_in := PEs(i)(j).c_out
      }
    }
    for (i<-0 until 4) {
      PEs(i)(0).c_in := c_in(i)
    }
  }

  def stationaryInputConnectB(PEs: Seq[Seq[PE[T]]]): Unit = {
    for (i<-0 until 4; j<-0 until 3) {
      PEs(i)(j+1).b_in := PEs(i)(j).b_out
    }
    for (i<-0 until 4) {
      PEs(i)(0).b_in := b_in(i)
    }
  }

  def systolicOutputConnectC(PEs:Seq[Seq[PE[T]]], dx: Int, dy: Int):Unit = {
    for (i<-0 until 4) {
      c_out(i) := PEs(i)(3).c_out
    }
  }

  connectSystolicControl(PEs)
  systolicInputConnectA(PEs, 1, 0)
  stationaryInputConnectB(PEs)
  systolicInputConnectC(PEs, 0, 1)
  systolicOutputConnectC(PEs, 0, 1)

}
