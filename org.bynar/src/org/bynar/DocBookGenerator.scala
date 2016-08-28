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
import org.bynar.versailles.AnnotationKey
import org.bynar.versailles.Term
import org.bynar.versailles.Lambda
import org.bynar.versailles.Undefined
import org.bynar.versailles.OrElse
import org.bynar.versailles.Member
import org.bynar.versailles.BooleanLiteral
import org.bynar.versailles.InfiniteIndex
import org.bynar.versailles.IndexMultiply
import org.bynar.versailles.RangeIndex
import scala.xml.Text

class DocBookGenerator(root: Statement) extends org.bynar.versailles.DocBookGenerator(root) {
    import org.bynar.versailles.DocBookGenerator._

    val simp = new Simplifier()
    val pp = new TextPrettyPrinter()

    override def annotatePathInfo(item: Term, path: Seq[Symbol] = Seq()) {
        item match {
        case item: BitRecordComponent =>
            item.putAnnotation(pathInfo, path)
            annotatePathInfo(item.`type`, path :+ item.name)
        case item: BitUnionVariant =>
            item.putAnnotation(pathInfo, path)
            annotatePathInfo(item.`type`, path :+ item.name)
        case item: EnumValue =>
            item.putAnnotation(pathInfo, path)
            annotatePathInfo(item.value, path :+ item.name)
        case _ => super.annotatePathInfo(item, path)
        }
    }

    def term2Xml(term: Term): Seq[Node] =
        XML.loadString("<root>" + pp.prettyPrint(term) + "</root>").child

    override def generateMainDefinitions(item: Statement): Seq[Node] =
        item match {
        case d@Def(id, t: BitTypeExpression) =>
            generateMainTypeDefinition(d)
        case _ => super.generateMainDefinitions(item)
        }

    def generateMainTypeDefinition(d: Def): Seq[Node] = {
        val path = d.identity.annotation(pathInfo).get :+ VariableIdentity.getName(d.identity)
        val title = d.annotation(titleInfo).getOrElse(niceTitle(path(path.size - 1)))
        val descr = d.annotation(descriptionInfo).map{ d =>
            XML.loadString("<root>" + pp.prettyPrint(simp.simplify(
                    Block(root, Application(d, StringLiteral("it"))),
                    false, defaultContext
            )._1) + "</root>").child }.getOrElse(Seq())
        Seq(<section id={ path.map{ _.name }.mkString(".") }>
			<title>{ title }</title>
			{ descr }
			{ generateMainTypeDescription(d.value.asInstanceOf[BitTypeExpression]) }
		</section>)
    }
    
