package org.bynar.versailles

import scala.collection._
import java.math.MathContext
import java.math.RoundingMode

class Simplifier {

    import TermImplicits._

    def defaultContext = org.bynar.versailles.defaultContext

    def isLiteral(expr: Expression): Boolean =
        expr match {
        case expr: Literal => true
        case Tuple(cs@_*) => cs.forall(isLiteral(_))
        case singletonIndex(i) => isLiteral(i)
        case rangeIndex(f, t) => isLiteral(f) && isLiteral(t)
        case infiniteIndex(f) => isLiteral(f)
        case f ++ s => isLiteral(f) && isLiteral(s)
        case _ => false
        }
    def literalLessOrEquals(l1: Expression, l2: Expression): Option[Boolean] =
        (l1, l2) match {
        case (Tuple(cs1@_*), Tuple(cs2@_*)) if cs1.size == cs2.size =>
            ((Some(true): Option[Boolean]) /: cs1.zip(cs2)){
                case (acc, (c1, c2)) =>
                    for (acc <- acc;
                         le <- literalLessOrEquals(c1, c2))
                        yield acc && le
            }
        case (NumberLiteral(v1), NumberLiteral(v2)) =>
            Some(v1 <= v2)
        case (StringLiteral(v1), StringLiteral(v2)) =>
            Some(v1 <= v2)
        case (BooleanLiteral(v1), BooleanLiteral(v2)) =>
            Some(v1 <= v2)
        case _ =>
            None
    }
    def isDefined(expr: Expression): Boolean =
        expr match {
        case Undefined() => false
        case expr: Literal => true
        case expr: Tuple => expr.components.forall{ isDefined(_) }
        case expr: Lambda => true
        case Application(InfiniteIndex(), a) => isDefined(a)
        case Application(Application(RangeIndex(), a), b) => isDefined(a) && isDefined(b)
        case Application(Application(IndexConcatenation(), a), b) => isDefined(a) && isDefined(b)
        case _ => false
        }

    def freshVariables(term: Term): Term = {
        val cache: mutable.Map[VariableIdentity, VariableIdentity] = mutable.Map()
        def doIt(term: Term): Term =
            term match {
            case v@Variable(id, true) =>
                v.copy(variable=cache.getOrElseUpdate(id, id.copy()))
            case v@Variable(id, false) if cache.contains(id) =>
                v.copy(variable=cache(id))
            case term =>
                term
            }
        doIt(term.map(doIt _))
    }

    def reverseStatement(stmt: Statement): Statement =
        stmt match {
        case stmt@Let(p, v) => stmt.copy(v, p)
        case stmt@Sequence(ss@_*) => stmt.copy(ss.reverse.map{ reverseStatement(_) }:_*)
        }

    val wasRangeIndexExclusive = new AnnotationKey[Unit]

    def preSimplify(term: Term): Term =
        term
    def postSimplify(term: Term): Term =
        term

    def simplify(expr: Expression, forward: Boolean, context: Map[VariableIdentity, Expression] = defaultContext, leaveDefs: Boolean = false): (Expression, Map[VariableIdentity, Expression]) = {
        val (e, ctx) = simplify1(preSimplify(expr).asInstanceOf[Expression], forward, context, leaveDefs)
        (postSimplify(e).asInstanceOf[Expression], ctx)
    }

