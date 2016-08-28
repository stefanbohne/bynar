package org.bynar.versailles

import org.apache.commons.lang3.StringEscapeUtils

trait Term extends Annotated { self =>

    type SelfTerm >: self.type <: Term

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

    override type SelfTerm >: self.type <: ZeroaryTerm
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) = a
    def copy(): SelfTerm = copy(Map.empty)
    override def copy(children: PartialFunction[Symbol, Term]) = this

}

trait Expression extends Term { self =>
    override type SelfTerm >: self.type <: Expression
}
trait ZeroaryExpression extends Expression with ZeroaryTerm { self =>
    override type SelfTerm >: self.type <: ZeroaryExpression
}
trait Literal extends ZeroaryExpression { self =>
    override type SelfTerm >: self.type <: Literal
}

trait Statement extends Term { self =>
    override type SelfTerm >: self.type <: Statement
}
trait ZeroaryStatement extends Statement with ZeroaryTerm { self =>
    override type SelfTerm >: self.type <: ZeroaryStatement
}

case class NumberLiteral(val value: BigDecimal) extends Literal {
    type SelfTerm = NumberLiteral
    override def toString = value.toString
    def copy(value: BigDecimal = value) =
        NumberLiteral(value).copyAnnotationsFrom(this)
}
case class StringLiteral(val value: String) extends Literal {
    type SelfTerm = StringLiteral
    override def toString = "\"" + StringEscapeUtils.escapeJava(value) + "\""
    def copy(value: String = value) =
        StringLiteral(value).copyAnnotationsFrom(this)
}
case class BooleanLiteral(val value: Boolean) extends Literal {
    type SelfTerm = BooleanLiteral
    override def toString = value.toString
    def copy(value: Boolean = value) =
        BooleanLiteral(value).copyAnnotationsFrom(this)
}

