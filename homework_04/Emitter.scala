import firrtl._
import firrtl.ir._

import scala.collection.mutable

class Emitter(circuit: Circuit) {
  private val main  = circuit.modules.head.asInstanceOf[Module]
  private val stmts = main.body match {
    case Block(stmts) => stmts
    case o => Seq(o)
  }

  // e 是用来储存结果的 StringBuilder
  private val e = new StringBuilder
  def result = e.result()

  // 生成下一个 LLVM 匿名临时变量
  private var index: Int = 0
  private def newIndex(): Int = {index += 1; index}

  // 防止重名
  private val usedNames = mutable.Set[String]()
  private def newLLVMValue(instr: String, preferName: Option[String]=None) = {
    var name = "%" + preferName.getOrElse(newIndex().toString)
    if(usedNames contains name) {
      var cnt = 0
      def tryName = s"${name}_$cnt"
      while (usedNames contains tryName)
        cnt = cnt + 1
      name = tryName
    }
    usedNames.add(name)
    e ++= s"$name = $instr\n"
    name
  }
  private def newLLVMInstr(instr: String): Unit = e ++= s"$instr\n"

  // 将寄存器的FIRRTL名字映射成LLVM里变量的名字
  private val nameMap = mutable.HashMap[String,String]()

  // 递归地生成一个表达式的LLVM表示，返回该表达式结果在LLVM里的名字
  private def emitExpr(e: Expression, preferName: Option[String]=None): String = {
    val ew = bitWidth(e.tpe)
    e match {
      case l: Literal =>
        // LLVM 不允许生成 %1 = %2，只能强行加一个 0，生成 add 0, literal
        newLLVMValue(s"add i$ew 0, ${l.value}", preferName)
      case Reference(name, tpe, kind, flow) =>
        assert(nameMap contains name)
        nameMap(name)
      case Mux(cond, tval, fval, tpe) =>
        val condName = emitExpr(cond, preferName.map{_ + "_cond"})
        val tvalName = emitExpr(tval, preferName.map{_ + "_tval"})
        val fvalName = emitExpr(fval, preferName.map{_ + "_fval"})
        newLLVMValue(s"select i1 $condName, i$ew $tvalName, i$ew $fvalName", preferName)
      case DoPrim(op, args, consts, tpe) =>
        import PrimOps._
        // CODE HERE!
        // 调用emitExpr(e)生成e，然后如果e的位宽小于ew，将其ext到ew，返回最后得到的LLVM变量。
        // ext的时候根据e的类型选择zext或sext
        def ext(e: Expression) = {
          val gen_e = emitExpr(e)
          val argw = bitWidth(e.tpe)
          if (ew > argw) {
            if (e.tpe == ir.UIntType(ir.IntWidth(argw))) {
              newLLVMValue(s"zext i$argw ${gen_e} to i$ew")
            }
            else if (e.tpe == ir.SIntType(ir.IntWidth(argw))) {
              newLLVMValue(s"sext i$argw ${gen_e} to i$ew")
            }
            else {
              throw new Exception(s"${e.tpe} unknown type")
            }
          }
          else {
            newLLVMValue(s"add i$argw 0, ${gen_e}")
          }
        }
        def argsExt = {
          args map ext
        }
        op match {
          case e @ (Add | Sub | Mul | Div ) =>
            val lhs :: rhs :: Nil = argsExt
            newLLVMValue(s"$e i$ew $lhs, $rhs", preferName)
          case e @ (Or | And | Xor) =>
            val arg0_width = bitWidth(args(0).tpe)
            val arg1_width = bitWidth(args(1).tpe)
            val arg0_str = emitExpr(args(0))
            val arg1_str = emitExpr(args(1))
            var pre_str = newLLVMValue(s"add i$ew 0, 0")
            var ret_width = arg0_width
            if (arg0_width < arg1_width) {
              ret_width = arg1_width
              pre_str = newLLVMValue(s"zext i$arg0_width ${arg0_str} to i${arg1_width}")
              newLLVMValue(s"$e i$ret_width $pre_str, $arg1_str", preferName)
            }
            else {
              if (arg0_width > arg1_width) {
                ret_width = arg0_width
                pre_str = newLLVMValue(s"zext i$arg1_width ${arg1_str} to i${arg0_width}")
                newLLVMValue(s"$e i$ret_width $arg0_str, $pre_str", preferName)
              }
              else {
                ret_width = arg0_width
                newLLVMValue(s"$e i$ret_width $arg0_str, $arg1_str", preferName)
              }
            }
          case Not =>
            val arg_width = bitWidth(args(0).tpe)
            val arg_str = emitExpr(args(0))
            newLLVMValue(s"xor i$arg_width $arg_str, -1", preferName)

          case e @ (Lt | Gt | Neq | Eq | Leq | Geq)=>
            val lhs :: rhs :: Nil = argsExt
            val ret_width = bitWidth(args(0).tpe)
            if (e == Eq || e == Neq) {
              if (e == Eq)
                newLLVMValue(s"icmp $e i$ret_width $lhs, $rhs", preferName)
              else 
                newLLVMValue(s"icmp ne i$ret_width $lhs, $rhs", preferName)
            }
            else if (e == Geq) {
              if (args(0).tpe == ir.UIntType(ir.IntWidth(ret_width))) {
                newLLVMValue(s"icmp uge i$ret_width $lhs, $rhs", preferName)
              }
              else {
                newLLVMValue(s"icmp sge i$ret_width $lhs, $rhs", preferName)
              }
            }
            else if (e == Leq) {
              if (args(0).tpe == ir.UIntType(ir.IntWidth(ret_width))) {
                newLLVMValue(s"icmp ule i$ret_width $lhs, $rhs", preferName)
              }
              else {
                newLLVMValue(s"icmp sle i$ret_width $lhs, $rhs", preferName)
              }
            }
            else {
              if (args(0).tpe == ir.UIntType(ir.IntWidth(ret_width))) {
                newLLVMValue(s"icmp u$e i$ret_width $lhs, $rhs", preferName)
              }
              else {
                newLLVMValue(s"icmp s$e i$ret_width $lhs, $rhs", preferName)
              }
            }

          case Pad => 
            val arg = args(0)
            val arg_str = emitExpr(arg)
            val arg_width = bitWidth(arg.tpe)
            val n = consts(0)
            var ret_width = n
            if (ret_width < arg_width) {
              ret_width = arg_width
            }
            if (args(0).tpe == ir.UIntType(ir.IntWidth(arg_width))) {
              newLLVMValue(s"zext i$arg_width ${arg_str} to i$ret_width", preferName)
            }
            else {
              newLLVMValue(s"sext i$arg_width ${arg_str} to i$ret_width", preferName)
            }
          case e @ (Shl | Shr) =>
            val arg = args(0)
            val arg_str = emitExpr(arg)
            val arg_width = bitWidth(arg.tpe)
            val n = consts(0)
            var ret_width = arg_width
            var new_arg = arg_str;
            if (e == Shl) {
              ret_width = ret_width + n
              if (args(0).tpe == ir.UIntType(ir.IntWidth(arg_width))) {
                new_arg = newLLVMValue(s"zext i$arg_width ${arg_str} to i$ret_width")
              }
              else {
                new_arg = newLLVMValue(s"sext i$arg_width ${arg_str} to i$ret_width")
              }
              newLLVMValue(s"shl i$ret_width ${new_arg}, $n", preferName)
            }
            else {
              ret_width = ret_width - n
              if (args(0).tpe == ir.UIntType(ir.IntWidth(arg_width))) {
                new_arg = newLLVMValue(s"lshr i$arg_width ${new_arg}, $n")
              }
              else {
                new_arg = newLLVMValue(s"ashr i$arg_width ${new_arg}, $n")
              }
              newLLVMValue(s"trunc i$arg_width ${new_arg} to i$ret_width", preferName)
            }
            
          case e @ (Cvt | Neg) =>
            val arg = args(0)
            var arg_str = emitExpr(arg)
            val arg_width = bitWidth(arg.tpe)
            var ret_width = arg_width + 1
            if (e == Cvt && args(0).tpe == ir.SIntType(ir.IntWidth(arg_width))) {
              ret_width = ret_width - 1
              newLLVMValue(s"add i$ew 0, 0")
            }
            else if (e == Cvt) {
              newLLVMValue(s"sext i$arg_width ${arg_str} to i${ret_width}")
            }
            else {
              arg_str = newLLVMValue(s"sext i$arg_width ${arg_str} to i${ret_width}")
              newLLVMValue(s"sub i$ret_width 0, ${arg_str}")
            }
          case Dshl=>
            val arg0 = args(0)
            val arg1 = args(1)
            val arg0_width = bitWidth(arg0.tpe)
            val arg1_width = bitWidth(arg1.tpe)
            val arg0_str = emitExpr(arg0)
            val arg1_str = emitExpr(arg1)
            var pow_arg1 = 1
            for (i<-0 until arg1_width.toInt) pow_arg1 *= 2
            val ret_width = arg0_width + pow_arg1 - 1
            val arg1_ext = newLLVMValue(s"zext i$arg1_width $arg1_str to i$ret_width")
            var arg0_ext = newLLVMValue(s"zext i$arg0_width $arg0_str to i$ret_width")
            if (arg0.tpe == ir.UIntType(ir.IntWidth(arg0_width))) {
              arg0_ext = newLLVMValue(s"zext i$arg0_width $arg0_str to i$ret_width")
            }
            else {
              arg0_ext = newLLVMValue(s"sext i$arg0_width $arg0_str to i$ret_width")
            }
            newLLVMValue(s"shl i$ret_width $arg0_ext, $arg1_ext")
          case Dshr=> 
            val arg0 = args(0)
            val arg1 = args(1)
            val arg0_width = bitWidth(arg0.tpe)
            val arg1_width = bitWidth(arg1.tpe)
            val arg0_str = emitExpr(arg0)
            val arg1_str = emitExpr(arg1)            
            if (arg0.tpe == ir.UIntType(ir.IntWidth(arg0_width))) {
              val arg1_ext = newLLVMValue(s"zext i$arg1_width $arg1_str to i$arg0_width")
              newLLVMValue(s"lshr i$arg0_width $arg0_str, $arg1_ext")
            }
            else {
              val arg1_ext = newLLVMValue(s"zext i$arg1_width $arg1_str to i$arg0_width")
              newLLVMValue(s"ashr i$arg0_width $arg0_str, $arg1_ext")
            }
          case Bits=> 
            val arg = args(0)
            val arg_width = bitWidth(arg.tpe)
            var arg_str = emitExpr(arg)
            val hi = consts(0)
            val lo = consts(1)
            val n = hi - lo + 1
            arg_str = newLLVMValue(s"lshr i$arg_width ${arg_str}, $lo")
            newLLVMValue(s"trunc i$arg_width ${arg_str} to i$n")
          case Head=> 
            val n = consts(0)
            val arg = args(0)
            val arg_width = bitWidth(arg.tpe)
            var arg_str = emitExpr(arg)
            val shift_width = arg_width - n
            arg_str = newLLVMValue(s"lshr i$arg_width ${arg_str}, $shift_width")
            newLLVMValue(s"trunc i$arg_width ${arg_str} to i${n}", preferName)
          case Tail=> 
            val n = consts(0)
            val arg = args(0)
            val arg_width = bitWidth(arg.tpe)
            var arg_str = emitExpr(arg)
            val ret_width = arg_width - n
            newLLVMValue(s"trunc i$arg_width ${arg_str} to i${ret_width}", preferName)
          case Cat => {
            val arg0_width = bitWidth(args(0).tpe)
            val arg1_width = bitWidth(args(1).tpe)
            val arg0_str = emitExpr(args(0))
            val arg1_str = emitExpr(args(1))
            var ret_width = arg0_width + arg1_width
            val arg0_ext = newLLVMValue(s"zext i$arg0_width $arg0_str to i$ret_width")
            val arg1_ext = newLLVMValue(s"zext i$arg1_width $arg1_str to i$ret_width")
            val arg0_shr = newLLVMValue(s"shl i$ret_width $arg0_ext, $arg1_width")
            newLLVMValue(s"add i$ret_width $arg0_shr, $arg1_ext")
          }
          case e @ (AsSInt | AsUInt) =>  // CODE
            newLLVMValue(s"add i$ew 0, 0")
          case o => throw new Exception(s"$o is not allowed")
        }
      case o => throw new Exception(s"expr $o is not allowed")
    }
  }