    def simplify1(expr: Expression, forward: Boolean, context: Map[VariableIdentity, Expression], leaveDefs: Boolean): (Expression, Map[VariableIdentity, Expression]) =
        expr match {
        case expr@Variable(id, true) =>
            if (forward) (context.getOrElse(id, expr), context)
            else (expr, context - id)
        case expr@Variable(id, false) =>
            (context.getOrElse(id, expr), context)
        case expr@Tuple(cs@_*) =>
            val (cs1, ctx1) = ((Seq[Expression](), context) /: cs){
            case ((cs2, ctx2), c) =>
                val (c2, ctx3) = simplify1(c, forward, ctx2, leaveDefs)
                (cs2 :+ c2, ctx3)
            }
            (expr.copy(cs1:_*), ctx1)
        case expr@Block(b, s) =>
            val (b2, ctx2) = simplifyStatement(b, context, leaveDefs)
            b2 match {
            case Fail() =>
                (Undefined(), ctx2)
            case Sequence() =>
                simplify1(s, forward, ctx2, leaveDefs)
            case b2 =>
                val (s2, ctx3) = simplify1(s, forward, ctx2, leaveDefs)
                (b2, s2) match {
                case (_, s2@Undefined()) => (s2, ctx3)
                case (Let(BooleanLiteral(true), c), v: OrElseValue) =>
                    simplify1(v.copy(expr.copy(b2, v.first), expr.copy(b2.deepCopy(), v.second)), forward, ctx3, leaveDefs)
                case (b2, Block(b3, s2)) =>
                    simplify1(Block(Sequence(b2, b3), s2), forward, context, leaveDefs)
                case (b2, s2) =>
                    (expr.copy(b2, s2), ctx3)
                }                
            }
        case expr@Lambda(Irreversible(), block@Block(ss, s), b) =>
            simplify1(expr.copy(pattern = s, body = block.copy(reverseStatement(ss), b)), forward, context, leaveDefs)
        case expr@Lambda(Irreversible(), app@Application(f, a), b) =>
            val x = VariableIdentity.setName(new VariableIdentity(), 
                        a match {
                        case Variable(id, _) => VariableIdentity.getName(id)
                        case _ => '_
                        })            
            simplify1(expr.copy(pattern = Variable(x, true),
                               body = Block(Let(app, Variable(x, false)), b)),
                     forward,
                     context, 
                     leaveDefs)
        case expr@Lambda(Irreversible(), p, b) =>
            val (p2, ctx2) = simplify1(p, !forward, context, leaveDefs)
            val (b2, ctx3) = simplify1(b, forward, ctx2, leaveDefs)
            (expr.copy(pattern = p2, body = b2), context)
        case expr@Lambda(jc, p, b) =>
            simplify1(Janus()(expr.copy(janusClass = Irreversible()))(
                             expr.copy(janusClass = Irreversible(),
                                       pattern = b,
                                       body = p)),
                     forward,
                     context, 
                     leaveDefs)
        case expr@OrElseValue(a, b) =>
            simplifyAssociative(new Pattern2[Expression, Expression, Expression] { 
                    def unapply(e: Expression) = e match { case OrElseValue(a, b) => Some(a, b) case _ => None }
                    def apply(a: Expression, b: Expression) = OrElseValue(a, b)
                    }, false, false, expr, forward, context, leaveDefs){
            case (a, Undefined()) => Seq(a)
            case (Undefined(), b) => Seq(b)
            case (a, _) if isDefined(a) => Seq(a)
            case (a@Block(Let(BooleanLiteral(true), ac), av), b) if av == b => Seq(a)
            case (a@Block(Let(BooleanLiteral(true), ac), _), Block(Let(BooleanLiteral(true), bc), _)) if ac == bc => 
                Seq(a)            
            case (Block(Let(BooleanLiteral(true), ac), av), Block(Let(BooleanLiteral(true), bc), bv)) if av == bv => 
                Seq(Block(Let(BooleanLiteral(true), ac || bc), av))
            }{ _ => () }

        case expr@Application(f, a) =>
            val (a1, ctx1) = simplify1(a, forward, context, leaveDefs)
            val (f2, ctx2) = simplify1(f, true, ctx1, leaveDefs)
            simplifyApplication(expr.copy(f2, a1), forward, ctx1, leaveDefs)
            
        case expr@Module(s) =>
            val (s1, ctx1) = simplifyStatement(s, context, true)
            (expr.copy(s1), ctx1)
            
        case expr => (expr, context)
        }
    
    trait Pattern2[A, B, C] extends Function2[A, B, C] {
        def unapply(c: C): Option[(A, B)]
    }
    def simplifyAssociative[S](pattern: Pattern2[Expression, Expression, Expression], symmetric: Boolean, reverse: Boolean, expr: Expression, forward: Boolean, context: Map[VariableIdentity, Expression], leaveDefs: Boolean)(simplifier: PartialFunction[(Expression, Expression), Seq[Expression]])(order: Expression => S)(implicit ord: math.Ordering[S]): (Expression, Map[VariableIdentity, Expression]) = {
        val result = mutable.Buffer[Expression]()
        var ctx = context
        def collectAssociativeArguments(expr: Expression): Unit =
            expr match {
            case pattern(a, b) =>
                if (reverse)
                    collectAssociativeArguments(b)
                collectAssociativeArguments(a)
                if (!reverse)
                    collectAssociativeArguments(b)
            case expr => 
                val (e, ctx2) = simplify1(expr, forward, ctx, leaveDefs)
                result += e
                ctx = ctx2
            }
        collectAssociativeArguments(expr)
        var found = false
        var i = 0
        while (i < result.size) {
            var j = if (symmetric) 0 else i + 1
            while (i < result.size && j < result.size && (symmetric || j < i + 2)) { 
                if (i != j)
                    simplifier.lift(result(i), result(j)) match {
                    case Some(m) => 
                        result.remove(Math.max(i, j))
                        result.remove(Math.min(i, j))
                        result.insertAll(Math.min(i, j), m)
                        found = true
                    case None => {}
                    }
                j += 1
            }
            i += 1
        }
        val result2 = if (reverse)
                result.sortBy(order).reduceLeft((a, b) => pattern(b, a))
            else
                result.sortBy(order).reduceLeft((a, b) => pattern(a, b))
        if (!found)
            (result2, ctx)
        else 
            simplify1(result2, forward, ctx, leaveDefs)
    }
        

