package org.bynar.versailles

import scala.xml.Elem
import scala.xml.Node

class DocBookGenerator(root: Statement) {
    import DocBookGenerator._

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

    def generate(): Elem =
        <article>
			<info>
				<title>Data Type Definition</title>
			</info>
			{generateMainDefinitions(root)}
	 	</article>

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
