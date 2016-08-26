package org.bynar

import org.bynar.versailles.StringLiteral
import org.bynar.versailles.Term
import org.bynar.versailles.Variable
import org.bynar.versailles.VariableIdentity

class TextPrettyPrinter extends PrettyPrinter {

    override def doPrettyPrint(term: Term) =
        term match {
        case StringLiteral(s) =>
            result.append(s)
        case Variable(id, false) =>
            if (id.annotation(versailles.DocBookGenerator.pathInfo).nonEmpty) {
                val path = id.annotation(versailles.DocBookGenerator.pathInfo).get :+ VariableIdentity.getName(id)
                result.append("<link linkend=\"")
                result.append(path.mkString("."))
                result.append("\"/>")
            }
            result.append("<emphasis>")
            super.doPrettyPrint(term)
            result.append("</emphasis>")
            if (id.annotation(versailles.DocBookGenerator.pathInfo).nonEmpty) {
                result.append("</link>")
            }
        case _ =>
            super.doPrettyPrint(term)
        }

}