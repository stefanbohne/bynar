package org.bynar.versailles

import scala.xml.Elem
import scala.xml.Node

class DocBookGenerator(root: Statement) {
    import DocBookGenerator._
  
    def generate(): Elem = 
        <article>
			<info>
				<title>Data Type Definition</title>
			</info>
			{generateMainDefinitions(root, Seq())}
	 	</article>
    
    def generateMainDefinitions(item: Statement, path: Seq[Symbol]): Seq[Node] =
        item match {
        case Sequence(ss@_*) =>
            ss.flatMap{ 
                generateMainDefinitions(_, path) 
            }
        case _ => Seq()
        }
    
    def niceTitle(name: Symbol): String = {
        val result = name.name.replace("_", " ").trim()
        result.substring(0, 1).toUpperCase + result.substring(1)
    }
}

object DocBookGenerator {
    val titleKey = new AnnotationKey[String]
    val descriptionKey = new AnnotationKey[Expression]
}