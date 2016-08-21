package org.bynar

import scala.xml.Elem
import org.bynar.versailles.Statement
import org.bynar.versailles.Variable
import org.bynar.versailles.Let
import org.bynar.versailles.VariableIdentity
import org.bynar.versailles.Def
import org.bynar.versailles.StringLiteral
import org.bynar.versailles.Application
import org.bynar.versailles.Block
import org.bynar.versailles.Irreversible
import scala.xml.Node
import scala.xml.XML

class DocGenerator(root: Statement) extends org.bynar.versailles.DocGenerator(root) {
    import org.bynar.versailles.DocGenerator._
    
    val va = new VariableAnalyzer();
    val simp = new Simplifier()
    val pp = new TextPrettyPrinter()
  
    override def generateMainDefinitions(item: Statement, path: Seq[Symbol]): Seq[Node] =
        item match {
        case d@Def(id, t: BitTypeExpression) =>
            generateMainTypeDefinition(d, path :+ VariableIdentity.getName(id))
        case _ => super.generateMainDefinitions(item, path)
        }
    
    def generateMainTypeDefinition(d: Def, path: Seq[Symbol]): Seq[Node] = {
        val title = d.annotation(titleKey).getOrElse(niceTitle(path(path.size - 1)))
        val descr = d.annotation(descriptionKey).map{ d =>
            XML.loadString("<root>" + pp.prettyPrint(simp.simplify(va.analyze(
                    Block(root, Application(d, StringLiteral("it"))), 
                    false, Irreversible(), va.Context(Map(defaultContext.toSeq.map{ case (id, _) => VariableIdentity.getName(id) -> va.ContextEntry(id, false) }:_*)))._1, 
                    false, defaultContext
            )._1) + "</root>").child }.getOrElse(Seq())
        Seq(<section>
			<title>{ title }</title>
			{ descr }
		</section>)
    }
}