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
import org.bynar.versailles.IndexComposition
import scala.xml.Text

class DocBookGenerator(root: Statement) extends org.bynar.versailles.DocBookGenerator(root) {
    import org.bynar.versailles.DocBookGenerator._
    import org.bynar.versailles.TermImplicits._
    import TermImplicits._

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
        case d@Def(id, v) =>
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
			    <para>{ term2Xml(v) }</para>
				</section>)
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
        case t: BitRecordType =>
            if (t.components.isEmpty)
                <para>An empty record.</para>
            else
                Seq()
        case t: BitRegisterType =>
            if (t.components.isEmpty)
                <para>An empty register.</para>
            else
                Seq()
        case t: BitUnionType =>
            if (t.variants.isEmpty)
                <para>An empty union.</para>
            else
                Seq()
        case WhereType(t, w) =>
            generateTypeDescription(t) :+
            <para>Valid if { term2Xml(simp.simplify(Block(root, Application(w, Variable(VariableIdentity.setName(new VariableIdentity, 'it), false))), true, defaultContext)._1) }.</para>
        case ConvertedType(t, c) =>
            generateTypeDescription(t) :+
            <para>Converted via { term2Xml(simp.simplify(Block(root, Application(c, Variable(VariableIdentity.setName(new VariableIdentity, 'it), false))), true, defaultContext)._1) }.</para>
        case WrittenType(t, w) =>
            generateTypeDescription(t) :+
            <para>Written as { term2Xml(simp.simplify(Block(root, Application(w, Variable(VariableIdentity.setName(new VariableIdentity, 'x), false))), true, defaultContext)._1) }.</para>
        case InterpretedBitType(t, i: EnumInterpretation) =>
            generateTypeDescription(t) :+
            <para>May contain one of the following values:
				<variablelist>
				{ i.foldValues(Seq[Node]()){
				    case (v, ns) =>
				        ns :+
				        <varlistentry><term>{ v.name.name } = { term2Xml(v.value) }</term>
                        <listitem>{ val d = term2Xml(v.annotation(descriptionInfo).getOrElse(StringLiteral("")))
					           v.annotation(titleInfo).map{ t => <formalpara><title>{ t }</title>{ d }</formalpara> }.getOrElse(d)
					         }</listitem></varlistentry>
				    }
				}
				</variablelist>
			</para>
        case InterpretedBitType(t, i: UnitInterpretation) =>
            generateTypeDescription(t) :+
            <para>Values are in {i.unit}.</para>
        case InterpretedBitType(t, i: FixedInterpretation) =>
            generateTypeDescription(t) :+
            <para>Must contain the following value: {
                val it = VariableIdentity.setName(new VariableIdentity, 'it)
                term2Xml(simp.simplify(Block(root, Application(i.fixedValue, Variable(it, false))), true, defaultContext)._1)
            }</para>
        case InterpretedBitType(t, i: ContainingInterpretation) =>
            generateTypeDescription(t) :+
            <para>Values are of the following structure.
				{ generateMainTypeDescription(i.containedType) }
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
            val (entries, bw) = generateTableEntries(t, Lambda(Irreversible(), Undefined(), Application(InfiniteIndex(), NumberLiteral(0))), Seq())
            if (entries.nonEmpty)
    			<para>Bit width: { term2Xml(simp.simplify(Block(root, Application(bw, Variable(it, false))), true, defaultContext)._1) }</para>
                <table>
            		<thead><tr><td>Offset</td><td>Name</td><td>Description</td></tr></thead>
					<tbody>{ entries }</tbody>
				</table>
            else
                <para>Empty record.</para>
        case BitRegisterType(bw, b) =>
            val it = VariableIdentity.setName(new VariableIdentity, 'it)
            val (entries, _) = generateTableEntries(t, Lambda(Irreversible(), Undefined(), rangeIndexInclusive(NumberLiteral(0), bw - 1)), Seq())
            if (entries.nonEmpty)
    			<para>Bit width: { term2Xml(simp.simplify(Block(root, bw), true, defaultContext)._1) }</para>
                <table>
            		<thead><tr><td>Offset</td><td>Name</td><td>Description</td></tr></thead>
					<tbody>{ entries }</tbody>
				</table>
            else
                <para>Empty register.</para>
        case BitFieldType(bw) =>
            <para>A bit field of { term2Xml(simp.simplify(Block(root, bw), true, defaultContext)._1) } bits.</para>
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

    def bitRange(bitWidth: Expression): Expression = {
        val x = VariableIdentity.setName(new VariableIdentity, 'it)
        Lambda(Irreversible(), Variable(x, true),
            rangeIndexInclusive(0, bitWidth(Variable(x, false)) - 1))
    }

    def bitPermutationText(bitPermutation: Expression, bitPositions: Expression): Seq[Node] =
        term2Xml(simp.simplify(Block(root, {
            val it = VariableIdentity.setName(new VariableIdentity, 'it)
            Application(Application(IndexComposition(), Application(bitPositions, Variable(it, false))),
                    Application(bitPermutation, Variable(it, false)))
        }), true, defaultContext)._1)

    def generateTableEntries(`type`: Expression,
                             bitPermutation: Expression,
                             tablePath: Seq[Symbol]): (Seq[Node], Expression) =
        `type` match {
        case BitFieldType(bw) =>
            (Seq(), Lambda(Irreversible(), Undefined(), bw))
        case t: BitRecordType =>
            t.foldComponents(Seq[Node](), Lambda(Irreversible(), Undefined(), NumberLiteral(0))){
                case (c, (rows, ofs)) =>
                    val bp = {
                        val x = VariableIdentity.setName(new VariableIdentity, '_)
                        Lambda(Irreversible(), Variable(x, true),
                            Application(Application(IndexComposition(), Application(InfiniteIndex(), Application(ofs, Variable(x, false)))),
                                Application(bitPermutation, Variable(x, false))))
                    }
                    val (rows2, bw) = generateTableEntries(c.`type`, bp, tablePath :+ c.name)
                    (rows ++ <tr>
					     <td>{ bitPermutationText(bp, bitRange(bw)) }</td>
						 <td>{ (tablePath :+ c.name).map{ _.name }.mkString(".") }</td>
					     <td>{ val d = term2Xml(c.annotation(descriptionInfo).getOrElse(StringLiteral("")))
					           c.annotation(titleInfo).map{ t => <formalpara><title>{ t }</title>{ d }</formalpara> }.getOrElse(d)
					         }{ generateTypeDescription(c.`type`) }</td>
				     </tr> ++ rows2, {
                        val x = VariableIdentity.setName(new VariableIdentity, '_)
                        Lambda(Irreversible(), Variable(x, true), Application(Application(Plus(), Application(ofs, Variable(x, false))), Application(bw, Variable(x, false))))
                    })
            }
        case t: BitRegisterType =>
            val bp = {
                val x = VariableIdentity.setName(new VariableIdentity, '_)
                Lambda(Irreversible(), Variable(x, true),
                    bitPermutation(Variable(x, false)) o rangeIndexInclusive(NumberLiteral(0), t.bitWidth - 1))
            }
            (t.foldComponents(Seq[Node]()){
                case (c, rows) =>
                    val (rows2, _) = generateTableEntries(c.`type`, {
                            val x = VariableIdentity.setName(new VariableIdentity, '_)
                            Lambda(Irreversible(), Variable(x, true),
                                Application(Application(IndexComposition(), c.position), Application(bp, Variable(x, false))))
                        }, tablePath :+ c.name)
                    rows ++ <tr>
					     <td>{ bitPermutationText(bp, Lambda(Irreversible(), Undefined(), c.position)) }</td>
						 <td>{ (tablePath :+ c.name).map{ _.name }.mkString(".") }</td>
					     <td>{ val d = term2Xml(c.annotation(descriptionInfo).getOrElse(StringLiteral("")))
					           c.annotation(titleInfo).map{ t => <formalpara><title>{ t }</title>{ d }</formalpara> }.getOrElse(d)
					         }{ generateTypeDescription(c.`type`) }</td>
				    </tr> ++ rows2
            }, Lambda(Irreversible(), Undefined(), t.bitWidth))
        case t: BitUnionType =>
            t.foldVariants(Seq[Node](), (Lambda(Irreversible(), Undefined(), Undefined()): Expression)){
                case (v, (rows, bw)) =>
                    val (rows2, bw2) = generateTableEntries(v.`type`, bitPermutation, tablePath :+ v.name)
                    (rows ++ <tr>
					     <td>{ bitPermutationText(bitPermutation, bitRange(bw2)) }</td>
						 <td>{ (tablePath :+ v.name).map{ _.name }.mkString(".") }</td>
					     <td>{ val d = term2Xml(v.annotation(descriptionInfo).getOrElse(StringLiteral("")))
					           v.annotation(titleInfo).map{ t => <formalpara><title>{ t }</title>{ d }</formalpara> }.getOrElse(d)
					         }{ generateTypeDescription(v.`type`) }</td>
				     </tr> ++ rows2, Application(Application(OrElse(), bw), bw2))
            }
        case WrittenType(t, _) =>
            generateTableEntries(t, bitPermutation, tablePath)
        case ConvertedType(t, _) =>
            generateTableEntries(t, bitPermutation, tablePath)
        case WhereType(t, w) =>
            val (rows, bw) = generateTableEntries(t, bitPermutation, tablePath)
            (rows, {
                val x = VariableIdentity.setName(new VariableIdentity, '_)
                Lambda(Irreversible(), Variable(x, true),
                        Block(Let(BooleanLiteral(true), Application(w, Variable(x, false))), Application(bw, Variable(x, false))))
            })
        case InterpretedBitType(t, _) =>
            generateTableEntries(t, bitPermutation, tablePath)
        case Application(MemberContextedType(p), t) =>
            val (rows, bw) = generateTableEntries(t, {
                val it = VariableIdentity.setName(new VariableIdentity, 'it)
                Lambda(Irreversible(), Variable(it, true), Application(bitPermutation,
                        ((Variable(it, false): Expression) /: p){ case (b, n) => Application(Member(n), b) }))
            }, tablePath)
            (rows, {
                val it = VariableIdentity.setName(new VariableIdentity, 'it)
                Lambda(Irreversible(), Variable(it, true), Application(bw,
                        ((Variable(it, false): Expression) /: p){ case (b, n) => Application(Member(n), b) }))
            })
        case _ =>
            (Seq(), Application(BitWidth(), `type`))
        }
}