    def generateTypeDescription(t: Expression): Seq[Node] =
        t match {
        case BitFieldType(_) => Seq()
        case BitRecordType(_) => Seq()
        case BitUnionType(_) => Seq()
        case WhereType(t, w) =>
            generateTypeDescription(t) :+
            <para>Valid if { term2Xml(simp.simplify(Block(root, Application(w, Variable(VariableIdentity.setName(new VariableIdentity, 'it), false))), true, defaultContext)._1) }.</para>
        case ConvertedType(t, c) =>
            generateTypeDescription(t) :+
            <para>Converted via { term2Xml(simp.simplify(Block(root, Application(c, Variable(VariableIdentity.setName(new VariableIdentity, 'it), false))), true, defaultContext)._1) }.</para>
        case WrittenType(t, w) =>
            generateTypeDescription(t) :+
            <para>Written as { term2Xml(simp.simplify(Block(root, Application(w, Variable(VariableIdentity.setName(new VariableIdentity, 'it), false))), true, defaultContext)._1) }.</para>
        case InterpretedBitType(t, i: EnumInterpretation) =>
            generateTypeDescription(t) :+
            <para>May contain one of the following values:
				<variablelist>
				{ i.foldValues(Seq[Node]()){
				    case (v, ns) =>
				        ns :+
				        <varlistentry><term>{ v.name.name }</term><listitem>{ term2Xml(v.value) }</listitem></varlistentry>
				    }
				}
				</variablelist>
			</para>
        case Variable(id, _) =>
            <para>A { term2Xml(t) }.</para>
        case Application(MemberContextedType(_), t) =>
            generateTypeDescription(t)
        }

    def generateMainTypeDescription(t: Expression): Seq[Node] =
        t match {
        case BitRecordType(b) =>
            val it = VariableIdentity.setName(new VariableIdentity, 'it)
            val (entries, bw) = generateTableEntries(
			        b,
                    Lambda(Irreversible(), Variable(VariableIdentity.setName(new VariableIdentity, '_), true), NumberLiteral(0)),
                    (a: Expression, b: Expression) => {
                        val x = VariableIdentity.setName(new VariableIdentity, '_)
                        simp.simplify(Lambda(Irreversible(), Variable(x, true),
                                Application(Application(Plus(), Application(a, Variable(x, false))), Application(b, Variable(x, false)))), true, Map())._1
                    },
                    (a: Expression) => {
                        term2Xml(simp.simplify(Block(root, Application(a, Variable(it, false))), false, defaultContext)._1)
                    },
			        Seq())
            if (entries.nonEmpty) 
    			<para>Bit width: { term2Xml(simp.simplify(Block(root, Application(bw, Variable(it, false))), true, defaultContext)._1) }</para>
                <table>
            		<thead><tr><td>Offset</td><td>Name</td><td>Description</td></tr></thead>
					<tbody>{ entries }</tbody>
				</table>
            else
                <para>Empty record.</para>                
        case WrittenType(t2, _) =>
            generateMainTypeDescription(t2) ++ generateTypeDescription(t)
        case ConvertedType(t2, _) =>
            generateMainTypeDescription(t2) ++ generateTypeDescription(t)
        case WhereType(t2, _) =>
            generateMainTypeDescription(t2) ++ generateTypeDescription(t)
        case InterpretedBitType(t2, _) =>
            generateMainTypeDescription(t2) ++ generateTypeDescription(t)
        case Variable(id, _) =>
            <para>Alias for { term2Xml(t) }.</para>
        case _ => Seq()
        }

    def generateTableEntries(statements: Statement,
                             offset: Expression,
                             offsetFold: (Expression, Expression) => Expression,
                             offsetText: Expression => Seq[Node],
                             tablePath: Seq[Symbol]): (Seq[Node], Expression) =
        statements match {
        case Sequence(ss@_*) =>
            ((Seq[Node](), offset) /: ss){
            case ((ss2, offset), s2) =>
                val (ss3, offset3) = generateTableEntries(s2, offset, offsetFold, offsetText, tablePath)
                (ss2 ++ ss3, offset3)
            }
        case BitRecordComponent(n, t) =>
            val head = <tr>
				<td>{ offsetText(offset) }</td>
				<td>{ (tablePath :+ n).map{ _.name }.mkString(".") }</td>
				<td>{ term2Xml(statements.annotation(descriptionInfo).getOrElse(StringLiteral(""))) ++
				      generateTypeDescription(t) }</td>
			</tr>
	        val (rows, ofs) = generateTableEntries(t, offset, offsetFold, offsetText, tablePath :+ n)
	        (head +: rows, ofs)
        case BitUnionVariant(n, t) =>
            val head = <tr>
				<td>{ offsetText(offset) }</td>
				<td>{ (tablePath :+ n).map{ _.name }.mkString(".") }</td>
				<td>{ term2Xml(statements.annotation(descriptionInfo).getOrElse(StringLiteral(""))) ++
				      generateTypeDescription(t) }</td>
			</tr>
	        val (rows, ofs) = generateTableEntries(t, offset, offsetFold, offsetText, tablePath :+ n)
	        (head +: rows, ofs)
        }

    def generateTableEntries(`type`: Expression,
                             offset: Expression,
                             offsetFold: (Expression, Expression) => Expression,
                             offsetText: Expression => Seq[Node],
                             tablePath: Seq[Symbol]): (Seq[Node], Expression) =
        `type` match {
        case BitFieldType(bw) =>
            (Seq(), offsetFold(offset, Lambda(Irreversible(), Variable(VariableIdentity.setName(new VariableIdentity, '_), true), bw)))
        case BitRecordType(b) =>
            val it = VariableIdentity.setName(new VariableIdentity, 'it)
            val (xml, bw) = generateTableEntries(
                    b,
                    Lambda(Irreversible(), Variable(VariableIdentity.setName(new VariableIdentity, '_), true), NumberLiteral(0)),
                    (a: Expression, b: Expression) => {
                        val x = VariableIdentity.setName(new VariableIdentity, '_)
                        simp.simplify(Lambda(Irreversible(), Variable(x, true),
                                Application(Application(Plus(), Application(a, Variable(x, false))), Application(b, Variable(x, false)))), false, Map())._1
                    },
                    (a: Expression) => {
                        term2Xml(simp.simplify(Block(root, Application(Application(Plus(), offset), Application(a, Variable(it, false)))), false, defaultContext)._1)
                    },
                    tablePath)
            (xml, offsetFold(offset, bw))
        case BitUnionType(b) =>
            val (xml, bw) = generateTableEntries(
                    b,
                    Lambda(Irreversible(), Undefined(), Undefined()),
                    (a: Expression, b: Expression) => Application(Application(OrElse(), a), b),
                    (ofs: Expression) => offsetText(offset),
                    tablePath)
            (xml, offsetFold(offset, bw))
        case WrittenType(t, _) =>
            generateTableEntries(t, offset, offsetFold, offsetText, tablePath)
        case ConvertedType(t, _) =>
            generateTableEntries(t, offset, offsetFold, offsetText, tablePath)
        case WhereType(t, w) =>
            generateTableEntries(t, offset,
                    (a: Expression, b: Expression) => {
                        val x = VariableIdentity.setName(new VariableIdentity, '_)
                        offsetFold(a, Lambda(Irreversible(), Variable(x, true),
                                Block(Let(BooleanLiteral(true), Application(w, Variable(x, false))), Application(b, Variable(x, false)))))
                    },
                    offsetText, tablePath)
        case InterpretedBitType(t, _) =>
            generateTableEntries(t, offset, offsetFold, offsetText, tablePath)
        case Application(MemberContextedType(p), t) =>
            val it = VariableIdentity.setName(new VariableIdentity, 'it)
            (Seq(), offsetFold(offset, Lambda(Irreversible(), Variable(it, true),
                    Application(Application(BitWidth(), t),
                            ((Variable(it, false): Expression) /: p){ case (b, n) => Application(Member(n), b) }))))
        case _ =>
            (Seq(), offset)
        }
}