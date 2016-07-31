package org.bynar.versailles

class VariableAnalyzer {
    
    case class ContextEntry(val variable: VariableIdentity,
                            val linear: Boolean)
    case class Context(val entries: Map[String, ContextEntry]) {
        def apply(name: String): ContextEntry =
            entries(name)
        def +(variable: VariableIdentity, linear: Boolean): Context =
            Context(entries + (VariableIdentity.getName(variable) -> ContextEntry(variable, linear)))
        def -(variable: VariableIdentity): Context = {
            val n = VariableIdentity.getName(variable)
            assert(contains(variable) && entries(n).linear) 
            Context(entries - n)
        }
        def asNonlinear() =
            Context(entries.mapValues{ case ContextEntry(v, l) => ContextEntry(v, false) }) 
        def contains(name: String): Boolean =
            entries.contains(name)
        def contains(variable: VariableIdentity): Boolean =
            entries.get(VariableIdentity.getName(variable)).map{ e => e.variable eq variable }.getOrElse(false)
    }
    
    def analyze(it: Expression, pattern: Boolean, janusClass: JanusClass, context: Context): (Expression, Context) =
        it match {
        case it: Literal => (it, context)
        case it@Variable(id, true) =>
            val n = VariableIdentity.getName(id)
            if (!pattern)
                if (context.contains(n))
                    if (context(n).linear)
                        (it.copy(context(n).variable), context - context(n).variable)
                    else
                        (Messages.addError(it.copy(context(n).variable), "Nonlinear variable used linearly"), context)
                else
                    (Messages.addError(it, "Undefined variable"), context)
            else
                if (context.contains(n))
                    (Messages.addError(it, "Already defined"), context + (id, true))
                else
                    (it, context + (id, true))
        case it@Variable(id, false) =>
            val n = VariableIdentity.getName(id)
            if (!pattern)
                if (context.contains(n))
                    (it.copy(context(n).variable), context)
                else
                    (Messages.addError(it, "Undefined variable"), context)
            else
                if (context.contains(n))
                    (it.copy(context(n).variable), context)
                else
                    (Messages.addInfo(it.copy(id, true), "Variable defined"), context + (id, true))
        case it@Tuple(cs@_*) =>
            if (!pattern) {
                val (cs1, ctx1) = ((Seq[Expression](), context) /: cs){
                    case ((cs2, ctx2), c) => 
                        val (c3, ctx3) = analyze(c, pattern, janusClass, ctx2)
                        (cs2 :+ c3, ctx3)
                }
                (it.copy(cs1:_*), ctx1)
            } else {
                val (cs1, ctx1) = (cs :\ (Seq[Expression](), context)){
                    case (c, (cs2, ctx2)) => 
                        val (c3, ctx3) = analyze(c, pattern, janusClass, ctx2)
                        (cs2 :+ c3, ctx3)
                }
                (it.copy(cs1:_*), ctx1)
            }
        case it@Application(f, a) =>
            if (!pattern) {
                val (a1, ctx1) = analyze(a, pattern, janusClass, context)
                val (f2, ctx2) = analyze(f, false, Irreversible(), ctx1.asNonlinear())
                assert(ctx2 == ctx1.asNonlinear())
                (it.copy(f2, a1), ctx1)
            } else {
                val (f1, ctx1) = analyze(f, false, Irreversible(), context.asNonlinear())
                assert(ctx1 == context.asNonlinear())
                val (a2, ctx2) = analyze(a, pattern, janusClass, context)
                (it.copy(f1, a2), ctx2)
            }
        case it@Lambda(jc, p, b) =>
            if (pattern) {
                (Messages.addError(it, "Lambda as pattern"), context)
            } else if (janusClass != Irreversible()) {
                (Messages.addError(it, "Lambda in linear context"), context)
            } else {
                val (jc1, ctx1) = analyze(jc, false, Irreversible(), context)
                assert(ctx1 == context)
                val (p2, ctx2) = analyze(p, true, jc1.asInstanceOf[JanusClass].reverse, context.asNonlinear())
                val (b3, ctx3) = analyze(b, false, jc1.asInstanceOf[JanusClass], ctx2)
                if (jc1.asInstanceOf[JanusClass] <= Reversible()) 
                    for (ContextEntry(id, linear) <- ctx3.entries.values if linear)
                        Messages.addError(id, "Unconsumed variable")
                (it.copy(jc1, p2, b3), context)
            }
        case it@Block(b, s) =>
            if (!pattern) {
                val (b1, ctx1) = analyze(b, pattern, janusClass, context)
                val (s2, ctx2) = analyze(s, pattern, janusClass, ctx1)
                if (janusClass <= Reversible())
                    for (ContextEntry(v2, l2) <- ctx2.entries.values if l2 && !context.contains(v2))
                        Messages.addError(v2, "Unconsumed variable") 
                (it.copy(b1, s2), Context(ctx2.entries.filter{ 
                    case (_, ContextEntry(v, l)) => !l || context.contains(v) 
                }))
            } else {
                val (s1, ctx1) = analyze(s, pattern, janusClass, context)
                val (b2, ctx2) = analyze(b, pattern, janusClass, ctx1)
                (it.copy(b2, s1), ctx2)
            }
        }
    
    def analyze(it: Statement, pattern: Boolean, janusClass: JanusClass, context: Context): (Statement, Context) =
        it match {
        case it@Let(p, v) =>
            if (!pattern) {
                val (v1, ctx1) = analyze(v, pattern, janusClass, context)
                val (p2, ctx2) = analyze(p, !pattern, janusClass.reverse, ctx1)
                (it.copy(p2, v1), ctx2)
            } else {
                val (p1, ctx1) = analyze(p, pattern, janusClass, context)
                val (v2, ctx2) = analyze(v, !pattern, janusClass.reverse, ctx1)
                (it.copy(p1, v2), ctx2)
            }
        case it@Sequence(ss@_*) =>
            if (!pattern) {
                val (ss1, ctx1) = ((Seq[Statement](), context) /: ss){
                    case ((ss2, ctx2), s) => 
                        val (s3, ctx3) = analyze(s, pattern, janusClass, ctx2)
                        (ss2 :+ s3, ctx3)
                }
                (it.copy(ss1:_*), ctx1)
            } else {
                val (ss1, ctx1) = (ss :\ (Seq[Statement](), context)){
                    case (s, (ss2, ctx2)) => 
                        val (s3, ctx3) = analyze(s, pattern, janusClass, ctx2)
                        (ss2 :+ s3, ctx3)
                }
                (it.copy(ss1:_*), ctx1)
            }
        }
}