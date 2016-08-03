package org.bynar.versailles

class VariableAnalyzer {
    
    import VariableAnalyzer._
    
    case class ContextEntry(val identity: VariableIdentity,
                            val linear: Boolean)
    case class Context(val variables: Map[String, ContextEntry],
                       val typeVariables: Map[String, TypeVariableIdentity]) {
        def +(variable: VariableIdentity, linear: Boolean): Context =
            Context(variables + (VariableIdentity.getName(variable) -> ContextEntry(variable, linear)),
                    typeVariables)
        def -(variable: VariableIdentity): Context = {
            val n = VariableIdentity.getName(variable)
            assert(containsVariable(variable) && variables(n).linear) 
            Context(variables - n, typeVariables)
        }
        def asNonlinear() =
            Context(variables.mapValues{ case ContextEntry(v, l) => ContextEntry(v, false) }, typeVariables) 
        def containsVariable(name: String): Boolean =
            variables.contains(name)
        def containsVariable(variable: VariableIdentity): Boolean =
            variables.get(VariableIdentity.getName(variable)).map{ e => e.identity eq variable }.getOrElse(false)

        def +(typeVariable: TypeVariableIdentity): Context =
            Context(variables, 
                    typeVariables + (TypeVariableIdentity.getName(typeVariable) -> typeVariable))
        def containsTypeVariable(name: String): Boolean =
            typeVariables.contains(name)
        def containsTypeVariable(typeVariable: TypeVariableIdentity): Boolean =
            typeVariables.get(TypeVariableIdentity.getName(typeVariable)).map{ tv => tv eq typeVariable }.getOrElse(false)
    }
    
