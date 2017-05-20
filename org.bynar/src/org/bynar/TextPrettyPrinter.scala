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
import org.bynar.versailles.Member

class TextPrettyPrinter extends PrettyPrinter {

    override def append(text: Any) {
        result.append(Text(text.toString).toString.
                replace(" * ", " <inlineequation><mml:math><mml:mo>⋅</mml:mo></mml:math></inlineequation> ").
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
                replace(" &lt;&gt;-&lt;&gt; ", " &#x21C4; ").
                replace("(", "(​"))
    }
    def appendXml(xml: Node*) {
        for (node <- xml)
            result.append(node.toString)
    }
    def pathAsXml(path: Seq[Symbol]) =
        path.map{ _.name.replace("_", "_​") }.mkString(".​")
    override def doPrettyPrint(term: Term) = {
        def isIfThenElse(term: Term): Boolean =
            term match {
            case Block(Let(BooleanLiteral(true), _), _) => true
            case OrElseValue(_, _) => true
            case _ => false
            }
        def isName(term: Term): Boolean =
            term match {
            case Variable(_, false) => true
            case Application(Member(_), t) => isName(t)
            case Application(MemberContextedType(_), t) => isName(t)
            case _ => false
            }
        def getPath(term: Term): (Seq[Symbol], VariableIdentity) =
            term match {
            case Variable(id, false) => (Seq(), id)
            case Application(Member(n), t) => 
                val (p, id) = getPath(t)
                (p :+ n, id)
            case Application(MemberContextedType(_), t) => getPath(t)
            }

        term match {
        case StringLiteral(s) =>
            result.append(s)
        case Application(MemberContextedType(p), t) =>
            doPrettyPrint(t)
        case Application(Application(Concat(), l), r) =>
            doPrettyPrint(l)
            doPrettyPrint(r)
        case term if isName(term) =>
            val (p, id) = getPath(term)
            if (id.annotation(versailles.DocBookGenerator.pathInfo).nonEmpty) {
                // TODO: don't fake cross-references
                val path = id.annotation(versailles.DocBookGenerator.pathInfo).get :+ VariableIdentity.getName(id)
                result.append("<xref linkend=\"")
                result.append((path ++ p).map{ _.name }.mkString("."))
                result.append("\" xrefstyle=\"select:title\"/>")
            } else {
                result.append("<emphasis>")
                result.append(pathAsXml(VariableIdentity.getName(id) +: p))
                result.append("</emphasis>")
            }
        case Application(Member(n), b) =>
            paren(60, {
                precedence = 59
                doPrettyPrint(b)
                append(".​")
                appendXml(<emphasis>{ pathAsXml(Seq(n)) }</emphasis>)
            })
        case Variable(id, true) =>
            result.append("<emphasis>")
            result.append(pathAsXml(Seq(VariableIdentity.getName(id))))
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
                            if (!last) append(", or ")
                        })
                        result.append("</member>")
                    case OrElseValue(f, s) =>
                        paren(5, {
                            printIfThenElse(f, false)
                            printIfThenElse(s, last)
                        })
                    case term =>
                        result.append("<member>")
                        doPrettyPrint(term)
                        if (!last) append(", or ")
                        result.append("</member>")
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