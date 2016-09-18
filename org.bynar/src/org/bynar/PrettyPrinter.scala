package org.bynar

import org.bynar.versailles.Term
import org.apache.commons.lang3.StringEscapeUtils
import org.bynar.versailles.Statement
import org.bynar.versailles.Application

class PrettyPrinter extends org.bynar.versailles.PrettyPrinter {

    override def doPrettyPrint(term: Term) {
        term match {
        case BitFieldType(bw) =>
            prefixOp("bits ", bw, 10)
        case Application(MemberContextedType(p), t) =>
            doPrettyPrint(t)
            result.append(p.map{ _.name }.mkString("@", ".", ""))
        case BitRecordType(b) =>
            paren("record {\n", indentText*indent + "}", 0, {
                indent += 1
                doPrettyPrintStatement(b)
                indent -= 1
            })
        case BitRegisterType(bw, b) =>
            paren("register ", indentText*indent + "}", 0, {
                indent += 1
                doPrettyPrint(bw)
                append(" {\n")
                doPrettyPrintStatement(b)
                indent -= 1
            })
        case BitUnionType(b) =>
            paren("union {\n", indentText*indent + "}", 0, {
                indent += 1
                doPrettyPrintStatement(b)
                indent -= 1
            })
        case BitArrayType(et, u) =>
            paren("array of ", "", 0, {
                doPrettyPrint(et)
                append(" until ")
                doPrettyPrint(u)
            })
        case WrittenType(t, w) =>
            binOpRight(" written ", t, w, 10)
        case ConvertedType(t, c) =>
            binOpRight(" converted ", t, c, 10)
        case WhereType(t, w) =>
            binOpRight(" where ", t, w, 10)
        case InterpretedBitType(t, i) =>
            binOpRight(" is ", t, i, 5)
        case EnumInterpretation(b) =>
            paren("one of {\n", indentText*indent + "}", 0, {
                indent += 1
                doPrettyPrintStatement(b)
                indent -= 1
            })
        case FixedInterpretation(fv) =>
            result.append("fixed ")
            doPrettyPrint(fv)
        case UnitInterpretation(u) =>
            result.append("in ")
            result.append(StringEscapeUtils.escapeJava(u))
        case ContainingInterpretation(ct) =>
            result.append("containing ")
            doPrettyPrint(ct)
        case term =>
            super.doPrettyPrint(term)
        }
    }

    override def doPrettyPrintStatement(it: Statement) =
        it match {
        case it@BitRecordComponent(n, t) =>
            result.append(indentText * indent)
            result.append("component ")
            doPrettyPrintName(n)
            result.append(" = ")
            doPrettyPrint(t)
            result.append(";\n")
        case it@BitRegisterComponent(n, p, t) =>
            result.append(indentText * indent)
            result.append("component ")
            doPrettyPrint(p)
            result.append(" ")
            doPrettyPrintName(n)
            result.append(" = ")
            doPrettyPrint(t)
            result.append(";\n")
        case it@BitUnionVariant(n, t) =>
            result.append(indentText * indent)
            result.append("variant ")
            doPrettyPrintName(n)
            result.append(" = ")
            doPrettyPrint(t)
            result.append(";\n")
        case it@EnumValue(n, v) =>
            result.append(indentText * indent)
            result.append("value ")
            doPrettyPrintName(n)
            result.append(" = ")
            doPrettyPrint(v)
            result.append(";\n")
        case _ => super.doPrettyPrintStatement(it)
        }

}
