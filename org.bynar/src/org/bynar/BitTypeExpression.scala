package org.bynar

import org.bynar.versailles.Term
import org.bynar.versailles.Expression
import org.bynar.versailles.Statement
import org.bynar.versailles.NumberLiteral
import org.bynar.versailles.Application
import org.bynar.versailles.Plus
import org.bynar.versailles.Member
import org.bynar.versailles.Literal

trait BitTypeExpression extends Expression {

    def dependentBitWidth(base: Expression): Expression

}

case class BitFieldType(val bitWidth: Expression) extends BitTypeExpression {

    override type SelfTerm = BitFieldType

    override def dependentBitWidth(base: Expression) = bitWidth
    override def copy(children: PartialFunction[Symbol, Term]) =
        BitFieldType(children.lift('bw).getOrElse(bitWidth).asInstanceOf[Expression]).copyAnnotationsFrom(this)
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
        copy(`type` = children.lift('t).getOrElse(`type`).asInstanceOf[BitTypeExpression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('t, `type`, a)

}
case class BitRecordType(val components: BitRecordComponent*) extends BitTypeExpression {

    override type SelfTerm = BitRecordType

    override def dependentBitWidth(base: Expression) =
        ((NumberLiteral(0): Expression) /: components){
        case (sum, c) => Application(Application(Plus(), sum),
                Application(Application(BitWidth(), c.`type`), Member(base, c.name)))
        }
    def copy(components: BitRecordComponent*) =
        BitRecordType(components:_*).copyAnnotationsFrom(this)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(components.zipWithIndex.map{ case (c, i) => children.lift(Symbol(i.toString)).getOrElse(c).asInstanceOf[BitRecordComponent] }:_*)
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        (a /: components.zipWithIndex) {
        case (a, (c, i)) => f(Symbol(i.toString), c, a)
        }
}

