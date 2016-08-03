package org.bynar.versailles

import java.math.MathContext
import java.math.RoundingMode

class Simplifier {
    
    val debug = scala.collection.mutable.Buffer[String]()

    def isLiteral(expr: Expression): Boolean =
        expr match {
        case expr: Literal => true
        case Tuple(cs@_*) => cs.forall(isLiteral(_))
        case _ => false
        }
    def isDefined(expr: Expression): Boolean = 
        expr match {
        case Undefined() => false
        case expr: Literal => true
        case expr: Tuple => true
        case expr: Lambda => true
        case _ => false
    }
    
    def reverseStatement(stmt: Statement): Statement =
        stmt match {
        case stmt@Let(p, v) => stmt.copy(v, p)
        case stmt@Sequence(ss@_*) => stmt.copy(ss.reverse.map{ reverseStatement(_) }:_*)
        }
    
    def simplify(expr: Expression, forward: Boolean, context: Map[VariableIdentity, Expression]): (Expression, Map[VariableIdentity, Expression]) =
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
            val x = VariableIdentity.setName(new VariableIdentity(), "_")
            simplify(expr.copy(pattern = Variable(x, true), 
                               body = Block(Let(app, Variable(x, false)), b)), 
                     forward, 
                     context)
        case expr@Lambda(Irreversible(), p, b) =>
            val (p2, ctx2) = simplify(p, !forward, context)
            val (b2, ctx3) = simplify(b, forward, ctx2)
            (expr.copy(pattern = p2, body = b2), context)
        case expr@Lambda(jc, p, b) =>
            simplify(Application(Application(Janus(), expr.copy(janusClass = Irreversible())),
                                 expr.copy(janusClass = Irreversible(),
                                           pattern = b,
                                           body = p)),
                     forward,
                     context)
        case expr@Application(f, a) =>
            val (a1, ctx1) = simplify(a, forward, context)
            val (f2, ctx2) = simplify(f, true, ctx1)
            (f2, a1) match {
            case (Lambda(Irreversible(), p, b), a1) =>
                simplify(Block(Let(p, a1), b), forward, ctx2)
            case (Reverse(), Reverse()) =>
                (Reverse(), ctx2)
            case (Reverse(), Application(Reverse(), f)) =>
                (f, ctx2)
                
            case (Minus(), r: NumberLiteral) =>
                (Application(Plus(), r.copy(-r.value)), ctx2)
            case (Minus(), r) =>
                (Application(Plus(), Application(Application(Times(), NumberLiteral(-1)), r)), ctx2)
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
                simplify(Application(Application(op, NumberLiteral(l + r)), x), forward, context)
            case (Application(Plus(), NumberLiteral(r)), Application(Application(op@Plus(), NumberLiteral(l)), x)) =>
                simplify(Application(Application(op, NumberLiteral(l + r)), x), forward, context)
            case (Application(Plus(), Application(Application(op1@Plus(), NumberLiteral(r)), x)), f3@Application(Application(op2@Plus(), NumberLiteral(l)), y)) =>
                simplify(Application(Application(op1, Application(Application(op2, NumberLiteral(l + r)), x)), y), forward, context)
            case (Application(op@Plus(), r), l: NumberLiteral) =>
                simplify(Application(Application(op, l), r), forward, context)
            case (f1@Application(Plus(), x), Application(f3@Application(Plus(), l: NumberLiteral), y)) =>
                simplify(Application(f3, Application(f1, y)), forward, context)
            case (f1@Application(Plus(), f2@Application(f3@Application(Plus(), r: NumberLiteral), x)), y) =>
                simplify(Application(f3, Application(Application(f1.function, x), y)), forward, context)            
    
            case (Divide(), l: NumberLiteral) if ((1 / l.value) * l.value == 1) =>
                simplify(Application(Times(), NumberLiteral(1 / l.value)), forward, context)
            case (Divide(), l) =>
                simplify(Application(Times(), Application(Application(Power(), NumberLiteral(-1)), l)), forward, context)
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
                simplify(Application(Application(f2.function, NumberLiteral(l * r)), x), forward, context)
            case (Application(Times(), NumberLiteral(r)), f1@Application(f2@Application(Times(), NumberLiteral(l)), x))
                if (l * r / r == l && l * r / l == r) =>
                simplify(Application(Application(f2.function, NumberLiteral(l * r)), x), forward, context)
            case (Application(Times(), NumberLiteral(r)), f1@Application(f2@Application(Times(), Application(Application(Power(), NumberLiteral(mo)), NumberLiteral(l))), x))
                if (mo == -1 && (r / l) * l == r && r / (r / l) == l) => 
                simplify(Application(Application(f2.function, NumberLiteral(r / l)), x), forward, context)
            case (Application(Times(), f1@Application(f2@Application(Times(), NumberLiteral(r)), x)), f3@Application(f4@Application(Times(), NumberLiteral(l)), y)) =>
                simplify(Application(Application(f2.function, Application(Application(f4.function, NumberLiteral(l * r)), y)), x), forward, context)
            case (f1@Application(Times(), r), l: NumberLiteral) =>
                simplify(Application(Application(f1.function, l), r), forward, context)
            case (f1@Application(Times(), x), f2@Application(f3@Application(Times(), l: NumberLiteral), y)) =>
                simplify(Application(f3, Application(f1, y)), forward, context)
            case (f1@Application(Times(), f2@Application(f3@Application(Times(), r: NumberLiteral), x)), y) =>
                simplify(Application(f3, Application(Application(f1.function, x), y)), forward, context)            
            case (f1@Application(Times(), r: NumberLiteral), f2@Application(f3@Application(Plus(), NumberLiteral(l)), x)) =>
                simplify(Application(f3.copy(f3.function, NumberLiteral(l * r.value)), Application(f1.copy(f1.function, r), x)), forward, context)
            case (IntegerDivide(), Tuple(l: NumberLiteral, r: NumberLiteral)) => 
                (NumberLiteral(((l.value - (l.value % r.value)) / r.value)), ctx2)
          
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
            case (f@Application(Application(OrElse(), f1), f2), a) =>
                val (v1, ctx3) = simplify(Application(f1, a), forward, ctx2)
                v1 match {
                case Undefined() => 
                    simplify(Application(f2, a), forward, ctx3)
                case v1 if isDefined(v1) =>
                    (v1, ctx3)
                case v1 =>
                    (expr.copy(f, a), ctx2)
                }
                
            case (rev@Reverse(), fix@Application(Fix(), lam@Lambda(Irreversible(), p, b))) =>
                simplify(fix.copy(argument = lam.copy(pattern = Application(rev.copy(), p), body = Application(rev.copy(), b))), forward, ctx2)
            case (fix@Application(Fix(), f), a) if isLiteral(a) =>
                simplify(Application(Application(f, fix), a), forward, ctx2)
                
            case (f, a) => (expr.copy(f, a), ctx2)
            }
        case expr => (expr, context)
        }
    
    def simplifyStatement(stmt: Statement, context: Map[VariableIdentity, Expression]): (Statement, Map[VariableIdentity, Expression]) = 
        stmt match {
        case stmt@Let(p, v) =>
            val (v1, ctx1) = simplify(v, true, context)
            val (p2, ctx2) = simplify(p, false, ctx1)
            (p2, v1) match {
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
            else if (ss1.size == 1)
                (ss1(0), ctx1)
            else
                (stmt.copy(ss1:_*), ctx1)
        case stmt => (stmt, context)
        }
} 