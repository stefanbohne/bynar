package org.bynar

import org.bynar.versailles.StringLiteral
import org.bynar.versailles.Term
import org.bynar.versailles.Variable
import org.bynar.versailles.VariableIdentity
import scala.xml.XML
import scala.xml.Text
import scala.xml.Node
import org.bynar.versailles.Concat
import org.bynar.versailles.Application

class TextPrettyPrinter extends PrettyPrinter {

    override def append(text: Any) {
        result.append(Text(text.toString).toString)
    }
    def appendXml(xml: Node*) {
        for (node <- xml)
            result.append(node.toString)
    }
    override def doPrettyPrint(term: Term) =
        term match {
        case StringLiteral(s) =>
            result.append(s)
        case Application(Application(Concat(), l), r) =>
            doPrettyPrint(l)
            doPrettyPrint(r)
        case Variable(id, false) =>
            if (id.annotation(versailles.DocBookGenerator.pathInfo).nonEmpty) {
                val path = id.annotation(versailles.DocBookGenerator.pathInfo).get :+ VariableIdentity.getName(id)
                result.append("<link linkend=\"")
                result.append(path.mkString("."))
                result.append("\">")
            }
            result.append("<emphasis>")
            result.append(VariableIdentity.getName(id).name)
            result.append("</emphasis>")
            if (id.annotation(versailles.DocBookGenerator.pathInfo).nonEmpty) {
                result.append("</link>")
            }
        case Variable(id, true) =>
            result.append("<emphasis>?")
            result.append(VariableIdentity.getName(id).name)
            result.append("</emphasis>")
        case _ =>
            super.doPrettyPrint(term)
        }

}