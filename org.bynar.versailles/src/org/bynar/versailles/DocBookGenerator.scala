package org.bynar.versailles

import scala.xml.Elem
import scala.xml.Node
import scala.xml.XML

abstract class DocBookGenerator(val root: Statement) {
    import DocBookGenerator._
    val pp: PrettyPrinter
    val simp: Simplifier

    def annotatePathInfo(item: Term, path: Seq[Symbol] = Seq()) {
        item match {
        case Def(id, t) =>
            id.putAnnotation(pathInfo, path)
            annotatePathInfo(t, path :+ VariableIdentity.getName(id))
        case _ =>
            for ((_, child) <- item.children)
                annotatePathInfo(child, path)
        }
    }
    annotatePathInfo(root)

    def generate(d: Def): Elem = {
        val title = d.identity.annotation(titleInfo).getOrElse(niceTitle(VariableIdentity.getName(d.identity)))
        val descr = d.identity.annotation(descriptionInfo).map{ d =>
            XML.loadString("<root>" + pp.prettyPrint(simp.simplify(
                    Block(root, Application(d, StringLiteral("it"))),
                    false, defaultContext
            )._1) + "</root>").child }.getOrElse(Seq())
        <article xmlns="http://docbook.org/ns/docbook" xmlns:mml="http://www.w3.org/1998/Math/MathML" version="5.0">
			<info>
				<title>{ title }</title>
			</info>
			{ descr }
			{ d.value match {
			    case m: Module => generateMainDefinitions(m.body)
		    }}
	 	</article>
    }
    
    def generateMainDefinitions(item: Statement): Seq[Node] =
        item match {
        case Sequence(ss@_*) =>
            ss.flatMap{
                generateMainDefinitions(_)
            }
        case _ => Seq()
        }

    def niceTitle(name: Symbol): String = {
        val result = name.name.replace("_", " ").trim()
        result.substring(0, 1).toUpperCase + result.substring(1)
    }
}

object DocBookGenerator {
    val titleInfo = new AnnotationKey[String]
    val descriptionInfo = new AnnotationKey[Expression]
    val pathInfo = new AnnotationKey[Seq[Symbol]]
}
