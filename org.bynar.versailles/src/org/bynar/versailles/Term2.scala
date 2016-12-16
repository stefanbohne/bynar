package org.bynar.versailles

import scala.language.higherKinds
import org.apache.commons.lang3.StringEscapeUtils

trait Interpreter2 {

    type Value
    type Context
    trait Container[+T] {
        def flatMap[S](f: T => Container[S]): Container[S] =
            Interpreter2.this.flatMap(f)(this)
        def map[S](f: T => S): Container[S] =
            flatMap[S](t => success(f(t)))
        def withFilter(f: T => Boolean): Container[T] =
            for (x <- this) yield
                if (!f(x)) throw new Exception else x                 
    }

    def tupleToValue(components: Value*): Value
    def valueToTuple(value: Value, size: Int): Container[Seq[Value]]
    def numberToValue(number: BigDecimal): Value
    def matchToNumber(number: BigDecimal, value: Value): Container[Unit]
    def stringToValue(string: String): Value
    def matchToString(string: String, value: Value): Container[Unit]
    def booleanToValue(boolean: Boolean): Value
    def matchToBoolean(boolean: Boolean, value: Value): Container[Unit]

    def success[A](a: A): Container[A]
    def flatMap[A, B](f: A => Container[B])(c: Container[A]): Container[B]
    def exchange(identity2: VariableIdentity2, value: Value, context: Context): Container[(Value, Context)]
    def quote(term: Term2): Value
    def unquote(value: Value): Container[Term2]

}

trait Term2 extends Annotated {

    def reverse: Term2
    def execute(i: Interpreter2)(value: i.Value, context: i.Context): i.Container[(i.Value, i.Context)]

}

class VariableIdentity2(val name: String) extends Annotated {
    override def toString = s"${name}_${hashCode.toHexString}"
}
case class Variable2(val identity: VariableIdentity2) extends Term2 {

    override def toString = identity.toString

    def reverse = this

    def execute(i: Interpreter2)(value: i.Value, context: i.Context) =
        i.exchange(identity, value, context)

}

case class NumberLiteral2(val value: BigDecimal) extends Term2 {

    override def toString = value.toString

    def reverse = new Term2 {
        def reverse = NumberLiteral2.this
        def execute(i: Interpreter2)(value: i.Value, context: i.Context) =
            for (() <- i.matchToNumber(NumberLiteral2.this.value, value))
                yield (i.tupleToValue(), context)
    }
    def execute(i: Interpreter2)(value: i.Value, context: i.Context) =
        for (_ <- i.valueToTuple(value, 0))
            yield (i.numberToValue(this.value), context)

}

case class StringLiteral2(val value: String) extends Term2 {

    override def toString = "\"" + StringEscapeUtils.escapeJava(value) + "\""

    def reverse = new Term2 {
        def reverse = StringLiteral2.this
        def execute(i: Interpreter2)(value: i.Value, context: i.Context) =
            for (() <- i.matchToString(StringLiteral2.this.value, value))
                yield (i.tupleToValue(), context)
    }
    def execute(i: Interpreter2)(value: i.Value, context: i.Context) =
        for (_ <- i.valueToTuple(value, 0))
            yield (i.stringToValue(this.value), context)

}

case class BooleanLiteral2(val value: Boolean) extends Term2 {

    override def toString = value.toString

    def reverse = new Term2 {
        def reverse = BooleanLiteral2.this
        def execute(i: Interpreter2)(value: i.Value, context: i.Context) =
            for (() <- i.matchToBoolean(BooleanLiteral2.this.value, value))
                yield (i.tupleToValue(), context)
    }
    def execute(i: Interpreter2)(value: i.Value, context: i.Context) =
        for (_ <- i.valueToTuple(value, 0))
            yield (i.booleanToValue(this.value), context)

}

case class Tuple2(val components: Term2*) extends Term2 {
    
    override def toString = components.mkString("(", ", ", ")")
    
    def reverse = new Term2 {
        def reverse = Tuple2.this
        def execute(i: Interpreter2)(value: i.Value, context: i.Context) = { 
            val r3 = for (vs <- i.valueToTuple(value, components.size);
                          r2 <- {
                (components.zip(vs) :\ i.success(context)){ case ((c, v), r) =>
                    for (ctx <- r;
                         (u, ctx2) <- c.reverse.execute(i)(v, ctx);
                         _ <- i.valueToTuple(u, 0)) yield ctx2
                }
            }) yield r2
            for (ctx3 <- r3) yield (i.tupleToValue(), ctx3)
        }
    }
    
    def execute(i: Interpreter2)(value: i.Value, context: i.Context) =
        for (_ <- i.valueToTuple(value, 0);
             r <- {
                val r3 = (i.success(Seq[i.Value](), context) /: components){ case (r2, c) =>
                    for ((vs2, ctx) <- r2;
                         (v2, ctx2) <- c.execute(i)(i.tupleToValue(), ctx)) yield (vs2 :+ v2, ctx2)
                }
                for ((vs3, ctx3) <- r3) yield (i.tupleToValue(vs3:_*), ctx3)
            }) yield r
    
}

