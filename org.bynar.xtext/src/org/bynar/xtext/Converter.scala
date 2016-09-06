package org.bynar.xtext

import scala.collection._
import JavaConversions._
import org.{bynar => b}
import org.bynar.{versailles => v}
import org.bynar.versailles.xtext.versaillesLang.Expression
import org.bynar.versailles.xtext.versaillesLang.TypeExpression
import org.bynar.xtext.bynarLang.BitFieldTypeExpr
import org.bynar.xtext.bynarLang.ByteFieldTypeExpr
import org.bynar.xtext.bynarLang.RecordTypeExpr
import org.bynar.xtext.bynarLang.RecordComponent
import org.bynar.xtext.bynarLang.BitTypeExpression
import org.bynar.xtext.bynarLang.TypeWrittenExpr
import org.bynar.xtext.bynarLang.TypeConvertedExpr
import org.bynar.xtext.bynarLang.TypeWhereExpr
import org.bynar.xtext.bynarLang.TypeWithInterpretationExpr
import org.bynar.xtext.bynarLang.Interpretation
import org.bynar.xtext.bynarLang.EnumInterpretation
import org.bynar.xtext.bynarLang.FixedInterpretation
import org.bynar.xtext.bynarLang.UnitInterpretation
import org.bynar.xtext.bynarLang.ContainingInterpretation
import org.bynar.xtext.bynarLang.UnionVariant
import org.bynar.xtext.bynarLang.EnumValue
import org.bynar.versailles.xtext.versaillesLang.Statement
import org.bynar.xtext.bynarLang.UnionTypeExpr
import org.bynar.versailles.AnnotationKey
import org.eclipse.emf.ecore.EObject
import org.bynar.xtext.bynarLang.RegisterComponent
import org.bynar.xtext.bynarLang.RegisterTypeExpr
import org.bynar.versailles.DocBookGenerator
import org.bynar.versailles.Term

class Converter extends org.bynar.versailles.xtext.Converter {
    import org.bynar.versailles.xtext.Converter._

    def fromInterpretation(it: Interpretation): b.BitTypeInterpretation =
        it match {
        case it: EnumInterpretation =>
            b.EnumInterpretation(fromStatements(it.getStatements)).putAnnotation(source, it)
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

    override def fromStatement(it: Statement): v.Statement =
        it match {
        case it: RecordComponent =>
            putDocs(b.BitRecordComponent(Symbol(it.getName),
                    new MemberConverter(path :+ Symbol(it.getName)).fromTypeExpression(it.getType)).
                    putAnnotation(source, it),
                    it.getTitle,
                    it.getDescription)
        case it: RegisterComponent =>
            putDocs(b.BitRegisterComponent(
                    Symbol(it.getName),
                    fromIndexExpr(it.getBitPosition, true),
                    new MemberConverter(path :+ Symbol(it.getName)).fromTypeExpression(it.getType)).
                    putAnnotation(source, it),
                    it.getTitle,
                    it.getDescription)
        case it: UnionVariant =>
            putDocs(b.BitUnionVariant(
                    Symbol(it.getName),
                    new MemberConverter(path :+ Symbol(it.getName)).fromTypeExpression(it.getType)).
                    putAnnotation(source, it),
                    it.getTitle,
                    it.getDescription)
        case it: EnumValue =>
            putDocs(b.EnumValue(
                    Symbol(it.getName),
                    new MemberConverter(path :+ Symbol(it.getName)).fromExpression(it.getValue)).
                    putAnnotation(source, it),
                    it.getTitle,
                    it.getDescription)
        case _ => super.fromStatement(it)
        }

    override def fromTypeExpression(it: TypeExpression): v.Expression =
        it match {
        case it: TypeWrittenExpr =>
            b.WrittenType(fromTypeExpression(it.getType),
                          new Converter().fromExpression(it.getWritten)).putAnnotation(source, it)
        case it: TypeConvertedExpr =>
            b.ConvertedType(fromTypeExpression(it.getType),
                            new Converter().fromExpression(it.getConversion)).putAnnotation(source, it)
        case it: TypeWhereExpr =>
            b.WhereType(fromTypeExpression(it.getType),
                        new Converter().fromExpression(it.getWhere)).putAnnotation(source, it)
        case it: TypeWithInterpretationExpr =>
            b.InterpretedBitType(fromTypeExpression(it.getType),
                                 fromInterpretation(it.getInterpretation)).putAnnotation(source, it)
        case it: BitFieldTypeExpr =>
            b.BitFieldType(fromExpression(it.getBitWidth)).putAnnotation(source, it)
        case it: ByteFieldTypeExpr =>
            b.BitFieldType(
                    v.Application(v.Application(v.Times().putAnnotation(source, it), v.NumberLiteral(8).putAnnotation(source, it)).putAnnotation(source, it), fromExpression(it.getByteWidth))).
                putAnnotation(source, it)
        case it: RecordTypeExpr =>
            b.BitRecordType(fromStatements(it.getStatements)).putAnnotation(source, it)
        case it: RegisterTypeExpr =>
            b.BitRegisterType(fromExpression(it.getBitWidth),
                              fromStatements(it.getStatements)).putAnnotation(source, it)
        case it: UnionTypeExpr =>
            b.BitUnionType(fromStatements(it.getStatements)).putAnnotation(source, it)
        case it =>
            if (path.size > 0)
                v.Application(b.MemberContextedType(path).putAnnotation(source, it),
                        new Converter().originalFromTypeExpression(it)).
                    putAnnotation(source, it)
            else
                new Converter().originalFromTypeExpression(it)
        }


}