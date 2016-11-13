package org.bynar

import org.bynar.versailles.Expression
import org.bynar.versailles.JanusClass
import org.bynar.versailles.Irreversible
import org.bynar.versailles.Messages
import org.bynar.versailles.Message
import org.bynar.versailles.Statement
import org.bynar.versailles.VariableIdentity

class VariableAnalyzer extends org.bynar.versailles.VariableAnalyzer {

    import VariableAnalyzer._
    import org.bynar.versailles.VariableAnalyzer._

    override def defaultContext = Context(Map(org.bynar.defaultContext.keySet.toSeq.map{
        id => VariableIdentity.getName(id) -> ContextEntry(id, false)
    }:_*))

    override def analyze(it: Expression, pattern: Boolean, janusClass: JanusClass, context: Context): (Expression, Context) =
        it match {
        case it@BitFieldType(bw) =>
            if (pattern || janusClass != Irreversible())
                (Messages.add(it, IllegalUseOfType), context)
            else {
                val (bw1, ctx1) = analyze(bw, pattern, janusClass, context)
                (it.copy(bw1), ctx1)
            }
        case it@BitRecordType(b) =>
            if (pattern || janusClass != Irreversible())
                (Messages.add(it, IllegalUseOfType), context)
            else {
                val ctx0 = analyzeDefinitions(b, context)
                val (b1, ctx1) = analyze(b, pattern, janusClass, ctx0)
                (it.copy(b1), Context(ctx1.variables.filter{
                    case (_, ContextEntry(v, l)) => !l || context.containsVariable(v)
                }))
            }
        case it@BitRegisterType(bw, b) =>
            if (pattern || janusClass != Irreversible())
                (Messages.add(it, IllegalUseOfType), context)
            else {
                val (bw1, ctx1) = analyze(bw, pattern, janusClass, context)
                val ctx0 = analyzeDefinitions(b, context)
                val (b2, ctx2) = analyze(b, pattern, janusClass, ctx0)
                (it.copy(bw1, b2), Context(ctx2.variables.filter{
                    case (_, ContextEntry(v, l)) => !l || context.containsVariable(v)
                }))
            }
        case it@BitUnionType(b) =>
            if (pattern || janusClass != Irreversible())
                (Messages.add(it, IllegalUseOfType), context)
            else {
                val ctx0 = analyzeDefinitions(b, context)
                val (b1, ctx1) = analyze(b, pattern, janusClass, ctx0)
                (it.copy(b1), Context(ctx1.variables.filter{
                    case (_, ContextEntry(v, l)) => !l || context.containsVariable(v)
                }))
            }
        case it@BitArrayType(et, u) =>
            if (pattern || janusClass != Irreversible())
                (Messages.add(it, IllegalUseOfType), context)
            else {
                val (et1, ctx1) = analyze(et, pattern, janusClass, context)
                val (u2, ctx2) = analyze(u, pattern, janusClass, context)
                (it.copy(et1, u2), ctx2)
            }
        case it@WrittenType(t, w) =>
            val (t1, ctx1) = analyze(t, pattern, janusClass, context)
            val (w2, ctx2) = analyze(w, false, Irreversible(), context)
            (it.copy(t1, w2), ctx2)
        case it@ConvertedType(t, c) =>
            val (t1, ctx1) = analyze(t, pattern, janusClass, context)
            val (c2, ctx2) = analyze(c, false, Irreversible(), context)
            (it.copy(t1, c2), ctx2)
        case it@WhereType(t, w) =>
            val (t1, ctx1) = analyze(t, pattern, janusClass, context)
            val (w2, ctx2) = analyze(w, false, Irreversible(), context)
            (it.copy(t1, w2), ctx2)
        case it@InterpretedBitType(t, i) =>
            val (t1, ctx1) = analyze(t, pattern, janusClass, context)
            val (i2, ctx2) = analyze(i, context)
            (it.copy(t1, i2), ctx2)
        case it => super.analyze(it, pattern, janusClass, context)
        }

    def analyze(it: BitTypeInterpretation, context: Context): (BitTypeInterpretation, Context) =
        it match {
        case it@EnumInterpretation(b) =>
            val ctx0 = analyzeDefinitions(b, context)
            val (b1, ctx1) = analyze(b, false, Irreversible(), ctx0)
            (it.copy(b1), Context(ctx1.variables.filter{
                case (_, ContextEntry(v, l)) => !l || context.containsVariable(v)
            }))
        case it@FixedInterpretation(fv) =>
            val (fv1, ctx1) = analyze(fv, false, Irreversible(), context)
            (it.copy(fv), ctx1)
        case it@UnitInterpretation(u) =>
            (it, context)
        case it@ContainingInterpretation(ct) =>
            val (ct1, ctx1) = analyze(ct, false, Irreversible(), context)
            (it.copy(ct1), ctx1)
        }

    override def analyze(it: Statement, pattern: Boolean, janusClass: JanusClass, context: Context): (Statement, Context) =
        it match {
        case it@BitRecordComponent(n, t) =>
            val (t1, ctx1) = analyze(t, pattern, janusClass, context)
            if (context.containsVariable(n))
                (Messages.add(it.copy(`type` = t1), VariableAlreadyDefined), ctx1)
            else
                (it.copy(`type` = t1), ctx1 + (VariableIdentity.setName(new VariableIdentity(), n), true))
        case it@BitRegisterComponent(n, p, t) =>
            val (p1, ctx1) = analyze(p, pattern, janusClass, context)
            val (t2, ctx2) = analyze(t, pattern, janusClass, context)
            if (context.containsVariable(n))
                (Messages.add(it.copy(position = p1, `type` = t2), VariableAlreadyDefined), ctx2)
            else
                (it.copy(position = p1, `type` = t2), ctx2 + (VariableIdentity.setName(new VariableIdentity(), n), true))
        case it@BitUnionVariant(n, t) =>
            val (t1, ctx1) = analyze(t, pattern, janusClass, context)
            if (context.containsVariable(n))
                (Messages.add(it.copy(`type` = t1), VariableAlreadyDefined), ctx1)
            else
                (it.copy(`type` = t1), ctx1 + (VariableIdentity.setName(new VariableIdentity(), n), true))
        case it@EnumValue(n, v) =>
            val (v1, ctx1) = analyze(v, pattern, janusClass, context)
            if (context.containsVariable(n))
                (Messages.add(it.copy(value = v1), VariableAlreadyDefined), ctx1)
            else
                (it.copy(value = v1), ctx1 + (VariableIdentity.setName(new VariableIdentity(), n), true))
        case it => super.analyze(it, pattern, janusClass, context)
        }

}

object VariableAnalyzer {
    case object IllegalUseOfType extends Message {
        override def level = Messages.Error
        override def toString = "Illegal use of type"
    }
}