case class Quote(val term: Term2) extends Term2 {

    override def toString = s"'$term'"

    def reverse = throw new UnsupportedOperationException
    def execute(i: Interpreter2)(value: i.Value, context: i.Context) =
        i.success(i.quote(term), context)

}

case class Antiquote(val term: Term2) extends Term2 {

    override def toString = s"<$term>"

    def reverse = new Term2 {
        def reverse = Antiquote.this
        def execute(i: Interpreter2)(value: i.Value, context: i.Context) =
            for ((v2, ctx2) <- term.execute(i)(value, context);
             t3 <- i.unquote(v2);
             (v3, ctx3) <- t3.reverse.execute(i)(value, context))
            yield (v3, ctx3)
    }
    def execute(i: Interpreter2)(value: i.Value, context: i.Context) =
        for ((v2, ctx2) <- term.execute(i)(value, context);
             t3 <- i.unquote(v2);
             (v3, ctx3) <- t3.execute(i)(value, context))
            yield (v3, ctx3)

}

case class Compose(val terms: Term2*) extends Term2 {

    override def toString = terms.mkString("(", " ", ")")

    def reverse = Compose(terms.reverse.map{ _.reverse }:_*)
    def execute(i: Interpreter2)(value: i.Value, context: i.Context) =
        (i.success(value, context) /: terms){ (result, t) =>
            for ((v, ctx) <- result;
                 (v2, ctx2) <- t.execute(i)(v, ctx))
                yield (v2, ctx2)
        }

}

class IdentityInterpreter extends Interpreter2
{
    type Value = Term2
    type Context = Term2
    case class Success[+T](value: T) extends Container[T]
    trait Error extends Container[Nothing]
    case class MatchError(term: Term2) extends Error

    def tupleToValue(components: Term2*): Term2 =
        Tuple2(components:_*)        
    def valueToTuple(value: Term2, size: Int): Container[Seq[Term2]] =
        value match {
        case Tuple2(cs@_*) if cs.size == size => success(cs)
        case _ => MatchError(value)
        }
    def numberToValue(number: BigDecimal): Term2 = 
        NumberLiteral2(number)
    def matchToNumber(number: BigDecimal, value: Term2): Container[Unit] =
        value match {
        case NumberLiteral2(number2) if number2 == number => success(())
        case _ => MatchError(value)
        }
    def stringToValue(string: String): Term2 =
        StringLiteral2(string)
    def matchToString(string: String, value: Term2): Container[Unit] =
        value match {
        case StringLiteral2(string2) if string == string2 => success(())
        case _ => MatchError(value)
        }
    def booleanToValue(boolean: Boolean): Term2 =
        BooleanLiteral2(boolean)
    def matchToBoolean(boolean: Boolean, value: Term2): Container[Unit] =
        value match {
        case BooleanLiteral2(boolean2) if boolean2 == boolean => success(())
        case _ => MatchError(value)
    }

    def success[A](a: A): Container[A] = Success(a)
    def flatMap[A, B](f: A => Container[B])(c: Container[A]): Container[B] = c match {
        case Success(a) => f(a)
        case c: Error => c
    }
    def exchange(identity2: VariableIdentity2, value: Value, context: Context): Container[(Value, Context)] =
        success((Variable2(identity2), context))
    def quote(term: Term2): Term2 =
        Quote(term)
    def unquote(value: Term2): Container[Term2] =
        success(Antiquote(value))
}

class EvaluatingInterpreter extends Interpreter2
{
    type Value = Any
    type Context = Map[VariableIdentity2, Any]
    case class Success[+T](value: T) extends Container[T]
    
    def tupleToValue(components: Value*): Value = Seq(components:_*)
    def valueToTuple(value: Value, size: Int): Container[Seq[Value]] = 
        value match {
        case Seq(cs@_*) if cs.size == size => Success(cs)
        }
    def numberToValue(number: BigDecimal): Value = number
    def matchToNumber(number: BigDecimal, value: Value): Container[Unit] =
        value match {
        case `number` => Success(())
        }
    def stringToValue(string: String): Value = string
    def matchToString(string: String, value: Value): Container[Unit] =
        value match {
        case `string` => Success(())
        }
    def booleanToValue(boolean: Boolean): Value = boolean
    def matchToBoolean(boolean: Boolean, value: Value): Container[Unit] = 
        value match {
        case `boolean` => Success(())        
        }

    def success[A](a: A): Container[A] = Success(a)
    def flatMap[A, B](f: A => Container[B])(c: Container[A]): Container[B] = 
        c match {
        case Success(x) => f(x)
        }
    def exchange(identity2: VariableIdentity2, value: Value, context: Context): Container[(Value, Context)] = {
        val v = context.getOrElse(identity2, ())
        Success(v, context + (identity2 -> value))
    }
    def quote(term: Term2): Value = Quote(term)
    def unquote(value: Value): Container[Term2] =
        value match {
        case Quote(t) => Success(t)
        }
}