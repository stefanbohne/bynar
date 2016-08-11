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
import org.bynar.versailles.Lambda
import org.bynar.versailles.Irreversible
import org.bynar.versailles.OrElse

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

case class MemberContextedType(val path: Seq[Symbol]) extends Literal {
    override type SelfTerm = MemberContextedType

    def copy(path: Seq[Symbol] = path) =
        MemberContextedType(path).copyAnnotationsFrom(this)
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

    def foldComponents[T](a: T)(f: (BitRecordComponent, T) => T): T = {
        def foldStatements(a: T, s: Statement): T =
            s match {
            case s@Sequence(ss@_*) =>
                (a /: ss)(foldStatements _)
            case s: BitRecordComponent =>
                f(s, a)
            case s => a
            }
        foldStatements(a, body)
    }
    def mapComponents(f: BitRecordComponent => Statement): Statement = {
        def mapStatements(s: Statement): Statement =
            s match {
            case s@Sequence(ss@_*) =>
                s.copy(ss.map{ mapStatements(_) }:_*)
            case s: BitRecordComponent =>
                f(s)
            case s => s
            }
        mapStatements(body)
    }
    lazy val components: Seq[BitRecordComponent] =
        foldComponents[Seq[BitRecordComponent]](Seq()){ case (brc, s) => s :+ brc }

    override def dependentBitWidth(base: Expression) = {
        val result0 = VariableIdentity.setName(new VariableIdentity(), 'result)
        var result = result0
        val newBody = mapComponents{
            case BitRecordComponent(n, t) =>
                val result1 = result
                result = VariableIdentity.setName(new VariableIdentity(), 'result)
                Let(Variable(result, true),
                    Application(Application(Plus(),
                        Application(Application(BitWidth(), t.copy(Map())), base)),
                        Variable(result1, true)))
        }
        Block(Sequence(Let(Variable(result0, true), NumberLiteral(0)),
                       newBody),
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

    def foldVariants[T](a: T)(f: (BitUnionVariant, T) => T): T = {
        def foldStatements(a: T, s: Statement): T =
            s match {
            case s@Sequence(ss@_*) =>
                (a /: ss)(foldStatements _)
            case s: BitUnionVariant =>
                f(s, a)
            case s => a
            }
        foldStatements(a, body)
    }
    def mapVariants(f: BitUnionVariant => Statement): Statement = {
        def mapStatements(s: Statement): Statement =
            s match {
            case s@Sequence(ss@_*) =>
                s.copy(ss.map{ mapStatements(_) }:_*)
            case s: BitUnionVariant =>
                f(s)
            case s => s
            }
        mapStatements(body)
    }
    lazy val variants: Seq[BitUnionVariant] =
        foldVariants[Seq[BitUnionVariant]](Seq()){ case (buv, s) => s :+ buv }

    override def dependentBitWidth(base: Expression) = {
        val result0 = VariableIdentity.setName(new VariableIdentity(), 'result)
        var result = result0
        val newBody = mapVariants{
            case BitUnionVariant(n, t) =>
                val result1 = result
                result = VariableIdentity.setName(new VariableIdentity(), 'result)
                val arg = VariableIdentity.setName(new VariableIdentity(), 'it)
                Let(Variable(result, true),
                    Application(Application(OrElse(), Variable(result1, false)),
                                Lambda(Irreversible(),
                                       Variable(arg, true),
                                       Block(Let(Application(Application(Member(n), BitUnionType.this), Variable(VariableIdentity.setName(new VariableIdentity(), '_), true)),
                                                 Variable(arg, false)),
                                             Application(Application(BitWidth(), t.copy(Map())), Variable(arg, false))))))
            }
        Block(Sequence(Let(Variable(result0, true), Lambda(Irreversible(), Variable(VariableIdentity.setName(new VariableIdentity(), '_), true), Undefined())),
                       newBody),
              Application(Variable(result, false), base).
                      putAnnotation(org.bynar.versailles.PrettyPrinter.applicationInfo,
                                    org.bynar.versailles.PrettyPrinter.ApplicationAsMatch))
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
        WrittenType(`type`, written).copyAnnotationsFrom(this)
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
        ConvertedType(`type`, conversion).copyAnnotationsFrom(this)
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
        WhereType(`type`, condition).copyAnnotationsFrom(this)
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
        EnumValue(name, value).copyAnnotationsFrom(this)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(value = children.lift('v).getOrElse(value).asInstanceOf[Expression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('v, value, a)
}
case class EnumInterpretation(val body: Statement) extends BitTypeInterpretation {
    override type SelfTerm = EnumInterpretation

    def foldValues[T](a: T)(f: (EnumValue, T) => T): T = {
        def foldStatements(a: T, s: Statement): T =
            s match {
            case s@Sequence(ss@_*) =>
                (a /: ss)(foldStatements _)
            case s: EnumValue =>
                f(s, a)
            case s => a
            }
        foldStatements(a, body)
    }
    def mapValues(f: EnumValue => Statement): Statement = {
        def mapStatements(s: Statement): Statement =
            s match {
            case s@Sequence(ss@_*) =>
                s.copy(ss.map{ mapStatements(_) }:_*)
            case s: EnumValue =>
                f(s)
            case s => s
            }
        mapStatements(body)
    }
    lazy val values: Seq[EnumValue] =
        foldValues[Seq[EnumValue]](Seq()){ case (ev, s) => s :+ ev }

    def copy(body: Statement = body) =
        EnumInterpretation(body).copyAnnotationsFrom(this)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('b).getOrElse(body).asInstanceOf[Statement])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('b, body, a)
}
case class FixedInterpretation(val fixedValue: Expression) extends BitTypeInterpretation {
    override type SelfTerm = FixedInterpretation
    def copy(fixedValue: Expression = fixedValue) =
        FixedInterpretation(fixedValue).copyAnnotationsFrom(this)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('fv).getOrElse(fixedValue).asInstanceOf[Expression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('fv, fixedValue, a)
}
case class UnitInterpretation(val unit: String) extends BitTypeInterpretation {
    override type SelfTerm = UnitInterpretation
    def copy(unit: String = unit) =
        UnitInterpretation(unit).copyAnnotationsFrom(this)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy()
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        a
}
case class ContainingInterpretation(val containedType: Expression) extends BitTypeInterpretation {
    override type SelfTerm = ContainingInterpretation
    def copy(containedType: Expression = containedType) =
        ContainingInterpretation(containedType).copyAnnotationsFrom(this)
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
        InterpretedBitType(`type`, interpretation).copyAnnotationsFrom(this)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('t).getOrElse(`type`).asInstanceOf[Expression],
             children.lift('i).getOrElse(interpretation).asInstanceOf[BitTypeInterpretation])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('i, interpretation, f('t, `type`, a))

}
