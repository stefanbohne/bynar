package org.bynar.versailles

import org.apache.commons.lang3.StringEscapeUtils

trait Term extends Annotated { self =>
    
    type SelfTerm <: Term { type SelfTerm <: Term }
    
    def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T): T
    def copy(children: PartialFunction[Symbol, Term]): SelfTerm

    def mapWithNames(f: (Symbol, Term) => Term): SelfTerm =
        copy(foldWithNames[Map[Symbol, Term]](Map()){
            case (n, t, cs) => cs + (n -> f(n, t)) 
        })
    
    def map(f: Term => Term): SelfTerm =
        mapWithNames{ (_, t) => f(t) }    
    
    lazy val children: Map[Symbol, Term] =
        foldWithNames[Map[Symbol, Term]](Map()){
            case (n, t, cs) => cs + (n -> t) 
        }
    
    def deepCopy(): SelfTerm =
        map{ _.deepCopy() }
    
}

trait ZeroaryTerm extends Term { self =>

    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) = a 
    override def copy(children: PartialFunction[Symbol, Term]) = this.asInstanceOf[SelfTerm]
    
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
    def copy(value: BigInt = value) = 
        IntegerLiteral(value).copyAnnotationsFrom(this)
}
case class StringLiteral(val value: String) extends Literal {
    override def toString = "\"" + StringEscapeUtils.escapeJava(value) + "\""
    def copy(value: String = value) = 
        StringLiteral(value).copyAnnotationsFrom(this)
}
case class BooleanLiteral(val value: Boolean) extends Literal {
    override def toString = value.toString
    def copy(value: Boolean = value) = 
        BooleanLiteral(value).copyAnnotationsFrom(this)
}

