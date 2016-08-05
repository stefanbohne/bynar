package org.bynar

import org.bynar.versailles.Term
import org.bynar.versailles.Expression
import org.bynar.versailles.Statement
import org.bynar.versailles.NumberLiteral
import org.bynar.versailles.Application
import org.bynar.versailles.Plus
import org.bynar.versailles.Member
import org.bynar.versailles.Literal
import org.bynar.versailles.VariableIdentity
import org.bynar.versailles.Sequence
import org.bynar.versailles.Let
import org.bynar.versailles.Variable
import org.bynar.versailles.Block
import org.bynar.versailles.Undefined
import org.bynar.versailles.Tuple
import org.bynar.versailles.Equals
import org.bynar.versailles.IfStmt
import org.bynar.versailles.BooleanLiteral

trait BitTypeExpression extends Expression {

    def dependentBitWidth(base: Expression): Expression

}

case class BitFieldType(val bitWidth: Expression) extends BitTypeExpression {

    override type SelfTerm = BitFieldType

    override def dependentBitWidth(base: Expression) = bitWidth
    def copy(bitWidth: Expression) =
        BitFieldType(bitWidth).copyAnnotationsFrom(this)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('bw).getOrElse(bitWidth).asInstanceOf[Expression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('bw, bitWidth, a)

}

case class BitWidth() extends Literal {
    override type SelfTerm = BitWidth
    override def toString = "bitwidth"
}