    def simplifyApplication(app: Application, forward: Boolean, ctx2: Map[VariableIdentity, Expression], leaveDefs: Boolean): (Expression, Map[VariableIdentity, Expression]) =
        app match {
        case Application(f2, a1) =>
            (f2, a1) match {
            case (lam(p, b), a1) =>
                simplify1(Block(Let(p, a1), b), forward, ctx2, leaveDefs)
            case (Reverse(), Reverse()) =>
                (Reverse(), ctx2)
            case (Reverse(), reverse(f)) =>
                (f, ctx2)

//            case (Minus(), r: NumberLiteral) =>
//                (Application(Plus(), r.copy(-r.value)), ctx2)
            case (Minus(), r) =>
                simplify1(Application(Plus(), r * NumberLiteral(-1)), forward, ctx2, leaveDefs)
            case (Reverse(), Application(Plus(), x)) =>
                (Application(Minus(), x), ctx2)
            case (Reverse(), Application(Minus(), x)) =>
                (Application(Plus(), x), ctx2)
//            case (Application(Plus(), NumberLiteral(r)), NumberLiteral(l)) =>
//                (NumberLiteral(l + r), ctx2)
//            case (Application(Plus(), x), NumberLiteral(z)) if z == 0 =>
//                (x, ctx2)
//            case (Application(Plus(), NumberLiteral(z)), x) if z == 0 =>
//                (x, ctx2)
//            case (Application(Plus(), Application(Application(op@Plus(), NumberLiteral(r)), x)), NumberLiteral(l)) =>
//                simplify1(Application(Application(op, NumberLiteral(l + r)), x), forward, ctx2, leaveDefs)
//            case (Application(Plus(), NumberLiteral(r)), Application(Application(op@Plus(), NumberLiteral(l)), x)) =>
//                simplify1(Application(Application(op, NumberLiteral(l + r)), x), forward, ctx2, leaveDefs)
//            case (Application(Plus(), Application(Application(op1@Plus(), NumberLiteral(r)), x)), f3@Application(Application(op2@Plus(), NumberLiteral(l)), y)) =>
//                simplify1(Application(Application(op1, Application(Application(op2, NumberLiteral(l + r)), x)), y), forward, ctx2, leaveDefs)
//            case (Application(op@Plus(), r), l: NumberLiteral) =>
//                simplify1(Application(Application(op, l), r), forward, ctx2, leaveDefs)
//            case (f1@Application(Plus(), x), Application(f3@Application(Plus(), l: NumberLiteral), y)) =>
//                simplify1(Application(f3, Application(f1, y)), forward, ctx2, leaveDefs)
//            case (f1@Application(Plus(), f2@Application(f3@Application(Plus(), r: NumberLiteral), x)), y) =>
//                simplify1(Application(f3, Application(Application(f1.function, x), y)), forward, ctx2, leaveDefs)
//            case (Application(Plus(), x), y) if x == y =>
//                simplify1(x * 2, forward, ctx2, leaveDefs)
//            case (Application(Plus(), x * k), y) if x == y =>
//                simplify1(x * (k + 1), forward, ctx2, leaveDefs)
//            case (Application(Plus(), x), y * k) if x == y =>
//                simplify1(x * (k + 1), forward, ctx2, leaveDefs)
//            case (Application(Plus(), x * k), y * l) if x == y =>
//                simplify1(x * (k + l), forward, ctx2, leaveDefs)

            case (Divide(), l: NumberLiteral) if ((1 / l.value) * l.value == 1) =>
                simplify1(Application(Times(), NumberLiteral(1 / l.value)), forward, ctx2, leaveDefs)
            case (Divide(), l) =>
                simplify1(Application(Times(), Application(Application(Power(), NumberLiteral(-1)), l)), forward, ctx2, leaveDefs)
            case (Reverse(), Application(Times(), x)) =>
                (Application(Divide(), x), ctx2)
            case (Reverse(), Application(Divide(), x)) =>
                (Application(Times(), x), ctx2)
//            case (Application(Times(), NumberLiteral(r)), NumberLiteral(l)) =>
//                (NumberLiteral(l * r), ctx2)
//            case (Application(Times(), x), NumberLiteral(o)) if o == 0 =>
//                (0, ctx2)
//            case (Application(Times(), NumberLiteral(o)), x) if o == 0 =>
//                (0, ctx2)
//            case (Application(Times(), x), NumberLiteral(o)) if o == 1 =>
//                (x, ctx2)
//            case (Application(Times(), NumberLiteral(o)), x) if o == 1 =>
//                (x, ctx2)
//            case (Application(Times(), f1@Application(f2@Application(Times(), NumberLiteral(r)), x)), NumberLiteral(l))
//                if (l * r / r == l && l * r / l == r) =>
//                simplify1(Application(Application(f2.function, NumberLiteral(l * r)), x), forward, ctx2, leaveDefs)
//            case (Application(Times(), NumberLiteral(r)), f1@Application(f2@Application(Times(), NumberLiteral(l)), x))
//                if (l * r / r == l && l * r / l == r) =>
//                simplify1(Application(Application(f2.function, NumberLiteral(l * r)), x), forward, ctx2, leaveDefs)
//            case (Application(Times(), NumberLiteral(r)), f1@Application(f2@Application(Times(), Application(Application(Power(), NumberLiteral(mo)), NumberLiteral(l))), x))
//                if (mo == -1 && (r / l) * l == r && r / (r / l) == l) =>
//                simplify1(Application(Application(f2.function, NumberLiteral(r / l)), x), forward, ctx2, leaveDefs)
//            case (Application(Times(), f1@Application(f2@Application(Times(), NumberLiteral(r)), x)), f3@Application(f4@Application(Times(), NumberLiteral(l)), y)) =>
//                simplify1(Application(Application(f2.function, Application(Application(f4.function, NumberLiteral(l * r)), y)), x), forward, ctx2, leaveDefs)
//            case (f1@Application(Times(), r), l: NumberLiteral) =>
//                simplify1(Application(Application(f1.function, l), r), forward, ctx2, leaveDefs)
//            case (f1@Application(Times(), x), f2@Application(f3@Application(Times(), l: NumberLiteral), y)) =>
//                simplify1(Application(f3, Application(f1, y)), forward, ctx2, leaveDefs)
//            case (f1@Application(Times(), f2@Application(f3@Application(Times(), r: NumberLiteral), x)), y) =>
//                simplify1(Application(f3, Application(Application(f1.function, x), y)), forward, ctx2, leaveDefs)
//            case (f1@Application(Times(), r: NumberLiteral), f2@Application(f3@Application(Plus(), NumberLiteral(l)), x)) =>
//                simplify1(Application(f3.copy(f3.function, NumberLiteral(l * r.value)), Application(f1.copy(f1.function, r), x)), forward, ctx2, leaveDefs)
            case (IntegerDivide(), Tuple(l: NumberLiteral, r: NumberLiteral)) =>
                (NumberLiteral(((l.value - (l.value % r.value)) / r.value)), ctx2)
                
            case (Application(Power(), NumberLiteral(o)), a) if o == 1 =>
                (a, ctx2)
            case (Application(Power(), l), pow(a, r)) =>
                simplify1(pow(a, r * l), forward, ctx2, leaveDefs)

            case (Application(Equals(), l1), l2) if isLiteral(l1) && isLiteral(l2) =>
                (BooleanLiteral(l1 == l2), ctx2)
            case (Application(NotEquals(), l1), l2) if isLiteral(l1) && isLiteral(l2) =>
                (BooleanLiteral(l1 != l2), ctx2)
            case (Application(Less(), l1), l2) if isLiteral(l1) && isLiteral(l2) =>
                (literalLessOrEquals(l1, l2).map{ v => BooleanLiteral(v && l1 != l2) }.getOrElse(Undefined()), ctx2)
            case (Application(LessOrEquals(), l1), l2) if isLiteral(l1) && isLiteral(l2) =>
                (literalLessOrEquals(l1, l2).map{ v => BooleanLiteral(v) }.getOrElse(Undefined()), ctx2)
            case (Application(Greater(), l1), l2) if isLiteral(l1) && isLiteral(l2) =>
                (literalLessOrEquals(l1, l2).map{ v => BooleanLiteral(!v) }.getOrElse(Undefined()), ctx2)
            case (Application(GreaterOrEquals(), l1), l2) if isLiteral(l1) && isLiteral(l2) =>
                (literalLessOrEquals(l1, l2).map{ v => BooleanLiteral(!v || l1 == l2) }.getOrElse(Undefined()), ctx2)
//            case (Application(And(), BooleanLiteral(false)), _) =>
//                (BooleanLiteral(false), ctx2)
//            case (Application(And(), _), BooleanLiteral(false)) =>
//                (BooleanLiteral(false), ctx2)
//            case (Application(And(), a), b) if a == b =>
//                (a, ctx2)
//            case (Application(And(), BooleanLiteral(b1)), BooleanLiteral(b2)) =>
//                (BooleanLiteral(b1 && b2), ctx2)
//            case (Application(And(), a), Application(Application(And(), b), c)) =>
//                (And()(And()(a)(b))(c), ctx2)
//            case (Application(And(), a1 >= NumberLiteral(n1)), a2 < NumberLiteral(n2)) if a1 == a2 && n2 < n1 =>
//                (false, ctx2)
//            case (Application(And(), a1 < NumberLiteral(n1)), a2 >= NumberLiteral(n2)) if a1 == a2 && n1 < n2 =>
//                (false, ctx2)
//            case (Application(Or(), BooleanLiteral(b1)), BooleanLiteral(b2)) =>
//                (BooleanLiteral(b1 || b2), ctx2)
//            case (Application(Or(), BooleanLiteral(true)), _) =>
//                (BooleanLiteral(true), ctx2)
//            case (Application(Or(), _), BooleanLiteral(true)) =>
//                (BooleanLiteral(true), ctx2)
//            case (Application(Or(), a), b) if a == b =>
//                (a, ctx2)
//            case (Application(Or(), a), Application(Application(Or(), b), c)) =>
//                (Or()(Or()(a)(b))(c), ctx2)
            case (and@Application(And(), a), or1@Application(or2@Application(Or(), b), c)) =>
                simplify1(or1.copy(or2.copy(argument = Application(and, b)), Application(and.deepCopy(), c)), forward, ctx2, leaveDefs) 
            case (and@Application(And(), or1@Application(or2@Application(Or(), a), b)), c) =>
                simplify1(or1.copy(or2.copy(argument = and.copy(argument=a)(c)), and.copy(and.function.deepCopy(), b)(c)), forward, ctx2, leaveDefs) 
            case (Not(), BooleanLiteral(b)) =>
                (BooleanLiteral(!b), ctx2)
            case (Not(), a && b) =>
                simplify1(!a || !b, forward, ctx2, leaveDefs)
            case (Not(), a || b) =>
                simplify1(!a && !b, forward, ctx2, leaveDefs)
            case (Not(), a == b) =>
                simplify1(a neq b, forward, ctx2, leaveDefs)
            case (Not(), a != b) =>
                simplify1(a equ b, forward, ctx2, leaveDefs)
            case (Not(), a < b) =>
                simplify1(a >= b, forward, ctx2, leaveDefs)
            case (Not(), a <= b) =>
                simplify1(a > b, forward, ctx2, leaveDefs)
            case (Not(), a > b) =>
                simplify1(a <= b, forward, ctx2, leaveDefs)
            case (Not(), a >= b) =>
                simplify1(a < b, forward, ctx2, leaveDefs)
//            case (Application(And(), a == l1), b == l2) if isLiteral(l1) && isLiteral(l2) && a == b =>
//                (BooleanLiteral(l1 == l2), ctx2)

            case (Application(Concat(), StringLiteral(l1)), StringLiteral(l2)) =>
                (StringLiteral(l1 + l2), ctx2)

            case (Application(Typed(), t), v) =>
                // TODO: proper type check
                (v, ctx2)

            case (Application(IndexConcatenation(), Application(Application(RangeIndex(), NumberLiteral(n1)), NumberLiteral(n2))), n3) if n1 == n2 =>
                (n3, ctx2)
            case (Application(IndexConcatenation(), n1), Application(Application(RangeIndex(), NumberLiteral(n2)), NumberLiteral(n3))) if n2 == n3 =>
                (n1, ctx2)
            case (Application(IndexConcatenation(), rangeIndex(n1, NumberLiteral(i))), r2@Application(r21@Application(RangeIndex(), NumberLiteral(i2)), _)) if i == i2 =>
                (r2.copy(r21.copy(argument=n1)), ctx2)
            case (Application(IndexConcatenation(), r1@Application(Application(IndexConcatenation(), _), rangeIndex(n1, NumberLiteral(i)))), r2@Application(r21@Application(RangeIndex(), NumberLiteral(i2)), _)) if i == i2 =>
                (r1.copy(argument=r2.copy(r21.copy(argument=n1))), ctx2)
            case (Application(IndexConcatenation(), Application(Application(RangeIndex(), n1), NumberLiteral(i))), Application(InfiniteIndex(), NumberLiteral(i2))) if i == i2 =>
                (infiniteIndex(n1), ctx2)
            case (Application(IndexComposition(), infiniteIndex(n1)), infiniteIndex(n2)) =>
                simplify1(infiniteIndex(n2 + n1), forward, ctx2, leaveDefs)
            case (Application(IndexComposition(), singletonIndex(n1)), Application(InfiniteIndex(), n2)) =>
                simplify1(singletonIndex(n2 + n1), forward, ctx2, leaveDefs)
            case (Application(IndexComposition(), singletonIndex(n1)), Application(Application(RangeIndex(), n2), n3)) =>
                simplify1(Block(Let(true, n1.deepCopy() < n3 - n2.deepCopy()),
                               singletonIndex(n2 + n1)), forward, ctx2, leaveDefs)
            case (Application(IndexComposition(), singletonIndex(n1)), n2 ++ n3) =>
                simplify1(OrElseValue(
                    n2.deepCopy() o singletonIndex(n1),
                    n3 o singletonIndex(n1.deepCopy() - length(n2))), forward, ctx2, leaveDefs)
            case (Application(IndexComposition(), r@Application(r2@Application(RangeIndex(), n1), n2)), Application(InfiniteIndex(), n3)) =>
                simplify1(r.copy(r2.copy(argument=n1 + n3.deepCopy()), n2 + n3), forward, ctx2, leaveDefs)
            case (Application(IndexComposition(), Application(InfiniteIndex(), n3)), r@Application(r2@Application(RangeIndex(), n1), n2)) =>
                simplify1(r.copy(r2.copy(argument=n3.deepCopy() + n1), n3 + n2), forward, ctx2, leaveDefs)
            case (Application(IndexComposition(), r@Application(r2@Application(RangeIndex(), n1), n2)), Application(Application(RangeIndex(), n3), n4)) =>
                simplify1(Block(Let(true,
                                n1.deepCopy() <= n4.deepCopy() - n3.deepCopy() &&
                                n2.deepCopy() <= n4 - n3.deepCopy()),
                        r.copy(r2.copy(argument=n1 + n3.deepCopy()), n2 + n3)), forward, ctx2, leaveDefs)
            case (Application(IndexComposition(), r@Application(r2@Application(RangeIndex(), n1), n2)), Application(Application(IndexConcatenation(), n3), n4)) =>
                simplify1({
                    val x = VariableIdentity.setName(new VariableIdentity, 'l)
                    Block(Let(Variable(x, true), length(n3.deepCopy())),
                        (n3 o r.copy(r2.copy(argument=min(n1, Variable(x, false))), min(n2, Variable(x, false)))) ++
                        (n4 o r.copy(r2.copy(argument=max(n1.deepCopy() - Variable(x, false), 0)), max(n2.deepCopy() - Variable(x, false), 0))))
                }, forward, ctx2, leaveDefs)
            case (Application(IndexComposition(), Application(Application(IndexConcatenation(), n1), n2)), n3) =>
                simplify1((n3.deepCopy() o n1) ++ (n3 o n2), forward, ctx2, leaveDefs)

            case (Length(), Application(Application(RangeIndex(), n1), n2)) =>
                simplify1(n2 - n1, forward, ctx2, leaveDefs)
            case (Length(), Application(Application(IndexConcatenation(), n1), n2)) =>
                simplify1(length(n1) + length(n2), forward, ctx2, leaveDefs)
            case (Application(Plus(), _), l@Application(Length(), Application(InfiniteIndex(), _))) =>
                (l, ctx2)
            case (Application(Plus(), l@Application(Length(), Application(InfiniteIndex(), _))), _) =>
                (l, ctx2)

            case (Application(Application(Janus(), f), _), a1) if forward =>
                simplify1(Application(f, a1), forward, ctx2, leaveDefs)
            case (Reverse(), Application(Application(Janus(), f), b)) =>
                (Application(Application(Janus(), b), f), ctx2)
            case (Application(Forget(), _), _) if forward =>
                (Tuple(), ctx2)
            case (Application(Reverse(), Application(Forget(), f)), Tuple()) if forward =>
                simplify1(Application(f, Tuple()), forward, ctx2, leaveDefs)
            case (rev@Reverse(), app1@Application(app2@Application(OrElse(), f1), f2)) =>
                simplify1(app1.copy(app2.copy(argument = Application(rev.copy(), f1)), Application(rev.copy(), f2)), forward, ctx2, leaveDefs)
            case (Application(OrElse(), Lambda(Irreversible(), _, Undefined())), f2) =>
                (f2, ctx2)
            case (app1@Application(app2@Application(OrElse(), f1), f2), a) =>
                val (v1, ctx3) = simplify1(Application(f1, a), forward, ctx2, leaveDefs)
                v1 match {
                case Undefined() =>
                    simplify1(Application(f2, a), forward, ctx3, leaveDefs)
                case v1 if isDefined(v1) =>
                    (v1, ctx3)
                case v1 =>
                    simplify1(OrElseValue(Application(f1, a.deepCopy()), Application(f2, a)), forward, ctx2, leaveDefs)
                }
            case (rev@Reverse(), fix@Application(Fix(), lam@Lambda(Irreversible(), p, b))) =>
                simplify1(fix.copy(argument = lam.copy(pattern = Application(rev.copy(), p), body = Application(rev.copy(), b))), forward, ctx2, leaveDefs)
            case (fix@Application(Fix(), f), a) if isLiteral(a) =>
                simplify1(Application(Application(f, fix), a), forward, ctx2, leaveDefs)

            case (Member(n), m: Module) =>
                m.definitions.find{ case d => d.identity.annotation(VariableIdentity.name).get == n } match {
                case Some(d) =>
                    (d.value, ctx2)
                case None => (Undefined(), ctx2)
                }
                
            case (f, or@OrElseValue(l, r)) =>
                simplify1(or.copy(Application(f, l), Application(f.deepCopy(), r)), forward, ctx2, leaveDefs)
            case (or@OrElseValue(f1, f2), a) =>
                simplify1(or.copy(Application(f1, a), Application(f2, a.deepCopy())), forward, ctx2, leaveDefs)
            case (f, b: Block) =>
                simplify1(b.copy(scope=Application(f, b.scope)), forward, ctx2, leaveDefs)
            case (b: Block, a) =>
                simplify1(b.copy(scope=Application(b.scope, a)), forward, ctx2, leaveDefs)

            case (Application(op@Plus(), _), _) =>
                simplifyAssociative(new Pattern2[Expression, Expression, Expression] { 
                    def unapply(e: Expression) = e match { case Application(Application(Plus(), b), a) => Some(a, b) case _ => None }
                    def apply(a: Expression, b: Expression) = Application(Application(op, b), a)
                    }, true, true, app, forward, ctx2, leaveDefs){
                    case (NumberLiteral(a), NumberLiteral(b)) => Seq(NumberLiteral(a + b))
                    case (a, NumberLiteral(z)) if z == BigDecimal(0) => Seq(a)
                    case (a, b) if a == b => Seq(a * 2)
                    case (a * (lit@NumberLiteral(k)), b) if a == b => 
                        Seq(a * lit.copy(k + 1))
                    case (a * (lit@NumberLiteral(k)), b * NumberLiteral(k2)) if a == b => 
                        Seq(a * lit.copy(k + k2))
                }{ _.isInstanceOf[NumberLiteral] }
            case (Application(op@Times(), _), _) =>
                simplifyAssociative(new Pattern2[Expression, Expression, Expression] { 
                    def unapply(e: Expression) = e match { case Application(Application(Times(), b), a) => Some(a, b) case _ => None }
                    def apply(a: Expression, b: Expression) = Application(Application(op, b), a)
                    }, true, true, app, forward, ctx2, leaveDefs){
                    case (a, b@NumberLiteral(z)) if z == BigDecimal(0) => Seq(b)
                    case (a@NumberLiteral(z), b) if z == BigDecimal(0) => Seq(a)
                    case (a, NumberLiteral(o)) if o == BigDecimal(1) => Seq(a)
                    case (NumberLiteral(l), NumberLiteral(r)) if (l * r / r == l && l * r / l == r) => Seq(NumberLiteral(l * r))
                    case (NumberLiteral(l), Application(Application(Power(), mo), NumberLiteral(r))) if (mo == -1 && (r / l) * l == r && r / (r / l) == l) => Seq(NumberLiteral(l / r))
                    case (a, b) if a == b => Seq(Power()(a)(2))
                    case (pow@Application(pow2@Application(Power(), lit@NumberLiteral(k)), a), b) if a == b => 
                        Seq(pow.copy(pow2.copy(argument = lit.copy(k + 1))))
                    case (pow@Application(pow2@Application(Power(), lit@NumberLiteral(k)), a), Application(Application(Power(), NumberLiteral(k2)), b)) if a == b => 
                        Seq(pow.copy(pow2.copy(argument = lit.copy(k + k2))))
                }{ _.isInstanceOf[NumberLiteral] }
            case (Application(op@And(), _), _) =>
                simplifyAssociative(new Pattern2[Expression, Expression, Expression] { 
                    def unapply(e: Expression) = e match { case Application(Application(And(), a), b) => Some(a, b) case _ => None }
                    def apply(a: Expression, b: Expression) = Application(Application(op, a), b)
                    }, true, false, app, forward, ctx2, leaveDefs){
                    case (BooleanLiteral(a), BooleanLiteral(b)) => Seq(BooleanLiteral(a && b))
                    case (a, b@BooleanLiteral(false)) => Seq(b)
                    case (a, b@BooleanLiteral(true)) => Seq(a)
                    case (a, b) if a == b => Seq(a)
                    case (a == l1, b == l2) if isLiteral(l1) && isLiteral(l2) && l1 != l2 && a == b => Seq(false)
                    case (l1 == a, b == l2) if isLiteral(l1) && isLiteral(l2) && l1 != l2 && a == b => Seq(false)
                    case (a == l1, l2 == b) if isLiteral(l1) && isLiteral(l2) && l1 != l2 && a == b => Seq(false)
                    case (l1 == a, l2 == b) if isLiteral(l1) && isLiteral(l2) && l1 != l2 && a == b => Seq(false)
                    case (a <= NumberLiteral(b), c > NumberLiteral(d)) if a == c && b <= d => Seq(false)
                    case (a < NumberLiteral(b), c >= NumberLiteral(d)) if a == c && b <= d => Seq(false)
                    case (a < NumberLiteral(b), c > NumberLiteral(d)) if a == c && b <= d => Seq(false)
                    case (a <= NumberLiteral(b), c >= NumberLiteral(d)) if a == c && b < d => Seq(false)
                    case (l@(a < NumberLiteral(b)), c < NumberLiteral(d)) if a == c && b <= d => Seq(l)
                    case (l@(a <= NumberLiteral(b)), c <= NumberLiteral(d)) if a == c && b <= d => Seq(l)
                    case (l@(a < NumberLiteral(b)), c <= NumberLiteral(d)) if a == c && b <= d => Seq(l)
                    case (l@(a <= NumberLiteral(b)), c < NumberLiteral(d)) if a == c && b < d => Seq(l)
                    case (l@(a > NumberLiteral(b)), c > NumberLiteral(d)) if a == c && b >= d => Seq(l)
                    case (l@(a >= NumberLiteral(b)), c >= NumberLiteral(d)) if a == c && b >= d => Seq(l)
                    case (l@(a > NumberLiteral(b)), c >= NumberLiteral(d)) if a == c && b >= d => Seq(l)
                    case (l@(a >= NumberLiteral(b)), c > NumberLiteral(d)) if a == c && b > d => Seq(l)
                }{ _ => () }
            case (Application(op@Or(), _), _) =>
                 simplifyAssociative(new Pattern2[Expression, Expression, Expression] { 
                    def unapply(e: Expression) = e match { case Application(Application(Or(), a), b) => Some(a, b) case _ => None }
                    def apply(a: Expression, b: Expression) = Application(Application(op, a), b)
                    }, true, false, app, forward, ctx2, leaveDefs){
                    case (BooleanLiteral(a), BooleanLiteral(b)) => Seq(BooleanLiteral(a || b))
                    case (a, b@BooleanLiteral(false)) => Seq(a)
                    case (a, b@BooleanLiteral(true)) => Seq(b)
                    case (a, b) if a == b => Seq(a)
                }{ _ => () }
            case (f, a) => (app, ctx2)
            }
        }

    def simplifyStatement(stmt: Statement, context: Map[VariableIdentity, Expression] = defaultContext, leaveDefs: Boolean): (Statement, Map[VariableIdentity, Expression]) =
        stmt match {
        case stmt@Let(p, v) =>
            val (v1, ctx1) = simplify1(v, true, context, leaveDefs)
            val (p2, ctx2) = simplify1(p, false, ctx1, leaveDefs)
            (p2, v1) match {
            case (Undefined(), _) =>
                (Sequence(), ctx2)
            case (Variable(id, true), value) =>
                (Sequence(), ctx2 + (id -> value))
            case (l1, l2) if isLiteral(l1) && isLiteral(l2) =>
                if (l1 == l2)
                    (Sequence(), ctx2)
                else
                    (Fail(), ctx2)
            case (Tuple(cs1@_*), Tuple(cs2@_*)) =>
                if (cs1.size == cs2.size)
                    simplifyStatement(Sequence((for ((c1, c2) <- cs1.zip(cs2)) yield Let(c1, c2)):_*), ctx2, leaveDefs)
                else
                    (Fail(), context)
            case (Application(f, a), value) =>
                val (value3, ctx3) = simplify1(Application(Application(Reverse(), f), value), true, ctx2, leaveDefs)
                simplifyStatement(Let(a, value3), ctx3, leaveDefs)
            case (p2, v1) => (stmt.copy(p2, v1), ctx2)
            }
        case stmt@Def(id, v) =>
            if (leaveDefs) {
                val (v1, ctx1) = simplify(v, true, context, leaveDefs)
                (simplifyDescription[Def](stmt.copy(value = v1), _.identity, context), ctx1)
            } else
                simplifyStatement(Let(Variable(id, true), v), context, leaveDefs)
        case stmt@Sequence(ss@_*) =>
            val (ss1, ctx1) = ((Seq[Statement](), context) /: ss){
            case ((ss2, context2), s2) =>
                simplifyStatement(s2, context2, leaveDefs) match {
                case (Sequence(ss3@_*), ctx3) => (ss2 ++ ss3, ctx3)
                case (s3, ctx3) => (ss2 :+ s3, ctx3)
                }
            }
            if (ss1.exists(_ == Fail()))
                (Fail(), ctx1)
            else {
                var ss2 = ss1
                var i = 0
                var j = ss2.size - 1
                var didMerge = false
                while (j > 0 && (ss2(j) match { case Let(BooleanLiteral(true), _) => false; case _ => true }))
                    j -= 1
                while (j > i) {
                    ss2(i) match {
                    case Let(BooleanLiteral(true), c1) =>
                        ss2(j) match {
                        case cond@Let(BooleanLiteral(true), c2) =>
                            ss2 = ss2.take(i) ++ ss2.drop(i + 1).take(j - i - 1) ++
                                Seq(cond.copy(value = Application(Application(And(), c1), c2))) ++
                                ss2.drop(j + 1)
                            j -= 1
                            i -= 1
                            didMerge = true
                        }
                    case _ =>
                    }
                    i += 1
                }
                if (didMerge)
                    simplifyStatement(stmt.copy(ss2:_*), ctx1, leaveDefs)
                else if (ss2.size == 1)
                    (ss2(0), ctx1)
                else
                    (stmt.copy(ss2:_*), ctx1)
            }
        case stmt => (stmt, context)
        }
    
    def simplifyDescription[T](x: T, part: T => Annotated, context: Map[VariableIdentity, Expression]): T = {
        if (part(x).annotation(DocBookGenerator.descriptionInfo).nonEmpty)
                part(x).putAnnotation(DocBookGenerator.descriptionInfo, 
                        simplify(part(x).annotation(DocBookGenerator.descriptionInfo).get, true, context)._1)
        x
    }
}