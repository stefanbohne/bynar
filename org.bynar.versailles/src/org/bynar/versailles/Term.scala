package org.bynar.versailles

import org.apache.commons.lang3.StringEscapeUtils

trait Term extends Annotated { self =>
    
    type SelfTerm <: Term { type SelfTerm <: Term }
    
    def mapWithNames(f: (Symbol, Term) => Term): SelfTerm
    
    def map(f: Term => Term): SelfTerm =
        mapWithNames{ (_, t) => f(t) }    
    
    def copy(children: PartialFunction[Symbol, Term]): SelfTerm =
        mapWithNames{ (n, t) => if (children.isDefinedAt(n)) children(n) else t }
    
    def deepCopy(): SelfTerm =
        map{ _.deepCopy() }
    
}

trait ZeroaryTerm extends Term { self =>

    override def mapWithNames(f: (Symbol, Term) => Term) = this.asInstanceOf[SelfTerm]
    
}

trait Expression extends Term {
    
    type SelfTerm <: Expression { type SelfTerm <: Expression }
    
}
trait ZeroaryExpression extends Expression with ZeroaryTerm
trait Literal extends ZeroaryExpression

trait Statement extends Term {
    
    type SelfTerm <: Statement { type SelfTerm <: Statement }
    
}
trait ZeroaryStatement extends Statement with ZeroaryTerm

case class IntegerLiteral(val value: BigInt) extends Literal {
    override def toString = value.toString
}
case class StringLiteral(val value: String) extends Literal {
    override def toString = "\"" + StringEscapeUtils.escapeJava(value) + "\""
}
case class BooleanLiteral(val value: Boolean) extends Literal {
    override def toString = value.toString
}

case class OrElse() extends Literal {
    override def toString = "`|`"
}
case class Reverse() extends Literal {
    override def toString = "`~`"
}
case class Forget() extends Literal {
    override def toString = "forget"
}
case class Identity() extends Literal {
    override def toString = "identity"
}
case class Undefined() extends Literal {
    override def toString = "undefined"
}
case class Plus() extends Literal {
    override def toString = "`+`"
}
case class Minus() extends Literal {
    override def toString = "`-`"
}
case class Times() extends Literal {
    override def toString = "`*`"
}
case class Divide() extends Literal {
    override def toString = "`/`"
}
case class Equals() extends Literal {
    override def toString = "`==`"
}

case class Fail() extends ZeroaryStatement {
    override def toString = "fail"
}

trait JanusClass extends Literal {
    def reverse: JanusClass
}
case class Irreversible() extends JanusClass {
    override def toString = "->"
    def reverse = ReverseIrreversible().copyAnnotationsFrom(this)
}
case class ReverseIrreversible() extends JanusClass {
    override def toString = "<-"
    def reverse = Irreversible().copyAnnotationsFrom(this)
}
case class Reversible() extends JanusClass {
    override def toString = "<>-<>"
    def reverse = this
}
case class PseudoInverse() extends JanusClass {
    override def toString = ">-<"
    def reverse = this
}
case class SemiInverse() extends JanusClass {
    override def toString = ">->"
    def reverse = ReverseSemiInverse().copyAnnotationsFrom(this)
}
case class ReverseSemiInverse() extends JanusClass {
    override def toString = "<-<"
    def reverse = SemiInverse().copyAnnotationsFrom(this)
}
case class Inverse() extends JanusClass {
    override def toString = "<->"
    def reverse = this
}

class VariableIdentity extends Annotated
object VariableIdentity {
    val originalName = new AnnotationKey[String]
}

case class Variable(val variable: VariableIdentity,
                    val linear: Boolean) extends ZeroaryExpression {
    
    type SelfTerm = Variable
    
}

case class Application(val function: Expression,
                       val argument: Expression) extends Expression {
    
    type SelfTerm = Application
    
    def mapWithNames(f: (Symbol, Term) => Term) =
        Application(f('f, function).asInstanceOf[Expression], 
                    f('a, argument).asInstanceOf[Expression]).copyAnnotationsFrom(this)
    
}

case class Lambda(val janusClass: Expression,
                  val pattern: Expression,
                  val body: Expression) extends Expression {
    
    type SelfTerm = Lambda
    
    def mapWithNames(f: (Symbol, Term) => Term) =
        Lambda(f('jc, janusClass).asInstanceOf[Expression],
               f('p, pattern).asInstanceOf[Expression],
               f('b, body).asInstanceOf[Expression])
               
}

case class Tuple(val components: Expression*) extends Expression {
    
    type SelfTerm = Tuple
    
    def mapWithNames(f: (Symbol, Term) => Term) =
        Tuple(components.zipWithIndex.map{ 
            case (c, i) => f(Symbol(i.toString), c).asInstanceOf[Expression] 
        }:_*).copyAnnotationsFrom(this)
        
}

case class Block(val block: Statement, val scope: Expression) extends Expression {
    
    type SelfTerm = Block
    
    def mapWithNames(f: (Symbol, Term) => Term) =
        Block(f('b, block).asInstanceOf[Statement], 
              f('s, scope).asInstanceOf[Expression]).copyAnnotationsFrom(this)
        
}

case class Sequence(val statements: Statement*) extends Statement {
    
    type SelfTerm = Sequence
    
    def mapWithNames(f: (Symbol, Term) => Term) =
        Sequence(statements.zipWithIndex.map{ 
            case (s, i) => f(Symbol(i.toString), s).asInstanceOf[Statement] 
        }:_*).copyAnnotationsFrom(this)
    
}

case class Let(val pattern: Expression, val value: Expression) extends Statement {
    
    type SelfTerm = Let
    
    def mapWithNames(f: (Symbol, Term) => Term) =
        Let(f('p, pattern).asInstanceOf[Expression],
            f('v, value).asInstanceOf[Expression]).copyAnnotationsFrom(this)
            
}

case class IfStmt(val condition: Expression, val then: Statement, val `else`: Statement, val assertion: Expression) extends Statement {
    
    type SelfTerm = IfStmt
    
    def mapWithNames(f: (Symbol, Term) => Term) =
        IfStmt(f('c, condition).asInstanceOf[Expression],
               f('t, then).asInstanceOf[Statement],
               f('e, `else`).asInstanceOf[Statement],
               f('a, assertion).asInstanceOf[Expression]).copyAnnotationsFrom(this)
    
}