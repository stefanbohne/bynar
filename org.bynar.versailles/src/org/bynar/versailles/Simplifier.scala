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
        case expr: Tuple => true
        case expr: Lambda => true
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

    def simplify(expr: Expression, forward: Boolean, context: Map[VariableIdentity, Expression] = defaultContext): (Expression, Map[VariableIdentity, Expression]) =
        expr match {
        case expr@Variable(id, true) =>
            (if (forward) context.getOrElse(id, expr) else expr, context - id)
        case expr@Variable(id, false) =>
            (context.getOrElse(id, expr), context)
        case expr@Tuple(cs@_*) =>
            val (cs1, ctx1) = ((Seq[Expression](), context) /: cs){
            case ((cs2, ctx2), c) =>
                val (c2, ctx3) = simplify(c, forward, ctx2)
                (cs2 :+ c2, ctx3)
            }
            (expr.copy(cs1:_*), ctx1)
        case expr@Block(b1, Block(b2, s)) =>
            simplify(Block(Sequence(b1, b2), s), forward, context)
        case expr@Block(b, s) =>
            val (b2, ctx2) = simplifyStatement(b, context)
            b2 match {
            case Fail() =>
                (Undefined(), ctx2)
            case Sequence() =>
                simplify(s, forward, ctx2)
            case b2 =>
                val (s2, ctx3) = simplify(s, forward, ctx2)
                (expr.copy(b2, s2), ctx3)
            }
        case expr@Lambda(Irreversible(), block@Block(ss, s), b) =>
            simplify(expr.copy(pattern = s, body = block.copy(reverseStatement(ss), b)), forward, context)
        case expr@Lambda(Irreversible(), app@Application(f, a), b) =>
            val x = VariableIdentity.setName(new VariableIdentity(), '_)
            simplify(expr.copy(pattern = Variable(x, true),
                               body = Block(Let(app, Variable(x, false)), b)),
                     forward,
                     context)
        case expr@Lambda(Irreversible(), p, b) =>
            val (p2, ctx2) = simplify(p, !forward, context)
            val (b2, ctx3) = simplify(b, forward, ctx2)
            (expr.copy(pattern = p2, body = b2), context)
        case expr@Lambda(jc, p, b) =>
            simplify(Janus()(expr.copy(janusClass = Irreversible()))(
                             expr.copy(janusClass = Irreversible(),
                                       pattern = b,
                                       body = p)),
                     forward,
                     context)
        case expr@OrElseValue(a, b) =>
            val (a2, ctx2) = simplify(a, forward, context)
            val (b2, ctx3) = simplify(b, forward, ctx2)
            (a2, b2) match {
            case (Undefined(), b2) => (b2, ctx3)
            case (a2, Undefined()) => (a2, ctx3)
            case (a2, _) if isDefined(a2) => (a2, ctx3)
            case (a2, b2) => (expr.copy(a2, b2), ctx3)
            }

        case expr@Application(f, a) =>
            val (a1, ctx1) = simplify(a, forward, context)
            val (f2, ctx2) = simplify(f, true, ctx1)
            simplifyApplication(expr.copy(f2, a1), forward, ctx1)
        case expr => (expr, context)
        }

    def simplifyApplication(app: Application, forward: Boolean, ctx2: Map[VariableIdentity, Expression]): (Expression, Map[VariableIdentity, Expression]) =
        app match {
        case Application(f2, a1) =>
            (f2, a1) match {
            case (lam(p, b), a1) =>
                simplify(Block(Let(p, a1), b), forward, ctx2)
            case (Reverse(), Reverse()) =>
                (Reverse(), ctx2)
            case (Reverse(), reverse(f)) =>
                (f, ctx2)

            case (Minus(), r: NumberLiteral) =>
                (Application(Plus(), r.copy(-r.value)), ctx2)
            case (Minus(), r) =>
                (Application(Plus(), r * NumberLiteral(-1)), ctx2)
            case (Reverse(), Application(Plus(), x)) =>
                (Application(Minus(), x), ctx2)
            case (Reverse(), Application(Minus(), x)) =>
                (Application(Plus(), x), ctx2)
            case (Application(Plus(), NumberLiteral(r)), NumberLiteral(l)) =>
                (NumberLiteral(l + r), ctx2)
            case (Application(Plus(), x), NumberLiteral(z)) if z == 0 =>
                (x, ctx2)
            case (Application(Plus(), NumberLiteral(z)), x) if z == 0 =>
                (x, ctx2)
            case (Application(Plus(), Application(Application(op@Plus(), NumberLiteral(r)), x)), NumberLiteral(l)) =>
                simplify(Application(Application(op, NumberLiteral(l + r)), x), forward, ctx2)
            case (Application(Plus(), NumberLiteral(r)), Application(Application(op@Plus(), NumberLiteral(l)), x)) =>
                simplify(Application(Application(op, NumberLiteral(l + r)), x), forward, ctx2)
            case (Application(Plus(), Application(Application(op1@Plus(), NumberLiteral(r)), x)), f3@Application(Application(op2@Plus(), NumberLiteral(l)), y)) =>
                simplify(Application(Application(op1, Application(Application(op2, NumberLiteral(l + r)), x)), y), forward, ctx2)
            case (Application(op@Plus(), r), l: NumberLiteral) =>
                simplify(Application(Application(op, l), r), forward, ctx2)
            case (f1@Application(Plus(), x), Application(f3@Application(Plus(), l: NumberLiteral), y)) =>
                simplify(Application(f3, Application(f1, y)), forward, ctx2)
            case (f1@Application(Plus(), f2@Application(f3@Application(Plus(), r: NumberLiteral), x)), y) =>
                simplify(Application(f3, Application(Application(f1.function, x), y)), forward, ctx2)

            case (Divide(), l: NumberLiteral) if ((1 / l.value) * l.value == 1) =>
                simplify(Application(Times(), NumberLiteral(1 / l.value)), forward, ctx2)
            case (Divide(), l) =>
                simplify(Application(Times(), Application(Application(Power(), NumberLiteral(-1)), l)), forward, ctx2)
            case (Reverse(), Application(Times(), x)) =>
                (Application(Divide(), x), ctx2)
            case (Reverse(), Application(Divide(), x)) =>
                (Application(Times(), x), ctx2)
            case (Application(Times(), NumberLiteral(r)), NumberLiteral(l)) =>
                (NumberLiteral(l * r), ctx2)
            case (Application(Times(), x), NumberLiteral(o)) if o == 1 =>
                (x, ctx2)
            case (Application(Times(), NumberLiteral(o)), x) if o == 1 =>
                (x, ctx2)
            case (Application(Times(), f1@Application(f2@Application(Times(), NumberLiteral(r)), x)), NumberLiteral(l))
                if (l * r / r == l && l * r / l == r) =>
                simplify(Application(Application(f2.function, NumberLiteral(l * r)), x), forward, ctx2)
            case (Application(Times(), NumberLiteral(r)), f1@Application(f2@Application(Times(), NumberLiteral(l)), x))
                if (l * r / r == l && l * r / l == r) =>
                simplify(Application(Application(f2.function, NumberLiteral(l * r)), x), forward, ctx2)
            case (Application(Times(), NumberLiteral(r)), f1@Application(f2@Application(Times(), Application(Application(Power(), NumberLiteral(mo)), NumberLiteral(l))), x))
                if (mo == -1 && (r / l) * l == r && r / (r / l) == l) =>
                simplify(Application(Application(f2.function, NumberLiteral(r / l)), x), forward, ctx2)
            case (Application(Times(), f1@Application(f2@Application(Times(), NumberLiteral(r)), x)), f3@Application(f4@Application(Times(), NumberLiteral(l)), y)) =>
                simplify(Application(Application(f2.function, Application(Application(f4.function, NumberLiteral(l * r)), y)), x), forward, ctx2)
            case (f1@Application(Times(), r), l: NumberLiteral) =>
                simplify(Application(Application(f1.function, l), r), forward, ctx2)
            case (f1@Application(Times(), x), f2@Application(f3@Application(Times(), l: NumberLiteral), y)) =>
                simplify(Application(f3, Application(f1, y)), forward, ctx2)
            case (f1@Application(Times(), f2@Application(f3@Application(Times(), r: NumberLiteral), x)), y) =>
                simplify(Application(f3, Application(Application(f1.function, x), y)), forward, ctx2)
            case (f1@Application(Times(), r: NumberLiteral), f2@Application(f3@Application(Plus(), NumberLiteral(l)), x)) =>
                simplify(Application(f3.copy(f3.function, NumberLiteral(l * r.value)), Application(f1.copy(f1.function, r), x)), forward, ctx2)
            case (IntegerDivide(), Tuple(l: NumberLiteral, r: NumberLiteral)) =>
                (NumberLiteral(((l.value - (l.value % r.value)) / r.value)), ctx2)

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
            case (Application(And(), BooleanLiteral(false)), _) =>
                (BooleanLiteral(false), ctx2)
            case (Application(And(), _), BooleanLiteral(false)) =>
                (BooleanLiteral(false), ctx2)
            case (Application(And(), BooleanLiteral(b1)), BooleanLiteral(b2)) =>
                (BooleanLiteral(b1 && b2), ctx2)
            case (Application(Or(), BooleanLiteral(b1)), BooleanLiteral(b2)) =>
                (BooleanLiteral(b1 || b2), ctx2)
            case (Application(Or(), BooleanLiteral(true)), _) =>
                (BooleanLiteral(true), ctx2)
            case (Application(Or(), _), BooleanLiteral(true)) =>
                (BooleanLiteral(true), ctx2)
            case (Not(), BooleanLiteral(b)) =>
                (BooleanLiteral(!b), ctx2)

            case (Application(Concat(), StringLiteral(l1)), StringLiteral(l2)) =>
                (StringLiteral(l1 + l2), ctx2)

            case (Application(Typed(), t), v) =>
                // TODO: proper type check
                (v, ctx2)

            case (Application(IndexConcatenation(), Application(Application(RangeIndex(), NumberLiteral(n1)), NumberLiteral(n2))), n3) if n1 == n2 =>
                (n3, ctx2)
            case (Application(IndexConcatenation(), n1), Application(Application(RangeIndex(), NumberLiteral(n2)), NumberLiteral(n3))) if n2 == n3 =>
                (n1, ctx2)
            case (Application(IndexConcatenation(), Application(Application(RangeIndex(), n1), NumberLiteral(i))), Application(Application(RangeIndex(), NumberLiteral(i2)), n2)) if i == i2 =>
                (rangeIndex(n1)(n2), ctx2)
            case (Application(IndexConcatenation(), Application(Application(RangeIndex(), n1), NumberLiteral(i))), Application(InfiniteIndex(), NumberLiteral(i2))) if i == i2 =>
                (infiniteIndex(n1), ctx2)
            case (Application(IndexComposition(), Application(InfiniteIndex(), n1)), Application(InfiniteIndex(), n2)) =>
                simplify(infiniteIndex(n2 + n1), forward, ctx2)
            case (Application(IndexComposition(), singletonIndex(n1)), Application(InfiniteIndex(), n2)) =>
                simplify(singletonIndex(n2 + n1), forward, ctx2)
            case (Application(IndexComposition(), singletonIndex(n1)), Application(Application(RangeIndex(), n2), n3)) =>
                simplify(Block(Let(true, n1.deepCopy() < n3 - n2.deepCopy()),
                               singletonIndex(n2 + n1)), forward, ctx2)
            case (Application(IndexComposition(), singletonIndex(n1)), n2 ++ n3) =>
                simplify(OrElseValue(
                    n2.deepCopy() o singletonIndex(n1),
                    n3 o singletonIndex(n1.deepCopy() - length(n2))), forward, ctx2)
            case (Application(IndexComposition(), Application(Application(RangeIndex(), n1), n2)), Application(InfiniteIndex(), n3)) =>
                simplify(rangeIndex(n1 + n3.deepCopy())(n2 + n3), forward, ctx2)
            case (Application(IndexComposition(), Application(Application(RangeIndex(), n1), n2)), Application(Application(RangeIndex(), n3), n4)) =>
                simplify(Block(Let(true,  
                                n1.deepCopy() <= n4.deepCopy() - n3.deepCopy() &&
                                n2.deepCopy() <= n4 - n3.deepCopy()),
                        rangeIndex(n1 + n3.deepCopy())(n2 + n3)), forward, ctx2)
            case (Application(IndexComposition(), Application(Application(RangeIndex(), n1), n2)), Application(Application(IndexConcatenation(), n3), n4)) =>
                simplify({
                    val x = VariableIdentity.setName(new VariableIdentity, 'l)
                    Block(Let(Variable(x, true), length(n3.deepCopy())),
                        (n3 o rangeIndex(min(n1)(Variable(x, false)))(min(n2)(Variable(x, false)))) ++
                        (n4 o rangeIndex(max(n1.deepCopy() - Variable(x, false))(0))(max(n2.deepCopy() - Variable(x, false))(0))))
                }, forward, ctx2)                          
            case (Application(IndexComposition(), Application(Application(IndexConcatenation(), n1), n2)), n3) =>
                simplify((n3.deepCopy() o n1) ++ (n3 o n2), forward, ctx2)
                
            case (Length(), Application(Application(RangeIndex(), n1), n2)) =>
                simplify(n2 - n1, forward, ctx2)
            case (Length(), Application(Application(IndexConcatenation(), n1), n2)) =>
                simplify(length(n1) + length(n2), forward, ctx2)
            case (Application(Plus(), _), l@Application(Length(), Application(InfiniteIndex(), _))) =>
                (l, ctx2)
            case (Application(Plus(), l@Application(Length(), Application(InfiniteIndex(), _))), _) =>
                (l, ctx2)

            case (Application(Application(Janus(), f), _), a1) =>
                simplify(Application(f, a1), forward, ctx2)
            case (Reverse(), Application(Application(Janus(), f), b)) =>
                (Application(Application(Janus(), b), f), ctx2)
            case (Application(Forget(), _), _) if forward =>
                (Tuple(), ctx2)
            case (Application(Reverse(), Application(Forget(), f)), Tuple()) if forward =>
                simplify(Application(f, Tuple()), forward, ctx2)
            case (rev@Reverse(), app1@Application(app2@Application(OrElse(), f1), f2)) =>
                simplify(app1.copy(app2.copy(argument = Application(rev.copy(), f1)), Application(rev.copy(), f2)), forward, ctx2)
            case (Application(OrElse(), Lambda(Irreversible(), _, Undefined())), f2) =>
                (f2, ctx2)
            case (app1@Application(app2@Application(OrElse(), f1), f2), a) =>
                val (v1, ctx3) = simplify(Application(f1, a), forward, ctx2)
                v1 match {
                case Undefined() =>
                    simplify(Application(f2, a), forward, ctx3)
                case v1 if isDefined(v1) =>
                    (v1, ctx3)
                case v1 =>
                    simplify(OrElseValue(Application(f1, a.deepCopy()), Application(f2, a)), forward, ctx2)
                }
            case (rev@Reverse(), fix@Application(Fix(), lam@Lambda(Irreversible(), p, b))) =>
                simplify(fix.copy(argument = lam.copy(pattern = Application(rev.copy(), p), body = Application(rev.copy(), b))), forward, ctx2)
            case (fix@Application(Fix(), f), a) if isLiteral(a) =>
                simplify(Application(Application(f, fix), a), forward, ctx2)

            case (f, or@OrElseValue(l, r)) =>
                simplify(or.copy(Application(f, l), Application(f.deepCopy(), r)), forward, ctx2)
            case (or@OrElseValue(f1, f2), a) =>
                simplify(or.copy(Application(f1, a), Application(f2, a.deepCopy())), forward, ctx2)
            case (f, b: Block) =>
                simplify(b.copy(scope=Application(f, b.scope)), forward, ctx2)
            case (b: Block, a) =>
                simplify(b.copy(scope=Application(b.scope, a)), forward, ctx2)
            case (f, a) => (app, ctx2)
            }
        }

    def simplifyStatement(stmt: Statement, context: Map[VariableIdentity, Expression] = defaultContext): (Statement, Map[VariableIdentity, Expression]) =
        stmt match {
        case stmt@Let(p, v) =>
            val (v1, ctx1) = simplify(v, true, context)
            val (p2, ctx2) = simplify(p, false, ctx1)
            (p2, v1) match {
            case (Undefined(), _) =>
                (Sequence(), context)
            case (Variable(id, true), value) =>
                (Sequence(), context + (id -> value))
            case (l1, l2) if isLiteral(l1) && isLiteral(l2) =>
                if (l1 == l2)
                    (Sequence(), context)
                else
                    (Fail(), context)
            case (Tuple(cs1@_*), Tuple(cs2@_*)) =>
                if (cs1.size == cs2.size)
                    simplifyStatement(Sequence((for ((c1, c2) <- cs1.zip(cs2)) yield Let(c1, c2)):_*), context)
                else
                    (Fail(), context)
            case (Application(f, a), value) =>
                val (value2, ctx2) = simplify(Application(Application(Reverse(), f), value), true, context)
                simplifyStatement(Let(a, value2), ctx2)
            case (p2, v1) => (stmt.copy(p2, v1), ctx2)
            }
        case Def(id, v) =>
            simplifyStatement(Let(Variable(id, true), v), context)
        case stmt@Sequence(ss@_*) =>
            val (ss1, ctx1) = ((Seq[Statement](), context) /: ss){
            case ((ss2, context2), s2) =>
                simplifyStatement(s2, context2) match {
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
                        }
                    case _ =>
                    }
                    i += 1
                }
                if (ss2.size == 1)
                    (ss2(0), ctx1)
                else
                    (stmt.copy(ss2:_*), ctx1)
            }
        case stmt => (stmt, context)
        }
}