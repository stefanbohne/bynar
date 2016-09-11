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
import org.bynar.versailles.Block
import org.bynar.versailles.Let
import org.bynar.versailles.BooleanLiteral
import org.bynar.versailles.OrElseValue
import org.bynar.versailles.Equals
import org.bynar.versailles.NotEquals
import org.bynar.versailles.LessOrEquals
import org.bynar.versailles.GreaterOrEquals
import org.bynar.versailles.And
import org.bynar.versailles.Or

class TextPrettyPrinter extends PrettyPrinter {

    override def append(text: Any) {
        result.append(Text(text.toString).toString.
                replace(" * ", " ⋅ ").
                replace(" == ", " = ").
                replace(" != ", " &#x2260; ").
                replace(" &lt;= ", " &#x2264; ").
                replace(" &gt;= ", " &#x2265; ").
                replace(" &amp;&amp; ", " and ").
                replace(" || ", " or ").
                replace(" -&gt; ", " &#x2192; ").
                replace(" &lt;- ", " &#x2190; ").
                replace(" &lt;-&gt; ", " &#x2194; ").
                replace(" &gt;-&gt; ", " &#x21A3; ").
                replace(" &lt;-&lt; ", " &#x21A2; ").
                replace(" &gt;-&lt; ", " &#x21AD; ").
                replace(" &lt;&gt;-&lt;&gt; ", " &#x21C4; "))
    }
    def appendXml(xml: Node*) {
        for (node <- xml)
            result.append(node.toString)
    }
    override def doPrettyPrint(term: Term) = {
        def isIfThenElse(term: Term): Boolean =
            term match {
            case Block(Let(BooleanLiteral(true), _), _) => true
            case OrElseValue(_, _) => true
            case _ => false
            }

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
                result.append(path.map{ _.name }.mkString("."))
                result.append("\">")
            }
            result.append("<emphasis>")
            id.annotation(versailles.DocBookGenerator.titleInfo) match {
                case Some(t) => result.append(t)
                case None => result.append(VariableIdentity.getName(id).name)
            }            
            result.append("</emphasis>")
            if (id.annotation(versailles.DocBookGenerator.pathInfo).nonEmpty) {
                result.append("</link>")
            }
        case Variable(id, true) =>
            result.append("<emphasis>?")
            result.append(VariableIdentity.getName(id).name)
            result.append("</emphasis>")
        case term if isIfThenElse(term) =>
            def printIfThenElse(term: Term, last: Boolean) {
                term match {
                    case Block(Let(BooleanLiteral(true), c), v) =>
                        result.append("<member>")
                        paren(5, {
                            doPrettyPrint(v)
                            append(" if ")
                            doPrettyPrint(c)
                        })
                        result.append("</member>")
                    case OrElseValue(f, s) =>
                        paren(5, {
                            printIfThenElse(f, false)
                            printIfThenElse(s, last)
                        })
                    case term =>
                        doPrettyPrint(term)
                }
            }
            result.append("<simplelist>")
            printIfThenElse(term, true)
            result.append("</simplelist>")
        case _ =>
            super.doPrettyPrint(term)
        }
    }

}