    def analyze(it: Expression, variables: Set[VariableIdentity], typeVariables: Set[TypeVariableIdentity]): Expression =
        analyze(it, false, Irreversible(), Context(
                Map(variables.toSeq.map{ case id => VariableIdentity.getName(id) -> ContextEntry(id, false) }:_*),
                Map(typeVariables.toSeq.map{ case id => TypeVariableIdentity.getName(id) -> id }:_*)
        ))._1
    def analyze(it: Expression, pattern: Boolean, janusClass: JanusClass, context: Context): (Expression, Context) =
        it match {
        case it: Literal => (it, context)
        case it@Variable(id, true) =>
            val n = VariableIdentity.getName(id)
            if (!pattern)
                if (context.containsVariable(n))
                    if (context.variables(n).linear)
                        (it.copy(context.variables(n).identity), context - context.variables(n).identity)
                    else
                        (Messages.add(it.copy(context.variables(n).identity), NonlinearVariableUsedLinearly), context)
                else
                    (Messages.add(it, UndefinedVariable), context)
            else
                if (context.containsVariable(n))
                    (Messages.add(it, VariableAlreadyDefined), context + (id, true))
                else
                    (it, context + (id, true))
        case it@Variable(id, false) =>
            val n = VariableIdentity.getName(id)
            if (!pattern)
                if (context.containsVariable(n))
                    (it.copy(context.variables(n).identity), context)
                else
                    (Messages.add(it, UndefinedVariable), context)
            else
                if (context.containsVariable(n))
                    (Messages.add(it.copy(context.variables(n).identity), VariableAsConstantPattern), context)
                else
                    (it.copy(id, true), context + (id, true))
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
                        (c3 +: cs2, ctx3)
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
                (Messages.add(it, LambdaAsPattern), context)
            } else if (janusClass != Irreversible()) {
                (Messages.add(it, LambdaUsedLinearly), context)
            } else {
                val (jc1, ctx1) = analyze(jc, false, Irreversible(), context)
                assert(ctx1 == context)
                val (p2, ctx2) = analyze(p, true, jc1.asInstanceOf[JanusClass].reverse, context.asNonlinear())
                val (b3, ctx3) = analyze(b, false, jc1.asInstanceOf[JanusClass], ctx2)
                if (jc1.asInstanceOf[JanusClass] <= Reversible()) 
                    for (ContextEntry(id, linear) <- ctx3.variables.values if linear)
                        Messages.add(id, UnconsumedVariable)
                (it.copy(jc1, p2, b3), context)
            }
        case it@Block(b, s) =>
            val ctx0 = analyzeDefinitions(b, context)
            if (!pattern) {
                val (b1, ctx1) = analyze(b, pattern, janusClass, ctx0)
                val (s2, ctx2) = analyze(s, pattern, janusClass, ctx1)
                if (janusClass <= Reversible())
                    for (ContextEntry(v2, l2) <- ctx2.variables.values if l2 && !context.containsVariable(v2))
                        Messages.add(v2, UnconsumedVariable) 
                (it.copy(b1, s2), Context(ctx2.variables.filter{ 
                    case (_, ContextEntry(v, l)) => !l || context.containsVariable(v) 
                }, context.typeVariables))
            } else {
                val (s1, ctx1) = analyze(s, pattern, janusClass, ctx0)
                val (b2, ctx2) = analyze(b, pattern, janusClass, ctx1)
                (it.copy(b2, s1), ctx2)
            }
        case it@TypedExpr(e, t) =>
            val (e1, ctx1) = analyze(e, pattern, janusClass, context)
            val (t2, ctx2) = analyze(t, ctx1)
            (it.copy(e1, t2), ctx2)
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
        case it@TypeDef(id, t) =>
            assert(context.containsTypeVariable(id))
            val (t1, ctx1) = analyze(t, context)
            (it.copy(`type` = t1), ctx1)
        }
    
    def analyze(it: TypeExpression, context: Context): (TypeExpression, Context) =
        it match {
        case it: TypeLiteral =>
            (it, context)
        case it@TypeVariable(id) =>
            val n = TypeVariableIdentity.getName(id)
            if (context.containsTypeVariable(n))
                (it.copy(context.typeVariables(n)), context)
            else
                (Messages.add(it, UndefinedType), context)
        case it@TupleType(cts@_*) =>
            val (cts1, ctx1) = ((Seq[TypeExpression](), context) /: cts) {
                case ((cts2, ctx2), ct) =>
                    val (ct3, ctx3) = analyze(ct, ctx2)
                    (cts2 :+ ct3, ctx3)
            }
            (it.copy(cts1:_*), ctx1)
        }
    
    def analyzeDefinitions(it: Statement, context: Context): Context = 
        it match {
        case TypeDef(id, _) =>
            val n = TypeVariableIdentity.getName(id)
            if (context.typeVariables.contains(n)) {
                Messages.add(it, TypeAlreadyDefined)
                context
            } else
                context + id
        case Sequence(ss@_*) =>
                (context /: ss){
                    case (ctx2, s) => 
                        analyzeDefinitions(s, ctx2)
                }
        case _ =>
            context
        }        
}

object VariableAnalyzer {
    case object TypeAlreadyDefined extends Message {
        override def toString = "Type already defined"
        override def level = Messages.Error
    }
    case object UndefinedType extends Message {
        override def toString = "Undefined type"
        override def level = Messages.Error
    }
    case object VariableAlreadyDefined extends Message {
        override def toString = "Variable already defined"
        override def level = Messages.Error
    }
    case object VariableAsConstantPattern extends Message {
        override def toString = "Variable used as constant pattern"
        override def level = Messages.Info
    }
    case object UndefinedVariable extends Message {
        override def toString = "Undefined variable"
        override def level = Messages.Error
    }
    case object UnconsumedVariable extends Message {
        override def toString = "Unconsumed variable"
        override def level = Messages.Error
    }
    case object LinearVariableUsedNonlinearly extends Message {
        override def toString = "Linear variable used non-linearly"
        override def level = Messages.Error
    }
    case object NonlinearVariableUsedLinearly extends Message {
        override def toString = "Non-linear variable used linearly"
        override def level = Messages.Error
    }
    case object LambdaAsPattern extends Message {
        override def toString = "Lambda expressions cannot be used as a pattern"
        override def level = Messages.Error
    }
    case object LambdaUsedLinearly extends Message {
        override def toString = "Lambda expression cannot be used linearly"
        override def level = Messages.Error
    }
}