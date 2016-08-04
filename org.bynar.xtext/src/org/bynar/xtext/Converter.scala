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

class Converter extends org.bynar.versailles.xtext.Converter {

    def fromBitTypeExpression(it: BitTypeExpression): v.Expression =
        it match {
        case it: BitFieldTypeExpr =>
            b.BitFieldType(fromExpression(it.getBitWidth)).putAnnotation(source, it)
        case it: ByteFieldTypeExpr =>
            b.BitFieldType(v.Application(v.Application(v.Times(), v.NumberLiteral(8)), fromExpression(it.getByteWidth))).putAnnotation(source, it)
        case it: RecordTypeExpr =>
            b.BitRecordType(it.getStatements.getStatements.map{
            case c: RecordComponent =>
                b.BitRecordComponent(Symbol(c.getName), fromTypeExpression(c.getType))
            }:_*)
        }

    override def fromTypeExpression(it: TypeExpression): v.Expression =
        it match {
        case it: BitTypeExpression =>
            fromBitTypeExpression(it)
        case it =>
            super.fromTypeExpression(it)
        }

}