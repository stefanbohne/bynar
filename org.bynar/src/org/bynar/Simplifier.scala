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

    override def simplifyApplication(app: Application, forward: Boolean, context: Map[VariableIdentity, Expression]): (Expression, Map[VariableIdentity, Expression]) =
        (app.function, app.argument) match {
        case (MemberContextedType(Seq()), t) =>
            (t, context)
        case (BitWidth(), Application(MemberContextedType(p), t)) =>
            val v = VariableIdentity.setName(new VariableIdentity(), '_)
            simplify(Lambda(Irreversible(),
                   Variable(v, true),
                   Application(app.copy(argument = t),
                               (p :\ (Variable(v, false): Expression)){ case (n, x) => Application(Member(n), x) })),
               forward, context)
        case (BitWidth(), bt: BitTypeExpression) =>
            val x = VariableIdentity.setName(new VariableIdentity(), '_)
            (Lambda(Irreversible(), Variable(x, true), simplify(bt.dependentBitWidth(Variable(x, false)), true, context)._1), context)
        case (Application(Member(n), t: BitUnionType), a) =>
            t.variants.find(buv => buv.name == n) match {
            case Some(buv) => simplify(Application(buv.`type`, a), forward, context)
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

}