case class BitRecordComponent(val name: Symbol, val `type`: Expression) extends Statement {

    override type SelfTerm = BitRecordComponent

    def copy(name: Symbol = name, `type`: Expression = `type`) =
        BitRecordComponent(name, `type`).copyAnnotationsFrom(this)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(`type` = children.lift('t).getOrElse(`type`).asInstanceOf[Expression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('t, `type`, a)

}
case class BitRecordType(val body: Statement) extends BitTypeExpression {

    override type SelfTerm = BitRecordType

    override def dependentBitWidth(base: Expression) = {
        val result0 = VariableIdentity.setName(new VariableIdentity(), 'result)
        var result = result0
        def mapStatements(s: Statement): Statement =
            s match {
            case s@Sequence(ss@_*) =>
                s.copy(ss.map{ mapStatements(_) }:_*)
            case BitRecordComponent(n, t) =>
                val result1 = result
                result = VariableIdentity.setName(new VariableIdentity(), 'result)
                Let(Variable(result, true),
                    Application(Application(Plus(),
                        Application(Application(BitWidth(), t.copy(Map())), Member(base, n))),
                        Variable(result1, true)))
            }
        Block(Sequence(Let(Variable(result0, true), NumberLiteral(0)),
                       mapStatements(body)),
              Variable(result, true))
    }
    def copy(body: Statement = body) =
        BitRecordType(body).copyAnnotationsFrom(this)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('b).getOrElse(body).asInstanceOf[Statement])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('b, body, a)
}

case class BitUnionVariant(val name: Symbol, val `type`: Expression) extends Statement {

    override type SelfTerm = BitUnionVariant

    def copy(name: Symbol = name, `type`: Expression = `type`) =
        BitUnionVariant(name, `type`).copyAnnotationsFrom(this)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(`type` = children.lift('t).getOrElse(`type`).asInstanceOf[Expression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('t, `type`, a)

}
case class BitUnionType(val body: Statement) extends BitTypeExpression {

    override type SelfTerm = BitUnionType

    override def dependentBitWidth(base: Expression) = {
        val result0 = VariableIdentity.setName(new VariableIdentity(), 'result)
        var result = result0
        def mapStatements(s: Statement): Statement =
            s match {
            case s@Sequence(ss@_*) =>
                s.copy(ss.map{ mapStatements(_) }:_*)
            case BitUnionVariant(n, t) =>
                val result1 = result
                result = VariableIdentity.setName(new VariableIdentity(), 'result)
                IfStmt(Application(Application(Equals(), Variable(result1, true)), Tuple()),
                       Let(Variable(result, true), Application(Application(BitWidth(), t.copy(Map())), Variable(result1, true))),
                       Let(Variable(result, true), Variable(result1, true)),
                       Undefined())
            }
        Block(Sequence(Let(Variable(result0, true), Tuple()),
                       mapStatements(body)),
              Variable(result, true))
    }
    def copy(body: Statement = body) =
        BitUnionType(body).copyAnnotationsFrom(this)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('b).getOrElse(body).asInstanceOf[Statement])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('b, body, a)
}

case class WrittenType(val `type`: Expression, val written: Expression) extends BitTypeExpression {

    override type SelfTerm = WrittenType

    override def dependentBitWidth(base: Expression) =
        Application(Application(BitWidth(), `type`), base)
    def copy(`type`: Expression = `type`, written: Expression = written) =
        WrittenType(`type`, written)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('t).getOrElse(`type`).asInstanceOf[Expression],
             children.lift('w).getOrElse(written).asInstanceOf[Expression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('w, written, f('t, `type`, a))

}

case class ConvertedType(val `type`: Expression, val conversion: Expression) extends BitTypeExpression {

    override type SelfTerm = ConvertedType

    override def dependentBitWidth(base: Expression) =
        Application(Application(BitWidth(), `type`), base)
    def copy(`type`: Expression = `type`, conversion: Expression = conversion) =
        ConvertedType(`type`, conversion)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('t).getOrElse(`type`).asInstanceOf[Expression],
             children.lift('c).getOrElse(conversion).asInstanceOf[Expression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('c, conversion, f('t, `type`, a))

}

case class WhereType(val `type`: Expression, val condition: Expression) extends BitTypeExpression {

    override type SelfTerm = WhereType

    override def dependentBitWidth(base: Expression) = {
        val it = VariableIdentity.setName(new VariableIdentity(), 'it)
        Block(Sequence(Let(Variable(it, true), base),
                       Let(BooleanLiteral(true), Application(condition, Variable(it, false)))),
              Application(Application(BitWidth(), `type`), Variable(it, false)))
    }
    def copy(`type`: Expression = `type`, condition: Expression = condition) =
        WhereType(`type`, condition)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('t).getOrElse(`type`).asInstanceOf[Expression],
             children.lift('c).getOrElse(condition).asInstanceOf[Expression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('c, condition, f('t, `type`, a))

}

trait BitTypeInterpretation extends Term { self =>
    override type SelfTerm >: self.type <: BitTypeInterpretation
}
case class EnumValue(val name: Symbol, val value: Expression) extends Statement {
    override type SelfTerm = EnumValue
    def copy(name: Symbol = name, value: Expression = value) =
        EnumValue(name, value)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(value = children.lift('v).getOrElse(value).asInstanceOf[Expression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('v, value, a)
}
case class EnumInterpretation(val body: Statement) extends BitTypeInterpretation {
    override type SelfTerm = EnumInterpretation
    def copy(body: Statement = body) =
        EnumInterpretation(body)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('b).getOrElse(body).asInstanceOf[Statement])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('b, body, a)
}
case class FixedInterpretation(val fixedValue: Expression) extends BitTypeInterpretation {
    override type SelfTerm = FixedInterpretation
    def copy(fixedValue: Expression = fixedValue) =
        FixedInterpretation(fixedValue)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('fv).getOrElse(fixedValue).asInstanceOf[Expression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('fv, fixedValue, a)
}
case class UnitInterpretation(val unit: String) extends BitTypeInterpretation {
    override type SelfTerm = UnitInterpretation
    def copy(unit: String = unit) =
        UnitInterpretation(unit)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy()
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        a
}
case class ContainingInterpretation(val containedType: Expression) extends BitTypeInterpretation {
    override type SelfTerm = ContainingInterpretation
    def copy(containedType: Expression = containedType) =
        ContainingInterpretation(containedType)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('ct).getOrElse(containedType).asInstanceOf[Expression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('ct, containedType, a)
}
case class InterpretedBitType(`type`: Expression, interpretation: BitTypeInterpretation) extends BitTypeExpression {

    override type SelfTerm = InterpretedBitType

    override def dependentBitWidth(base: Expression) =
        Application(Application(BitWidth(), `type`), base)
    def copy(`type`: Expression = `type`, interpretation: BitTypeInterpretation = interpretation) =
        InterpretedBitType(`type`, interpretation)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('t).getOrElse(`type`).asInstanceOf[Expression],
             children.lift('i).getOrElse(interpretation).asInstanceOf[BitTypeInterpretation])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('i, interpretation, f('t, `type`, a))

}
