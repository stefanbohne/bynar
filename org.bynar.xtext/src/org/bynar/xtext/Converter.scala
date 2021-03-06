package org.bynar.xtext

import scala.collection._
import JavaConversions._
import org.{bynar => b}
import org.bynar.{versailles => v}
import org.bynar.versailles.xtext.versaillesLang.Expression
import org.bynar.versailles.xtext.versaillesLang.TypeExpression
import org.bynar.xtext.bynarLang.BitFieldType
import org.bynar.xtext.bynarLang.RecordType
import org.bynar.xtext.bynarLang.RecordComponent
import org.bynar.xtext.bynarLang.WrittenType
import org.bynar.xtext.bynarLang.ConvertedType
import org.bynar.xtext.bynarLang.InterpretedType
import org.bynar.xtext.bynarLang.Interpretation
import org.bynar.xtext.bynarLang.EnumInterpretation
import org.bynar.xtext.bynarLang.FixedInterpretation
import org.bynar.xtext.bynarLang.UnitInterpretation
import org.bynar.xtext.bynarLang.ContainingInterpretation
import org.bynar.xtext.bynarLang.UnionVariant
import org.bynar.xtext.bynarLang.EnumValue
import org.bynar.versailles.xtext.versaillesLang.Statement
import org.bynar.xtext.bynarLang.UnionType
import org.bynar.versailles.AnnotationKey
import org.eclipse.emf.ecore.EObject
import org.bynar.xtext.bynarLang.RegisterComponent
import org.bynar.xtext.bynarLang.RegisterType
import org.bynar.versailles.DocBookGenerator
import org.bynar.versailles.Term
import org.bynar.xtext.bynarLang.ArrayType
import org.bynar.versailles.TermImplicits._
import org.bynar.versailles.xtext.versaillesLang.WhereType

class Converter extends org.bynar.versailles.xtext.Converter {
    import org.bynar.versailles.xtext.Converter._

    def fromInterpretation(it: Interpretation): b.BitTypeInterpretation =
        it match {
        case it: EnumInterpretation =>
            b.EnumInterpretation(fromBlockStmt(it.getStatements)).putAnnotation(source, it)
        case it: FixedInterpretation =>
            b.FixedInterpretation(fromExpression(it.getValue)).putAnnotation(source, it)
        case it: UnitInterpretation =>
            b.UnitInterpretation(it.getUnit).putAnnotation(source, it)
        case it: ContainingInterpretation =>
            b.ContainingInterpretation(fromTypeExpression(it.getContainedType)).putAnnotation(source, it)
        }

    override def fromTypeExpression(it: TypeExpression): v.Expression =
        new MemberConverter(Seq()).fromTypeExpression(it)

    def originalFromTypeExpression(it: TypeExpression) =
        super.fromTypeExpression(it)


}

class MemberConverter(val path: Seq[Symbol]) extends Converter {
    import org.bynar.versailles.xtext.Converter._
    import DocBookGenerator._

    def putDocs[T <: Term](it: T, title: String, description: Expression): T = {
        if (title != null)
            it.putAnnotation(titleInfo, title)
        if (description != null)
            it.putAnnotation(descriptionInfo, fromExpression(description))
        it
    }

    override def fromStatement(it: Statement): Seq[v.Statement] =
        it match {
        case it: RecordComponent =>
            Seq(putDocs(b.BitRecordComponent(Symbol(it.getName),
                        new MemberConverter(path :+ Symbol(it.getName)).fromTypeExpression(it.getType)).
                        putAnnotation(source, it),
                        it.getTitle,
                        it.getDescription))
        case it: RegisterComponent =>
            Seq(putDocs(b.BitRegisterComponent(
                        Symbol(it.getName),
                        fromIndexExpr(it.getBitPosition, true),
                        new MemberConverter(path :+ Symbol(it.getName)).fromTypeExpression(it.getType)).
                        putAnnotation(source, it),
                        it.getTitle,
                        it.getDescription))
        case it: UnionVariant =>
            Seq(putDocs(b.BitUnionVariant(
                        Symbol(it.getName),
                        new MemberConverter(path :+ Symbol(it.getName)).fromTypeExpression(it.getType)).
                        putAnnotation(source, it),
                        it.getTitle,
                        it.getDescription))
        case it: EnumValue =>
            Seq(putDocs(b.EnumValue(
                        Symbol(it.getName),
                        new MemberConverter(path :+ Symbol(it.getName)).fromExpression(it.getValue)).
                        putAnnotation(source, it),
                        it.getTitle,
                        it.getDescription))
        case _ => super.fromStatement(it)
        }

    override def fromTypeExpression(it: TypeExpression): v.Expression =
        it match {
        case it: WrittenType =>
            b.WrittenType(fromTypeExpression(it.getType),
                          new Converter().fromExpression(it.getWritten)).putAnnotation(source, it)
        case it: ConvertedType =>
            b.ConvertedType(fromTypeExpression(it.getType),
                            new Converter().fromExpression(it.getConversion)).putAnnotation(source, it)
        case it: WhereType =>
            b.WhereType(fromTypeExpression(it.getType),
                        new Converter().fromExpression(it.getWhere)).putAnnotation(source, it)
        case it: InterpretedType =>
            b.InterpretedBitType(fromTypeExpression(it.getType),
                                 fromInterpretation(it.getInterpretation)).putAnnotation(source, it)
        case it: BitFieldType =>
            b.BitFieldType(fromExpression(it.getBitWidth)).putAnnotation(source, it)
        case it: RecordType =>
            b.BitRecordType(fromBlockStmt(it.getStatements)).putAnnotation(source, it)
        case it: RegisterType =>
            b.BitRegisterType(fromExpression(it.getBitWidth),
                              fromBlockStmt(it.getStatements)).putAnnotation(source, it)
        case it: UnionType =>
            b.BitUnionType(fromBlockStmt(it.getStatements)).putAnnotation(source, it)
        case it: ArrayType =>
            val u = if (it.getUntil != null)
                fromExpression(it.getUntil)
            else {
                val x = v.VariableIdentity.setName(new v.VariableIdentity, '_)
                v.Lambda(
                    v.Irreversible().putAnnotation(source, it.getLength),
                    v.Variable(x, true).putAnnotation(source, it.getLength),
                    v.Application(v.Application(v.GreaterOrEquals().putAnnotation(source, it.getLength),
                        v.Length().putAnnotation(source, it.getLength)(((v.Variable(x, false).putAnnotation(source, it.getLength): v.Expression) /: path){ 
                            case (x, p) => v.Member(p).putAnnotation(source, it.getLength)(x).putAnnotation(source, it.getLength) 
                        }).putAnnotation(source, it.getLength)).putAnnotation(source, it.getLength),
                        v.Application(fromExpression(it.getLength), v.Variable(x, false).putAnnotation(source, it.getLength)).putAnnotation(source, it.getLength)).putAnnotation(source, it.getLength)
                ).putAnnotation(source, it.getLength)
            }
                
            b.BitArrayType(fromTypeExpression(it.getElementType), u).putAnnotation(source, it)
        case it =>
            if (path.size > 0)
                v.Application(b.MemberContextedType(path).putAnnotation(source, it),
                        new Converter().originalFromTypeExpression(it)).
                    putAnnotation(source, it)
            else
                new Converter().originalFromTypeExpression(it)
        }


}