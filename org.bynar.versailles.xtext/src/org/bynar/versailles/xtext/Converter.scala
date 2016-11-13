package org.bynar.versailles.xtext


import scala.collection._
import JavaConversions._

import org.bynar.{versailles => v}
import org.bynar.versailles.xtext.versaillesLang._
import org.eclipse.emf.ecore.EObject
import org.bynar.versailles.PrettyPrinter
import org.bynar.versailles.DocBookGenerator
import org.bynar.versailles.TermImplicits._
import org.eclipse.xtext.nodemodel.util.NodeModelUtils

class Converter {

    import PrettyPrinter._
    import DocBookGenerator._
    import Converter._

    def fromCompilationUnit(cu: CompilationUnit): v.Expression = {
        val e =
            if (cu.getExpression == null)
                v.Tuple().putAnnotation(source, cu)
            else
                fromExpression(cu.getExpression)
        if (cu.getStatements == null)
            e
        else
            v.Block(fromStatements(cu.getStatements), e).putAnnotation(source, cu)
    }

    def fromExpression(it: Expression): v.Expression =
        it match {
        case it: NumberLiteral =>
            v.NumberLiteral(it.getValue).
                    putAnnotation(source, it).
                    putAnnotation(sourceRepresentationInfo, 
                                 NodeModelUtils.getTokenText(NodeModelUtils.getNode(it)))
        case it: StringLiteral =>
            v.StringLiteral(it.getValue).putAnnotation(source, it)
        case it: InterpolatedString =>
            def stringLit(index: Int): v.Expression =
                v.StringLiteral(it.getStrings.get(index).substring(1, it.getStrings.get(index).length - 1)).putAnnotation(source, it)
            (stringLit(0) /: (0 until it.getExpressions.size)){
            case (acc, i) =>
                v.Application(
                    v.Application(
                        v.Concat().putAnnotation(source, it),
                        v.Application(
                            v.Application(
                                v.Concat().putAnnotation(source, it),
                                acc
                            ).putAnnotation(source, it),
                            fromExpression(it.getExpressions.get(i))
                        ).putAnnotation(source, it)
                    ).putAnnotation(source, it),
                    stringLit(i + 1)
                ).putAnnotation(source, it)
            }
        case it: Variable =>
            v.Variable(v.VariableIdentity.setName(new v.VariableIdentity(), Symbol(it.getName)),
                       it.isLinear()).putAnnotation(source, it)
        case it: BinaryExpr =>
            val l = fromExpression(it.getLeft)
            val r = fromExpression(it.getRight)
            def normal(op: v.Expression) =
                v.Application(v.Application(op.putAnnotation(source, it.getOp),
                                            l).putAnnotation(source, it),
                              r).
                        putAnnotation(source, it).
                        putAnnotation(applicationInfo, ApplicationAsOperator)
            def swapped(op: v.Expression) =
                v.Application(v.Application(op.putAnnotation(source, it.getOp),
                                            r).putAnnotation(source, it),
                              l).
                        putAnnotation(source, it).
                        putAnnotation(applicationInfo, ApplicationAsOperator)
            it.getOp.getOp match {
            case "+" => swapped(v.Plus())
            case "-" => swapped(v.Minus())
            case "*" => swapped(v.Times())
            case "/" => swapped(v.Divide())
            case "++" => swapped(v.Concat())
            case "==" => normal(v.Equals())
            case "!=" => normal(v.NotEquals())
            case "<" => normal(v.Less())
            case "<=" => normal(v.LessOrEquals())
            case ">" => normal(v.Greater())
            case ">=" => normal(v.GreaterOrEquals())
            case "&&" => normal(v.And())
            case "||" => normal(v.Or())
            }
        case it: UnaryExpr =>
            val a = fromExpression(it.getExpr)
            it.getOp.getOp match {
            case "~" => v.Application(v.Reverse().putAnnotation(source, it.getOp), a).
                    putAnnotation(source, it.getOp).
                    putAnnotation(applicationInfo, ApplicationAsOperator)
            case "!" => v.Application(v.Not().putAnnotation(source, it.getOp), a).
                    putAnnotation(source, it.getOp).
                    putAnnotation(applicationInfo, ApplicationAsOperator)
            case "-" => v.Application(v.Application(v.Times().putAnnotation(source, it),
                    v.NumberLiteral(-1).putAnnotation(source, it)).putAnnotation(source, it),
                    a).putAnnotation(source, it).
                    putAnnotation(applicationInfo, ApplicationAsOperator)
            }
        case it: MemberAccessExpr =>
            v.Application(v.Member(Symbol(it.getMemberName)).putAnnotation(source, it),
                          fromExpression(it.getBase)).putAnnotation(source, it)
        case it: ApplicationExpr =>
            v.Application(fromExpression(it.getFunction),
                          fromExpression(it.getArgument)).
                    putAnnotation(source, it).
                    putAnnotation(applicationInfo, ApplicationAsApplication)
        case it: TypeApplicationExpr =>
            v.Application(fromExpression(it.getFunction),
                          fromTypeExpression(it.getArgument)).
                    putAnnotation(source, it).
                    putAnnotation(applicationInfo, ApplicationAsTypeApplication)
        case it: MatchExpr =>
            v.Application(fromCaseStatements(it.getStatements),
                          fromExpression(it.getIndex)).
                    putAnnotation(source, it).
                    putAnnotation(applicationInfo, ApplicationAsMatch)
        case it: LambdaExpr =>
            v.Lambda(fromJanusClassExpression(it.getJanusClass),
                     fromExpression(it.getPattern),
                     fromExpression(it.getBody)).putAnnotation(source, it)
        case it: TupleExpr =>
            if (it.getPositional.size == 1 &&
                it.getNamed.size == 0 &&
                !it.isForceTuple())
                fromExpression(it.getPositional.get(0))
            else
                v.Tuple(it.getPositional.map{ fromExpression(_) }:_*).putAnnotation(source, it)
        case it: BlockExpr =>
            if (it.getScope == null && it.getStatements.getStatements.forall{ _.isInstanceOf[CaseStmt] })
                fromCaseStatements(it.getStatements)
            else if (it.getScope == null && it.getStatements.getStatements == null)
                v.Lambda(v.Irreversible().putAnnotation(source, it),
                         v.Undefined().putAnnotation(source, it),
                         v.Undefined().putAnnotation(source, it)).putAnnotation(source, it)
            else
                v.Block(fromStatements(it.getStatements),
                        if (it.getScope == null)
                            v.Tuple().putAnnotation(source, it)
                        else
                            fromExpression(it.getScope)).putAnnotation(source, it)
        case it: TypeExpr =>
            fromTypeExpression(it.getType)
        case it: TypedExpr =>
            v.Application(v.Application(v.Typed().putAnnotation(source, it),
                                        fromTypeExpression(it.getType)).putAnnotation(source, it),
                          fromExpression(it.getBase)).putAnnotation(source, it).putAnnotation(v.PrettyPrinter.applicationInfo, v.PrettyPrinter.ApplicationAsOperator)
        case it: ListExpr =>
            fromIndexExpr(it.getIndices, false).putAnnotation(applicationInfo, ApplicationAsList)
            
    }