  // 1. 将寄存器、IO Port生成为全局变量
  main.ports collect {
    case Port(info, name, direction, tpe) =>
      newLLVMInstr(s"@$name = global i${bitWidth(tpe)} 0")
  }
  stmts collect {
    case DefRegister(info, name, tpe, clock, reset, init) =>
      newLLVMInstr(s"@$name = global i${bitWidth(tpe)} 0")
  }
  // 2. 函数定义
  newLLVMInstr("define void @eval() {")
  // 3. 生成load全局变量的命令
  main.ports collect {
    case Port(info, name, Input, tpe) => // here Input
      val width = bitWidth(tpe)
      nameMap(name) = newLLVMValue(s"load i$width, i$width* @$name", Some(name))
  }
  stmts collect {
    case DefRegister(info, name, tpe, clock, reset, init) =>
      val width = bitWidth(tpe)
      nameMap(name) = newLLVMValue(s"load i$width, i$width* @$name", Some(name))
  }
  // 4. 生成函数内容
  // 4.1 先将DefNode的组合逻辑生成
  stmts collect {
    case s @ DefNode(info, name, value) =>
      newLLVMInstr(s"; ${s.serialize}")
      nameMap(name) = emitExpr(value, Some(name))
  }
  // 4.2 再生成寄存器更新的时序逻辑
  stmts collect {
    case s @ Connect(info, Reference(name, tpe, RegKind, flow), expr) => // here RegKind
      newLLVMInstr(s"; ${s.serialize}")
      val width = bitWidth(tpe)
      val next = emitExpr(expr, Some(name + "_next"))
      newLLVMInstr(s"store i$width $next, i$width* @$name")
      (name, next)
  } foreach {
    case (name, next) => nameMap(name) = next
  }
  // 4.3 重新生成DefNode的组合逻辑
  // 可能会生成大量死代码，我们不管，让LLVM来优化它
  stmts collect {
    case s @ DefNode(info, name, value) =>
      newLLVMInstr(s"; regen ${s.serialize}")
      nameMap(name) = emitExpr(value, Some(name))
  }
  // 4.4 最后生成输出Port的组合逻辑
  stmts collect {
    case s @ Connect(info, Reference(name, tpe, PortKind, flow), expr) => // here PortKind
      newLLVMInstr(s"; ${s.serialize}")
      val width = bitWidth(tpe)
      val next = emitExpr(expr, Some(name + "_next"))
      newLLVMInstr(s"store i$width $next, i$width* @$name")
  }
  // 5. 函数结尾
  newLLVMInstr("ret void")
  newLLVMInstr("}")
}
