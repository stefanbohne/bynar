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
import org.bynar.versailles.Sequence
import org.bynar.versailles.Expression
import org.bynar.versailles.NumberLiteral
import org.bynar.versailles.Plus

class DocBookGenerator(root: Statement) extends org.bynar.versailles.DocBookGenerator(root) {
    import org.bynar.versailles.DocBookGenerator._

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
            XML.loadString("<root>" + pp.prettyPrint(simp.simplify(
                    Block(root, Application(d, StringLiteral("it"))),
                    false, defaultContext
            )._1) + "</root>").child }.getOrElse(Seq())
        Seq(<section id={ path.map{ _.name }.mkString(".") }>
			<title>{ title }</title>
			{ descr }
			{ generateMainTypeDescription(d.value.asInstanceOf[BitTypeExpression], path) }
		</section>)
    }

    def generateMainTypeDescription(t: Expression, path: Seq[Symbol]): Seq[Node] =
        t match {
        case BitRecordType(Sequence()) =>
            <para>Empty record.</para>
        case BitRecordType(b) =>
            Seq(<table>
            	<thead><tr><td>Offset</td><td>Name</td><td>Description</td></tr></thead>
				<tbody>{ generateTableEntries(b, NumberLiteral(0), path, Seq())._1 }</tbody>
			</table>)
        case WrittenType(t, _) =>
            generateMainTypeDescription(t, path)
        case ConvertedType(t, _) =>
            generateMainTypeDescription(t, path)
        case WhereType(t, _) =>
            generateMainTypeDescription(t, path)
        case InterpretedBitType(t, _) =>
            generateMainTypeDescription(t, path)
        case _ => Seq()
        }

    def generateTableEntries(statements: Statement, offset: Expression, mainPath: Seq[Symbol], tablePath: Seq[Symbol]): (Seq[Node], Expression) =
        statements match {
        case Sequence(ss@_*) =>
            ((Seq[Node](), offset) /: ss){
            case ((ss2, offset), s2) =>
                val (ss3, offset3) = generateTableEntries(s2, offset, mainPath, tablePath)
                (ss2 ++ ss3, offset3)
            }
        case BitRecordComponent(n, t) =>
            val head = <tr>
				<td>{ pp.prettyPrint(offset) }</td>
				<td>{ (tablePath :+ n).map{ _.name }.mkString(".") }</td>
				<td></td>
			</tr>
	        val (rows, ofs) = generateTableEntries(t, offset, mainPath, tablePath :+ n)
	        (head +: rows, ofs)
        case BitUnionVariant(n, t) =>
            val head = <tr>
				<td>{ pp.prettyPrint(offset) }</td>
				<td>{ (tablePath :+ n).map{ _.name }.mkString(".") }</td>
				<td></td>
			</tr>
	        val (rows, ofs) = generateTableEntries(t, offset, mainPath, tablePath :+ n)
	        (head +: rows, ofs)
        }

    def generateTableEntries(`type`: Expression, offset: Expression, mainPath: Seq[Symbol], tablePath: Seq[Symbol]): (Seq[Node], Expression) =
        `type` match {
        case BitFieldType(bw) =>
            (Seq(), simp.simplify(Application(Application(Plus(), offset), bw), true, Map())._1)
        case BitRecordType(b) =>
            generateTableEntries(b, offset, mainPath, tablePath)
        case BitUnionType(b) =>
            generateTableEntries(b, offset, mainPath, tablePath)
        case WrittenType(t, _) =>
            generateTableEntries(t, offset, mainPath, tablePath)
        case ConvertedType(t, _) =>
            generateTableEntries(t, offset, mainPath, tablePath)
        case WhereType(t, _) =>
            generateTableEntries(t, offset, mainPath, tablePath)
        case InterpretedBitType(t, _) =>
            generateTableEntries(t, offset, mainPath, tablePath)
        case _ =>
            (Seq(), offset)
        }
}