    def fromIndexExpr(it: IndexExpr, rangeOnly: Boolean): v.Expression =
        it match {
        case it: SingletonIndexExpr =>
            if (rangeOnly) {
                val i = v.VariableIdentity.setName(new v.VariableIdentity, 'i)
                v.Block(v.Let(v.Variable(i, true).putAnnotation(source, it), fromExpression(it.getIndex)).putAnnotation(source, it),
                        v.Application(v.Application(
                                v.RangeIndexInclusive().putAnnotation(source, it),
                                v.Variable(i, false).putAnnotation(source, it)).putAnnotation(source, it),
                                v.Variable(i, false).putAnnotation(source, it)).putAnnotation(source, it)).putAnnotation(source, it)
            } else
                v.Application(v.SingletonIndex().putAnnotation(source, it),
                              fromExpression(it.getIndex)).putAnnotation(source, it)
        case it: RangeIndexExpr =>
            v.Application(v.Application((if (it.isInclusive) v.RangeIndexInclusive() else v.RangeIndex()).putAnnotation(source, it),
                    fromExpression(it.getFrom.getIndex)).putAnnotation(source, it),
                    fromExpression(it.getTo)).putAnnotation(source, it)
        case it: SequenceIndexExpr =>
            v.Application(v.Application(v.IndexConcatenation().putAnnotation(source, it),
                    fromIndexExpr(it.getFirst, true)).putAnnotation(source, it),
                    fromIndexExpr(it.getSecond, true)).putAnnotation(source, it)
        }

    def fromCaseStatements(it: Statements): v.Expression =
        it.getStatements.map{ case s: CaseStmt => fromExpression(s.getCase) }.reduceRight[v.Expression]{
        case (c, r) =>
            v.Application(v.Application(v.OrElse().putAnnotation(source, it),
                                        c).putAnnotation(source, it),
                          r).putAnnotation(source, it)
        }

    def fromJanusClassExpression(it: JanusClassExpression): v.Expression =
        it match {
        case it: JanusClass =>
            it.getOp match {
            case "->" => v.Irreversible().putAnnotation(source, it)
            case "<-" => v.ReverseIrreversible().putAnnotation(source, it)
            case "<->" => v.Inverse().putAnnotation(source, it)
            case ">->" => v.SemiInverse().putAnnotation(source, it)
            case "<-<" => v.ReverseSemiInverse().putAnnotation(source, it)
            case ">-<" => v.PseudoInverse().putAnnotation(source, it)
            case "<>-<>" => v.Reversible().putAnnotation(source, it)
            }
    }

    def fromStatements(it: Statements): v.Statement =
        if (it == null)
            v.Sequence()
        else {
            val ss = it.getStatements.flatMap{ fromStatement(_) }
            if (ss.size == 1)
                ss(0)
            else
                v.Sequence(ss:_*).putAnnotation(source, it)
        }

    def fromStatement(it: Statement): Seq[v.Statement] =
        it match {
        case it: PassStmt =>
            Seq(v.Sequence().putAnnotation(source, it))
        case it: FailStmt =>
            Seq(v.Fail().putAnnotation(source, it))
        case it: LetStmt =>
            if (it.getValue() != null)
                Seq(v.Let(fromExpression(it.getPattern),
                          fromExpression(it.getValue)).putAnnotation(source, it))
            else
                Seq(v.Let(v.BooleanLiteral(true).putAnnotation(source, it),
                          fromExpression(it.getPattern)).
                             putAnnotation(source, it).
                             putAnnotation(letInfo, LetAsAssert))
        case it: IfStmt =>
            Seq(v.IfStmt(fromExpression(it.getCondition),
                         fromStatements(it.getThen),
                         fromStatements(it.getElse),
                         if (it.getAssertion == null)
                             v.Undefined().putAnnotation(source, it)
                         else
                             fromExpression(it.getAssertion)).putAnnotation(source, it))
        case it: ForgetStmt =>
            Seq(v.Let(
                v.Application(
                    v.Application(
                        v.Forget().putAnnotation(source, it),
                        v.Lambda(
                            v.Irreversible().putAnnotation(source, it),
                            v.Tuple().putAnnotation(source, it),
                            fromExpression(it.getValue())
                        ).putAnnotation(source, it)
                    ).putAnnotation(source, it),
                    v.Tuple().putAnnotation(source, it)
                ).putAnnotation(source, it),
                fromExpression(it.getPattern)).putAnnotation(source, it))
        case it: RememberStmt =>
            Seq(v.Let(
                fromExpression(it.getPattern),
                v.Application(
                    v.Application(v.Reverse().putAnnotation(source, it),
                        v.Application(
                            v.Forget().putAnnotation(source, it),
                            v.Lambda(
                                v.Irreversible().putAnnotation(source, it),
                                v.Tuple().putAnnotation(source, it),
                                fromExpression(it.getValue())
                            ).putAnnotation(source, it)
                        ).putAnnotation(source, it)
                    ).putAnnotation(source, it),
                    v.Tuple().putAnnotation(source, it)
                ).putAnnotation(source, it)
            ).putAnnotation(source, it))
        case it: TypeStmt =>
            val t = fromTypeExpression(it.getType)
            val t2 = if (it.getTypeArguments != null)
                v.Lambda(v.Irreversible().putAnnotation(source, it), fromTupleTypeType(it.getTypeArguments), t).putAnnotation(source, it)
            else
                t
            val id = v.VariableIdentity.setName(new v.VariableIdentity(), Symbol(it.getName))
            val result = if (it.isLet)
                    v.Let(v.Variable(id, true).putAnnotation(source, it), t2).
                        putAnnotation(source, it).
                        putAnnotation(letInfo, LetAsType)
                else
                    v.Def(id, t2).
                        putAnnotation(source, it)
            if (it.getTitle != null)
                id.putAnnotation(titleInfo, it.getTitle)
            if (it.getDescription != null)
                id.putAnnotation(descriptionInfo,
                        v.Lambda(v.Irreversible(),
                                 v.Variable(v.VariableIdentity.setName(new v.VariableIdentity(), 'it), true),
                                 fromExpression(it.getDescription)))
            Seq(result)
        case it: DefStmt =>
            val args = (if (it.getTypeArguments != null) Seq(fromTupleTypeType(it.getTypeArguments)) else Seq()) ++
                (for (a <- it.getArguments) yield fromExpression(a))
            val (body, jc) = if (it.getValueType != null) {
                // value with type
                val x = fromExpression(it.getValue)
                val t = fromTypeExpression(it.getValueType)
                (v.Application(v.Application(v.Typed().putAnnotation(source, it), t).putAnnotation(source, it), x).putAnnotation(source, it), 
                        v.Irreversible().putAnnotation(source, it))
            } else {
                assert(it.getValue == null && it.getValueType == null)
                val b = if (it.getStatements == null) {
                    // function with expression
                    fromExpression(it.getResults)
                } else {
                    // function with statements
                            v.Block(fromStatements(it.getStatements),
                                    fromExpression(it.getResults)).putAnnotation(source, it)
                }
                (b, fromJanusClassExpression(it.getJanusClass))
            }
            val value = if (args.isEmpty) body else 
                (args.take(args.size - 1) :\ v.Lambda(jc, args.last, body).putAnnotation(source, it)){
                case (a, b) => v.Lambda(v.Irreversible().putAnnotation(source, it), a, b).putAnnotation(source, it)
                }

            val id = v.VariableIdentity.setName(new v.VariableIdentity(), Symbol(it.getName))
            val result = if (it.isLet)
                    v.Let(v.Variable(id, true).putAnnotation(source, it), value).
                        putAnnotation(source, it).
                        putAnnotation(letInfo, LetAsLet)
                else
                    v.Def(id, value).
                        putAnnotation(source, it)
            if (it.getTitle != null)
                id.putAnnotation(titleInfo, it.getTitle)
            if (it.getDescription != null)
                id.putAnnotation(descriptionInfo,
                        v.Lambda(v.Irreversible().putAnnotation(source, it),
                                 v.Variable(v.VariableIdentity.setName(new v.VariableIdentity(), 'it), true).putAnnotation(source, it),
                                 fromExpression(it.getDescription).putAnnotation(source, it)))
            val result2: Seq[v.Statement] = if (it.getName2 == null) Seq(result) else {
                val v2 = ((v.Variable(id, false): v.Expression).putAnnotation(source, it) /: args.take(args.size - 1)){
                         case (a, b) => v.Application(a, b.treeMap{ case x@v.Variable(_, true) => x.copy(linear=false); case x => x }.asInstanceOf[v.Expression]).putAnnotation(source, it)
                         }
                val v3 = (args.take(args.size - 1) :\ (v.Application(v.Reverse().putAnnotation(source, it), v2).putAnnotation(source, it): v.Expression)){
                         case (a, b) => v.Lambda(v.Irreversible().putAnnotation(source, it), a.deepCopy(), b).putAnnotation(source, it)
                         }
                val id2 = v.VariableIdentity.setName(new v.VariableIdentity, Symbol(it.getName2))
                val r2 = if (it.isLet)
                    v.Let(v.Variable(id2, true).putAnnotation(source, it), v3).putAnnotation(source, it)
                else
                    v.Def(id2, v3).putAnnotation(source, it)
                if (it.getTitle2 != null)
                    id2.putAnnotation(titleInfo, it.getTitle2)
                if (it.getDescription2 != null)
                    id2.putAnnotation(descriptionInfo,
                            v.Lambda(v.Irreversible().putAnnotation(source, it),
                                     v.Variable(v.VariableIdentity.setName(new v.VariableIdentity(), 'it), true).putAnnotation(source, it),
                                     fromExpression(it.getDescription2).putAnnotation(source, it)))
                Seq(result, r2)
            }
            result2
        case it: ModuleStmt =>
            val id = v.VariableIdentity.setName(new v.VariableIdentity, Symbol(it.getPath.getSteps.last))
            if (it.getTitle != null)
                id.putAnnotation(titleInfo, it.getTitle)
            if (it.getDescription != null)
                id.putAnnotation(descriptionInfo,
                        v.Lambda(v.Irreversible().putAnnotation(source, it),
                                 v.Variable(v.VariableIdentity.setName(new v.VariableIdentity(), 'it), true).putAnnotation(source, it),
                                 fromExpression(it.getDescription).putAnnotation(source, it)))
            val base = v.Def(id, v.Module(fromStatements(it.getStatements)).putAnnotation(source, it)).putAnnotation(source, it)
            Seq((base /: it.getPath.getSteps.take(it.getPath.getSteps.size - 1)){
            case (i, n) => 
                v.Def(v.VariableIdentity.setName(new v.VariableIdentity, Symbol(n)), 
                        v.Module(i).putAnnotation(source, it)).putAnnotation(source, it)
            })
        }

    def fromTypeExpression(it: TypeExpression): v.Expression =
        it match {
        case it: TypeVariable =>
            v.Variable(v.VariableIdentity.setName(new v.VariableIdentity(), Symbol(it.getName)), false).
                    putAnnotation(source, it)
        case it: TupleTypeExpr =>
            if (it.getPositional.size == 1 &&
                it.getNamed.size == 0 &&
                !it.isForceTuple())
                fromTypeExpression(it.getPositional.get(0))
            else
                v.TupleType(it.getPositional.map{ fromTypeExpression(_) }:_*).putAnnotation(source, it)
        case it: TypeConcretion =>
            v.Application(
                fromTypeExpression(it.getBase),
                fromTypeExpression(it.getArgument)
            ).putAnnotation(source, it).putAnnotation(applicationInfo, ApplicationAsTypeApplication)
        case it: TypeValueConcretion =>
            v.Application(
                fromTypeExpression(it.getFunction),
                fromExpression(it.getArgument)
            ).putAnnotation(source, it).putAnnotation(applicationInfo, ApplicationAsApplication)
        case it: TypeMemberAccessExpr =>
            v.Application(v.Member(Symbol(it.getMemberName)).putAnnotation(source, it),
                          fromTypeExpression(it.getBase)).putAnnotation(source, it)
        case it: ValueType =>
            fromExpression(it.getValue)
        }

    def fromTupleTypeType(it: TupleTypeTypeExpr): v.Expression =
        if (it.getArguments.size == 1 &&
                !it.isForceTuple())
            v.Variable(v.VariableIdentity.setName(new v.VariableIdentity, Symbol(it.getArguments.get(0).getName)), true).
                putAnnotation(source, it.getArguments.get(0))
        else
            v.TupleType(it.getArguments.map{
            case tv =>
                v.Variable(v.VariableIdentity.setName(new v.VariableIdentity, Symbol(tv.getName)), true).
                    putAnnotation(source, tv)
            }:_*).putAnnotation(source, it)

}

object Converter {
    val source = new v.AnnotationKey[EObject]
}