case class Janus() extends Literal {
    override def toString = "janus"
}
case class OrElse() extends Literal {
    override def toString = "`|`"
}
case class Reverse() extends Literal {
    override def toString = "`~`"
}
case class Fix() extends Literal {
    override def toString = "fix"
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
case class IntegerDivide() extends Literal {
    override def toString = "div"
}
case class Equals() extends Literal {
    override def toString = "`==`"
}

case class Fail() extends ZeroaryStatement {
    override def toString = "fail"
}

trait JanusClass extends Literal {
    def reverse: JanusClass
    def < (that: JanusClass): Boolean
    def <= (that: JanusClass): Boolean =
        that == this || that < this
}
case class Irreversible() extends JanusClass {
    override def toString = "->"
    def reverse = ReverseIrreversible().copyAnnotationsFrom(this)
    def < (that: JanusClass) =
        that <= Reversible()
}
case class ReverseIrreversible() extends JanusClass {
    override def toString = "<-"
    def reverse = Irreversible().copyAnnotationsFrom(this)
    def < (that: JanusClass) =
        that <= Reversible()
}
case class Reversible() extends JanusClass {
    override def toString = "<>-<>"
    def reverse = this
    def < (that: JanusClass) =
        that <= PseudoInverse()
}
case class PseudoInverse() extends JanusClass {
    override def toString = ">-<"
    def reverse = this
    def < (that: JanusClass) =
        that <= SemiInverse() || that <= ReverseSemiInverse()
}
case class SemiInverse() extends JanusClass {
    override def toString = ">->"
    def reverse = ReverseSemiInverse().copyAnnotationsFrom(this)
    def < (that: JanusClass) =
        that <= Inverse()
}
case class ReverseSemiInverse() extends JanusClass {
    override def toString = "<-<"
    def reverse = SemiInverse().copyAnnotationsFrom(this)
    def < (that: JanusClass) =
        that <= Inverse()
}
case class Inverse() extends JanusClass {
    override def toString = "<->"
    def reverse = this
    def < (that: JanusClass) =  
        false
}

class VariableIdentity extends Annotated {
    override def toString = VariableIdentity.getName(this) + "@" + hashCode.toHexString
}
object VariableIdentity {
    val name = new AnnotationKey[String]
    def getName(it: VariableIdentity): String =
        it.annotation(name).getOrElse("")
    def setName(it: VariableIdentity, name: String): VariableIdentity =
        it.putAnnotation(this.name, name)
}

case class Variable(val variable: VariableIdentity,
                    val linear: Boolean) extends ZeroaryExpression {
    
    type SelfTerm = Variable
    
    def copy(variable: VariableIdentity = variable, 
             linear: Boolean = linear) = 
        Variable(variable, linear).copyAnnotationsFrom(this)
}

case class Application(val function: Expression,
                       val argument: Expression) extends Expression {
    
    type SelfTerm = Application
    
    def copy(function: Expression = function,
             argument: Expression = argument) = 
         Application(function, argument).copyAnnotationsFrom(this)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('f).getOrElse(function).asInstanceOf[Expression],
             children.lift('a).getOrElse(argument).asInstanceOf[Expression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) = 
        f('a, argument, f('f, function, a))
    
}

case class Lambda(val janusClass: Expression,
                  val pattern: Expression,
                  val body: Expression) extends Expression {
    
    type SelfTerm = Lambda

    def copy(janusClass: Expression = janusClass,
             pattern: Expression = pattern,
             body: Expression = body) = 
        Lambda(janusClass, pattern, body).copyAnnotationsFrom(this)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('jc).getOrElse(janusClass).asInstanceOf[Expression],
             children.lift('p).getOrElse(pattern).asInstanceOf[Expression],
             children.lift('b).getOrElse(body).asInstanceOf[Expression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) = 
        f('b, body, f('p, pattern, f('jc, janusClass, a)))
               
}

case class Tuple(val components: Expression*) extends Expression {
    
    type SelfTerm = Tuple
    
    def copy(components: Expression*) =
        Tuple(components:_*).copyAnnotationsFrom(this)
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        (a /: components.zipWithIndex){
        case (a, (c, i)) => f(Symbol(i.toString), c, a)    
        }
    def copy(children: PartialFunction[Symbol, Term]) =
        copy(components.zipWithIndex.map{ 
            case (c, i) => children.lift(Symbol(i.toString)).getOrElse(c).asInstanceOf[Expression] 
        }:_*)
        
}

case class Block(val block: Statement, val scope: Expression) extends Expression {
    
    type SelfTerm = Block
    
    def copy(block: Statement = block, scope: Expression = scope) =
        Block(block, scope).copyAnnotationsFrom(this)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('b).getOrElse(block).asInstanceOf[Statement],
             children.lift('s).getOrElse(scope).asInstanceOf[Expression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) = 
        f('s, scope, f('b, block, a))
        
}

case class Sequence(val statements: Statement*) extends Statement {
    
    type SelfTerm = Sequence
    
    def copy(statements: Statement*) =
        Sequence(statements:_*).copyAnnotationsFrom(this)
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        (a /: statements.zipWithIndex){
        case (a, (s, i)) => f(Symbol(i.toString), s, a)    
        }
    def copy(children: PartialFunction[Symbol, Term]) =
        copy(statements.zipWithIndex.map{ 
            case (s, i) => children.lift(Symbol(i.toString)).getOrElse(s).asInstanceOf[Statement] 
        }:_*)
    
}

case class Let(val pattern: Expression, val value: Expression) extends Statement {
    
    type SelfTerm = Let
    
    def copy(pattern: Expression = pattern, value: Expression = value) =
        Let(pattern, value).copyAnnotationsFrom(this)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('p).getOrElse(pattern).asInstanceOf[Expression],
             children.lift('v).getOrElse(value).asInstanceOf[Expression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) = 
        f('v, value, f('p, pattern, a))
            
}

case class IfStmt(val condition: Expression, val then: Statement, val `else`: Statement, val assertion: Expression) extends Statement {
    
    type SelfTerm = IfStmt
    
    def copy(condition: Expression = condition, 
             then: Statement = then,
             `else`: Statement = `else`,
             assertion: Expression = assertion) =
        IfStmt(condition, then, `else`, assertion).copyAnnotationsFrom(this)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('c).getOrElse(condition).asInstanceOf[Expression],
             children.lift('t).getOrElse(then).asInstanceOf[Statement],
             children.lift('e).getOrElse(`else`).asInstanceOf[Statement],
             children.lift('a).getOrElse(assertion).asInstanceOf[Expression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) = 
        f('a, assertion, f('e, `else`, f('t, then, f('c, condition, a))))
    
}