case class Janus() extends Literal {
    type SelfTerm = Janus
    override def toString = "janus"
}
case class OrElse() extends Literal {
    type SelfTerm = OrElse
    override def toString = "`|`"
}
case class OrElseValue(first: Expression, second: Expression) extends Expression {
    type SelfTerm = OrElseValue
    override def toString = s"or_else($first, $second)"
    def copy(first: Expression = first, second: Expression = second) =
        OrElseValue(first, second).copyAnnotationsFrom(this)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('f).getOrElse(first).asInstanceOf[Expression],
             children.lift('s).getOrElse(second).asInstanceOf[Expression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('s, second, f('f, first, a))
}
case class Reverse() extends Literal {
    type SelfTerm = Reverse
    override def toString = "`~`"
}
case class Fix() extends Literal {
    type SelfTerm = Fix
    override def toString = "fix"
}
case class Forget() extends Literal {
    type SelfTerm = Forget
    override def toString = "forget"
}
case class Identity() extends Literal {
    type SelfTerm = Identity
    override def toString = "identity"
}
case class Undefined() extends Literal {
    type SelfTerm = Undefined
    override def toString = "undefined"
}
case class Plus() extends Literal {
    type SelfTerm = Plus
    override def toString = "`+`"
}
case class Minus() extends Literal {
    type SelfTerm = Minus
    override def toString = "`-`"
}
case class Times() extends Literal {
    type SelfTerm = Times
    override def toString = "`*`"
}
case class Divide() extends Literal {
    type SelfTerm = Divide
    override def toString = "`/`"
}
case class IntegerDivide() extends Literal {
    type SelfTerm = IntegerDivide
    override def toString = "div"
}
case class Power() extends Literal {
    type SelfTerm = Power
    override def toString = "`^`"
}
case class Concat() extends Literal {
    type SelfTerm = Concat
    override def toString = "`++`"
}
case class Equals() extends Literal {
    type SelfTerm = Equals
    override def toString = "`==`"
}
case class NotEquals() extends Literal {
    type SelfTerm = NotEquals
    override def toString = "`!=`"
}
case class Less() extends Literal {
    type SelfTerm = Less
    override def toString = "`<`"
}
case class LessOrEquals() extends Literal {
    type SelfTerm = LessOrEquals
    override def toString = "`<=`"
}
case class Greater() extends Literal {
    type SelfTerm = Greater
    override def toString = "`>`"
}
case class GreaterOrEquals() extends Literal {
    type SelfTerm = GreaterOrEquals
    override def toString = "`>=`"
}
case class And() extends Literal {
    type SelfTerm = And
    override def toString = "`&&`"
}
case class Or() extends Literal {
    type SelfTerm = Or
    override def toString = "`||`"
}
case class Not() extends Literal {
    type SelfTerm = Not
    override def toString = "`!`"
}

case class Index() extends Literal {
    type SelfTerm = Index
    override def toString = "index"
}
case class SingletonIndex() extends Literal {
    type SelfTerm = SingletonIndex
    override def toString = "singleton_index"
}
case class RangeIndex() extends Literal {
    type SelfTerm = RangeIndex
    override def toString = "range_index"
}
case class ConcatIndex() extends Literal {
    type SelfTerm = ConcatIndex
    override def toString = "concat_index"
}
case class InfiniteIndex() extends Literal {
    type SelfTerm = InfiniteIndex
    override def toString = "infinite_index"
}
case class IndexMultiply() extends Literal {
    type SelfTerm = IndexMultiply
    override def toString = "index_multiply"
}

case class Fail() extends ZeroaryStatement {
    type SelfTerm = Fail
    override def toString = "fail"
}

trait JanusClass extends Literal { self =>
    type SelfTerm >: self.type <: JanusClass
    def reverse: JanusClass
    def < (that: JanusClass): Boolean
    def <= (that: JanusClass): Boolean =
        that == this || that < this
}
case class Irreversible() extends JanusClass {
    type SelfTerm = Irreversible
    override def toString = "->"
    def reverse = ReverseIrreversible().copyAnnotationsFrom(this)
    def < (that: JanusClass) =
        that <= Reversible()
}
case class ReverseIrreversible() extends JanusClass {
    type SelfTerm = ReverseIrreversible
    override def toString = "<-"
    def reverse = Irreversible().copyAnnotationsFrom(this)
    def < (that: JanusClass) =
        that <= Reversible()
}
case class Reversible() extends JanusClass {
    type SelfTerm = Reversible
    override def toString = "<>-<>"
    def reverse = this
    def < (that: JanusClass) =
        that <= PseudoInverse()
}
case class PseudoInverse() extends JanusClass {
    type SelfTerm = PseudoInverse
    override def toString = ">-<"
    def reverse = this
    def < (that: JanusClass) =
        that <= SemiInverse() || that <= ReverseSemiInverse()
}
case class SemiInverse() extends JanusClass {
    type SelfTerm = SemiInverse
    override def toString = ">->"
    def reverse = ReverseSemiInverse().copyAnnotationsFrom(this)
    def < (that: JanusClass) =
        that <= Inverse()
}
case class ReverseSemiInverse() extends JanusClass {
    type SelfTerm = ReverseSemiInverse
    override def toString = "<-<"
    def reverse = SemiInverse().copyAnnotationsFrom(this)
    def < (that: JanusClass) =
        that <= Inverse()
}
case class Inverse() extends JanusClass {
    type SelfTerm = Inverse
    override def toString = "<->"
    def reverse = this
    def < (that: JanusClass) =
        false
}

class VariableIdentity extends Annotated {
    override def toString = VariableIdentity.getName(this) + "@" + hashCode.toHexString
    def copy(): VariableIdentity =
        new VariableIdentity().copyAnnotationsFrom(this)
}
object VariableIdentity {
    val name = new AnnotationKey[Symbol]
    def getName(it: VariableIdentity): Symbol =
        it.annotation(name).getOrElse(Symbol(""))
    def setName(it: VariableIdentity, name: Symbol): VariableIdentity =
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

case class Member(val name: Symbol) extends Literal {

    override type SelfTerm = Member
    override def toString = s"_.${name.name}"

    def copy(name: Symbol = name) =
        Member(name).copyAnnotationsFrom(this)

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

case class Def(val identity: VariableIdentity, val value: Expression) extends Statement {

    override type SelfTerm = Def

    def copy(identity: VariableIdentity = identity,
             value: Expression = value) =
        Def(identity, value).copyAnnotationsFrom(this)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(value = children.lift('v).getOrElse(value).asInstanceOf[Expression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('v, value, a)

}

case class IfStmt(val condition: Expression, val `then`: Statement, val `else`: Statement, val assertion: Expression) extends Statement {

    type SelfTerm = IfStmt

    def copy(condition: Expression = condition,
             `then`: Statement = `then`,
             `else`: Statement = `else`,
             assertion: Expression = assertion) =
        IfStmt(condition, `then`, `else`, assertion).copyAnnotationsFrom(this)
    override def copy(children: PartialFunction[Symbol, Term]) =
        copy(children.lift('c).getOrElse(condition).asInstanceOf[Expression],
             children.lift('t).getOrElse(`then`).asInstanceOf[Statement],
             children.lift('e).getOrElse(`else`).asInstanceOf[Statement],
             children.lift('a).getOrElse(assertion).asInstanceOf[Expression])
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        f('a, assertion, f('e, `else`, f('t, `then`, f('c, condition, a))))

}

case class Typed() extends Literal {
    override def toString = "typed"
}
case class NumberType() extends Literal {
    override def toString = "Number"
}
case class StringType() extends Literal {
    override def toString = "String"
}
case class BooleanType() extends Literal {
    override def toString = "Boolean"
}

case class TupleType(val componentTypes: Expression*) extends Expression {

    type SelfTerm = TupleType

    def copy(componentTypes: Expression*) =
        TupleType(componentTypes:_*).copyAnnotationsFrom(this)
    override def foldWithNames[T](a: T)(f: (Symbol, Term, T) => T) =
        (a /: componentTypes.zipWithIndex){
        case (a, (ct, i)) => f(Symbol(i.toString), ct, a)
        }
    def copy(children: PartialFunction[Symbol, Term]) =
        copy(componentTypes.zipWithIndex.map{
            case (ct, i) => children.lift(Symbol(i.toString)).getOrElse(ct).asInstanceOf[Expression]
        }:_*)

}

