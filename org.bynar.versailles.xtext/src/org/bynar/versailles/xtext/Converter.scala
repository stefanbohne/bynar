package org.bynar.versailles.xtext

import scala.collection._
import JavaConversions._

import org.bynar.{versailles => v}
import org.bynar.versailles.xtext.versaillesLang._
import org.eclipse.emf.ecore.EObject
import org.bynar.versailles.PrettyPrinter

object Converter {

    import PrettyPrinter._
    val source = new v.AnnotationKey[EObject]
  
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
        case it: IntegerLiteral => 
            v.NumberLiteral(it.getValue).putAnnotation(source, it)
        case it: StringLiteral => 
            v.StringLiteral(it.getValue).putAnnotation(source, it)
        case it: Variable =>
            v.Variable(v.VariableIdentity.setName(new v.VariableIdentity(), it.getName),
                       it.isLinear()).putAnnotation(source, it)
        case it: BinaryExpr =>
            val l = fromExpression(it.getLeft)
            val r = fromExpression(it.getRight)
            def normal(op: v.Expression) =
                v.Application(v.Application(op.putAnnotation(source, it.getOp),
                                            r).putAnnotation(source, it),
                              l).
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
            }
        case it: UnaryExpr =>
            val a = fromExpression(it.getExpr)
            it.getOp.getOp match {
            case "~" => v.Application(v.Reverse().
                    putAnnotation(source, it.getOp), a).
                    putAnnotation(applicationInfo, ApplicationAsOperator)
            } 
        case it: ApplicationExpr => 
            v.Application(fromExpression(it.getFunction),
                          fromExpression(it.getArgument)).
                    putAnnotation(source, it).
                    putAnnotation(applicationInfo, ApplicationAsApplication)
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
        case it: TypedExpr =>
            v.TypedExpr(fromExpression(it.getBase), fromTypeExpression(it.getType)).putAnnotation(source, it)
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
            v.Let(fromExpression(it.getPattern),
                  fromExpression(it.getValue)).putAnnotation(source, it)
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
            v.TypeDef(v.TypeVariableIdentity.setName(new v.TypeVariableIdentity(), it.getName),
                      fromTypeExpression(it.getType)).putAnnotation(source, it)
        }    
    
    def fromTypeExpression(it: TypeExpression): v.TypeExpression = 
        it match {
        case it: TypeVariable =>
            v.TypeVariable(v.TypeVariableIdentity.setName(new v.TypeVariableIdentity(), it.getName)).
                    putAnnotation(source, it)
        case it: TupleTypeExpr =>
            if (it.getPositional.size == 1 &&
                it.getNamed.size == 0 &&
                !it.isForceTuple())
                fromTypeExpression(it.getPositional.get(0))
            else
                v.TupleType(it.getPositional.map{ fromTypeExpression(_) }:_*).putAnnotation(source, it)
        }
    
}