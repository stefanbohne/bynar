package org.bynar

import scala.collection._
import org.bynar.versailles.Application
import org.bynar.versailles.VariableIdentity
import org.bynar.versailles.Expression
import org.bynar.versailles.Lambda
import org.bynar.versailles.Irreversible
import org.bynar.versailles.Variable
import org.bynar.versailles.Member
import org.bynar.versailles.Tuple
import org.bynar.versailles.Undefined
import org.bynar.versailles.Statement

class Simplifier extends org.bynar.versailles.Simplifier {

    override def defaultContext = org.bynar.defaultContext

    override def isLiteral(expr: Expression): Boolean =
        expr match {
        case Application(_: BitRecordType, v) =>
            isLiteral(v)
        case Application(Application(Member(_), _: BitUnionType), v) =>
            isLiteral(v)
        case Application(t: WhereType, v) =>
            isLiteral(Application(t.`type`, v))
        case _ =>
            super.isLiteral(expr)
        }
    override def isDefined(expr: Expression): Boolean =
        expr match {
        case _ =>
            super.isDefined(expr)
        }

    override def simplify1(expr: Expression, forward: Boolean, context: Map[VariableIdentity, Expression] = defaultContext): (Expression, Map[VariableIdentity, Expression]) =
        expr match {
        case expr@BitFieldType(bw) if forward =>
            val (bw1, ctx1) = simplify1(bw, forward, context)
            (expr.copy(bw1), ctx1)
        case expr@BitRecordType(b) if forward =>
            val (b1, ctx1) = simplifyStatement(b, context)
            (expr.copy(b1), ctx1)
        case expr@BitRegisterType(bw, b) if forward =>
            val (bw1, ctx1) = simplify1(bw, forward, context)
            val (b2, ctx2) = simplifyStatement(b, context)
            (expr.copy(bw1, b2), ctx2)
        case expr@BitUnionType(b) if forward =>
            val (b1, ctx1) = simplifyStatement(b, context)
            (expr.copy(b1), ctx1)
        case expr@BitArrayType(et, u) if forward =>
            val (et1, ctx1) = simplify1(et, forward, context)
            val (u2, ctx2) = simplify1(u, forward, context)
            (expr.copy(et1, u2), ctx1)
        case expr@WrittenType(t, w) if forward =>
            val (t1, ctx1) = simplify1(t, forward, context)
            val (w2, ctx2) = simplify1(w, forward, context)
            (expr.copy(t1, w2), ctx1)
        case expr@ConvertedType(t, c) if forward =>
            val (t1, ctx1) = simplify1(t, forward, context)
            val (c2, ctx2) = simplify1(c, forward, context)
            (expr.copy(t1, c2), ctx1)
        case expr@WhereType(t, w) if forward =>
            val (t1, ctx1) = simplify1(t, forward, context)
            val (w2, ctx2) = simplify1(w, forward, context)
            (expr.copy(t1, w2), ctx1)
        case expr@InterpretedBitType(t, i) if forward =>
            val (t1, ctx1) = simplify1(t, forward, context)
            val (i2, ctx2) = i match {
            case i@FixedInterpretation(f) =>
                val (f2, ctx2) = simplify1(f, forward, ctx1)
                (i.copy(f2), ctx2)
            case UnitInterpretation(_) =>
                (i, context)
            case i@EnumInterpretation(b) =>
                val (b2, ctx2) = simplifyStatement(b, ctx1)
                (i.copy(b2), ctx2)
            case i@ContainingInterpretation(ct) =>
                val (ct2, ctx2) = simplify1(ct, forward, context)
                (i.copy(ct2), ctx2)
            }
            (expr.copy(t1, i2), ctx1)
        case expr => super.simplify1(expr, forward, context)
        }
    
    override def simplifyApplication(app: Application, forward: Boolean, context: Map[VariableIdentity, Expression]): (Expression, Map[VariableIdentity, Expression]) =
        (app.function, app.argument) match {
        case (MemberContextedType(Seq()), t) =>
            (t, context)
        case (BitWidth(), Application(MemberContextedType(p), t)) =>
            val v = VariableIdentity.setName(new VariableIdentity(), '_)
            simplify1(Lambda(Irreversible(),
                   Variable(v, true),
                   Application(app.copy(argument = t),
                               (p :\ (Variable(v, false): Expression)){ case (n, x) => Application(Member(n), x) })),
               forward, context)
        case (BitWidth(), bt: BitTypeExpression) =>
            val x = VariableIdentity.setName(new VariableIdentity(), '_)
            (Lambda(Irreversible(), Variable(x, true), simplify1(bt.dependentBitWidth(Variable(x, false)), true, context)._1), context)
        case (Application(Member(n), t: BitUnionType), a) =>
            t.variants.find(buv => buv.name == n) match {
            case Some(buv) => simplify1(Application(buv.`type`, a), forward, context)
            case None => (Undefined(), context)
            }
        case (Member(n), t: BitRecordType) =>
            t.components.find{ case brc => brc.name == n } match {
            case Some(brc) =>
                (brc.`type`, context)
            case None => (Undefined(), context)
            }
        case (Member(n), Application(t: BitRecordType, Tuple(cs@_*))) =>
            if (t.components.size != cs.size)
                (Undefined(), context)
            else
                t.components.zipWithIndex.find{ case (brc, i) => brc.name == n } match {
                case Some((brc, i)) =>
                    (cs(i), context)
                case None => (Undefined(), context)
                }
        case (Member(n), InterpretedBitType(t, i: EnumInterpretation)) =>
            i.values.find{ case ev => ev.name == n } match {
            case Some(ev) =>
                (ev.value, context)
            case None => (Undefined(), context)
            }
        case _ => super.simplifyApplication(app, forward, context)
        }

    override def simplifyStatement(stmt: Statement, context: Map[VariableIdentity, Expression] = defaultContext, leaveDefs: Boolean = false): (Statement, Map[VariableIdentity, Expression]) =
        stmt match {
        case stmt@BitRecordComponent(_, t) =>
            val (t1, ctx1) = simplify1(t, true, context)
            (stmt.copy(`type` = t1), ctx1)
        case stmt@BitRegisterComponent(_, p, t) =>
            val (p1, ctx1) = simplify1(p, true, context)
            val (t2, ctx2) = simplify1(t, true, ctx1)
            (stmt.copy(position = p1, `type` = t2), ctx1)
        case stmt@BitUnionVariant(_, t) =>
            val (t1, ctx1) = simplify1(t, true, context)
            (stmt.copy(`type` = t1), ctx1)
        case stmt@EnumValue(_, v) =>
            val (v1, ctx1) = simplify1(v, true, context)
            (stmt.copy(value = v1), ctx1)
        case stmt => super.simplifyStatement(stmt, context, leaveDefs)
        }
    
}