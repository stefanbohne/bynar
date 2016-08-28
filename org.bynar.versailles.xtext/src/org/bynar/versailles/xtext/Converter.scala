package org.bynar.versailles.xtext

import scala.collection._
import JavaConversions._

import org.bynar.{versailles => v}
import org.bynar.versailles.xtext.versaillesLang._
import org.eclipse.emf.ecore.EObject
import org.bynar.versailles.PrettyPrinter
import org.bynar.versailles.DocBookGenerator

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
            v.NumberLiteral(it.getValue).putAnnotation(source, it)
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
        }
    
    def fromIndexExpr(it: IndexExpr): v.Expression =
        it match {
        case it: SingletonIndexExpr =>
            v.Application(v.SingletonIndex().putAnnotation(source, it),
                          fromExpression(it.getIndex)).putAnnotation(source, it)
        case it: RangeIndexExpr =>
            v.Application(v.Application(v.RangeIndex().putAnnotation(source, it),
                    fromExpression(it.getFrom.getIndex)).putAnnotation(source, it),
                    fromExpression(it.getTo)).putAnnotation(source, it)
        case it: SequenceIndexExpr =>
            v.Application(v.Application(v.ConcatIndex().putAnnotation(source, it),
                    fromIndexExpr(it.getFirst)).putAnnotation(source, it),
                    fromIndexExpr(it.getSecond)).putAnnotation(source, it)
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
            val ss = it.getStatements.map{ fromStatement(_) }
            if (ss.size == 1)
                ss(0)
            else
                v.Sequence(ss:_*).putAnnotation(source, it)
        }

    def fromStatement(it: Statement): v.Statement =
        it match {
        case it: PassStmt =>
            v.Sequence().putAnnotation(source, it)
        case it: FailStmt =>
            v.Sequence().putAnnotation(source, it)
        case it: LetStmt =>
            if (it.getValue() != null)
                v.Let(fromExpression(it.getPattern),
                      fromExpression(it.getValue)).putAnnotation(source, it)
            else
                v.Let(v.BooleanLiteral(true).putAnnotation(source, it),
                      fromExpression(it.getPattern)).
                          putAnnotation(source, it).
                          putAnnotation(letInfo, LetAsAssert)
        case it: IfStmt =>
            v.IfStmt(fromExpression(it.getCondition),
                     fromStatements(it.getThen),
                     fromStatements(it.getElse),
                     if (it.getAssertion == null)
                         v.Undefined().putAnnotation(source, it)
                     else
                         fromExpression(it.getAssertion)).putAnnotation(source, it)
        case it: ForgetStmt =>
            v.Let(
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
                fromExpression(it.getPattern)).putAnnotation(source, it)
        case it: RememberStmt =>
            v.Let(
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
            ).putAnnotation(source, it)
        case it: TypeStmt =>
            val t = fromTypeExpression(it.getType)
            val t2 = if (it.getTypeArguments != null)
                v.Lambda(v.Irreversible(), fromTupleTypeType(it.getTypeArguments), t)
            else
                t
            val result = if (it.isLet)
                    v.Let(v.Variable(v.VariableIdentity.setName(new v.VariableIdentity(), Symbol(it.getName)), true).putAnnotation(source, it), t2).
                        putAnnotation(source, it).
                        putAnnotation(letInfo, LetAsType)
                else
                    v.Def(v.VariableIdentity.setName(new v.VariableIdentity(), Symbol(it.getName)), t2).
                        putAnnotation(source, it)
            if (it.getTitle != null)
                result.putAnnotation(titleInfo, it.getTitle)
            if (it.getDescription != null)
                result.putAnnotation(descriptionInfo,
                        v.Lambda(v.Irreversible(),
                                 v.Variable(v.VariableIdentity.setName(new v.VariableIdentity(), 'it), true),
                                 fromExpression(it.getDescription)))
            result
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
            }:_*)

}

object Converter {
    val source = new v.AnnotationKey[EObject]
}