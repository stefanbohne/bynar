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
import org.bynar.versailles.OrElseValue
import org.bynar.versailles.RangeIndex
import org.bynar.versailles.Module

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
        case d@Def(id, Module(b)) =>
            val path = d.identity.annotation(pathInfo).get :+ VariableIdentity.getName(d.identity)
            val title = d.identity.annotation(titleInfo).getOrElse(niceTitle(path(path.size - 1)))
            val descr = d.identity.annotation(descriptionInfo).map{ d =>
                XML.loadString("<root>" + pp.prettyPrint(simp.simplify(
                        Block(root, Application(d, StringLiteral("it"))),
                        false, defaultContext
                )._1) + "</root>").child }.getOrElse(Seq())
            Seq(<section id={ path.map{ _.name }.mkString(".") }>
				<title>{ title }</title>
			    { descr }
			    { generateMainDefinitions(b) }
				</section>)
        case d@Def(id, v) =>
            val path = d.identity.annotation(pathInfo).get :+ VariableIdentity.getName(d.identity)
            val title = d.identity.annotation(titleInfo).getOrElse(niceTitle(path(path.size - 1)))
            val descr = d.identity.annotation(descriptionInfo).map{ d =>
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
        val title = d.identity.annotation(titleInfo).getOrElse(niceTitle(path(path.size - 1)))
        val descr = d.identity.annotation(descriptionInfo).map{ d =>
            XML.loadString("<root>" + pp.prettyPrint(simp.simplify(
                    Block(root, Application(d, StringLiteral("it"))),
                    false, defaultContext
            )._1) + "</root>").child }.getOrElse(Seq())
        Seq(<section id={ path.map{ _.name }.mkString(".") }>
			<title>{ title }</title>
			{ descr }
			{ generateMainTypeDescription(d.value.asInstanceOf[BitTypeExpression], title) }
		</section>)
    }

    def generateTypeDescription(t: Expression): Seq[Node] = {
        def isName(t: Expression): Boolean =
            t match {
            case Variable(_, false) => true
            case Application(Member(_), t2) => isName(t2)
            case _ => false
            }
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
        case BitArrayType(ct, u) =>
            <para>Elements are parsed until {
                val it = VariableIdentity.setName(new VariableIdentity, 'it)
                term2Xml(simp.simplify(Block(root, Application(u, Variable(it, false))), true, defaultContext)._1)
            }.</para>
        case WhereType(t, w) =>
            generateTypeDescription(t) ++ whereTypeDescription(w)
        case ConvertedType(t, c) =>
            generateTypeDescription(t) ++ convertedTypeDescription(c)
        case WrittenType(t, w) =>
            generateTypeDescription(t) ++ writtenTypeDescription(w)            
        case InterpretedBitType(t, i) =>
            generateTypeDescription(t) ++ interpretedTypeDescription(i)
        case _ if isName(t) =>
            <para>A { term2Xml(t) }.</para>
        case Application(MemberContextedType(_), t) =>
            generateTypeDescription(t)
        }
    }

    def generateMainTypeDescription(t: Expression, title: String): Seq[Node] =
        t match {
        case BitRecordType(b) =>
            val it = VariableIdentity.setName(new VariableIdentity, 'it)
            val (entries, bw) = generateTableEntries(t, Lambda(Irreversible(), Undefined(), Application(InfiniteIndex(), NumberLiteral(0))), Seq())
            if (entries.nonEmpty)
    			<para>Bit width: { term2Xml(simp.simplify(Block(root, Application(bw, Variable(it, false))), true, defaultContext)._1) }</para>
                <table pgwide="1">
					<title>Structure of a { title }</title>
					<tgroup cols="3" colsep="1" rowsep="1">
					<colspec colwidth="1.5*"/>
					<colspec colwidth="2*"/>
					<colspec colwidth="3*"/>
            		<thead><row><entry>Offset</entry><entry>Name</entry><entry>Description</entry></row></thead>
					<tbody>{ entries }</tbody>
					</tgroup>
				</table>
            else
                <para>Empty record.</para>
        case BitRegisterType(bw, b) =>
            val it = VariableIdentity.setName(new VariableIdentity, 'it)
            val (entries, _) = generateTableEntries(t, Lambda(Irreversible(), Undefined(), rangeIndexInclusive(NumberLiteral(0), bw - 1)), Seq())
            if (entries.nonEmpty)
    			<para>Bit width: { term2Xml(simp.simplify(Block(root, bw), true, defaultContext)._1) }</para>
                <table pgwide="1">
					<title>Structure of a { title }</title>
					<tgroup cols="3" colsep="1" rowsep="1">
					<colspec colwidth="1.5*"/>
					<colspec colwidth="2*"/>
					<colspec colwidth="3*"/>
            		<thead><row><entry>Offset</entry><entry>Name</entry><entry>Description</entry></row></thead>
					<tbody>{ entries }</tbody>
					</tgroup>
				</table>
            else
                <para>Empty register.</para>
        case BitFieldType(bw) =>
            <para>A bit field of { 
                val it = VariableIdentity.setName(new VariableIdentity, 'it)
                term2Xml(simp.simplify(Block(root, Application(bw, Variable(it, false))), true, defaultContext)._1) 
                } bits.</para>
        case WrittenType(t2, w) =>
            generateMainTypeDescription(t2, title) ++ writtenTypeDescription(w)
        case ConvertedType(t2, c) =>
            generateMainTypeDescription(t2, title) ++ convertedTypeDescription(c)
        case WhereType(t2, w) =>
            generateMainTypeDescription(t2, title) ++ whereTypeDescription(w)
        case InterpretedBitType(t2, i) =>
            generateMainTypeDescription(t2, title) ++ interpretedTypeDescription(i)
        case Variable(id, _) =>
            <para>Alias for { term2Xml(t) }.</para>
        case _ => Seq()
        }

    def bitRange(bitWidth: Expression): Expression = {
        val x = VariableIdentity.setName(new VariableIdentity, 'it)
        Lambda(Irreversible(), Variable(x, true),
            rangeIndexInclusive(0, bitWidth(Variable(x, false)) - 1))
    }

    def bitPermutationText(bitPermutation: Expression, bitPositions: Expression): Seq[Node] = {
        def removeIfs(expr: Expression): Seq[Expression] =
            expr match {
            case OrElseValue(a, b) =>
                removeIfs(a) ++ removeIfs(b)
            case Block(Let(BooleanLiteral(true), _), x) =>
                removeIfs(x)
            case expr => Seq(expr)
            }
        term2Xml(removeIfs(simp.simplify(Block(root, {
            val it = VariableIdentity.setName(new VariableIdentity, 'it)
            Application(Application(IndexComposition(), Application(bitPositions, Variable(it, false))),
                    Application(bitPermutation, Variable(it, false)))
        }), true, defaultContext)._1).distinct.reduce{ OrElseValue(_, _) })
    }

    def generateTableEntries(`type`: Expression,
                             bitPermutation: Expression,
                             tablePath: Seq[Symbol]): (Seq[Node], Expression) =
        `type` match {
        case BitFieldType(bw) =>
            (Seq(), bw)
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
                    val d = { val d = term2Xml(c.annotation(descriptionInfo).getOrElse(StringLiteral("")))
    					              c.annotation(titleInfo).map{ t => <formalpara><title>{ t }</title>{ d }</formalpara> }.getOrElse(d)
    					            } ++ generateTypeDescription(c.`type`)
                    val row3 = if (d.nonEmpty || c.annotation(descriptionInfo).nonEmpty || rows2.isEmpty)
                            <row>
					     		<entry>{ bitPermutationText(bp, bitRange(bw)) }</entry>
						  	    <entry>{ (tablePath :+ c.name).map{ _.name }.mkString(".​") }</entry>
					     		<entry>{ d }</entry>
				     	    </row>
				         else Seq()
                    (rows ++ row3 ++ rows2, {
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
                    val row3 = if (c.annotation(descriptionInfo).nonEmpty || c.annotation(descriptionInfo).nonEmpty || rows2.isEmpty)
                             <row>
					     	 	 <entry>{ bitPermutationText(bp, Lambda(Irreversible(), Undefined(), c.position)) }</entry>
						 	     <entry>{ (tablePath :+ c.name).map{ _.name }.mkString(".​") }</entry>
					             <entry>{ val d = term2Xml(c.annotation(descriptionInfo).getOrElse(StringLiteral("")))
					                   c.annotation(titleInfo).map{ t => <formalpara><title>{ t }</title>{ d }</formalpara> }.getOrElse(d)
					                 }{ generateTypeDescription(c.`type`) }</entry>
				    		</row> 
				         else 
				             Seq()
                    rows ++ row3 ++ rows2
            }, Lambda(Irreversible(), Undefined(), t.bitWidth))
        case t: BitUnionType =>
            t.foldVariants(Seq[Node](), (Lambda(Irreversible(), Undefined(), Undefined()): Expression)){
                case (v, (rows, bw)) =>
                    val (rows2, bw2) = generateTableEntries(v.`type`, bitPermutation, tablePath :+ v.name)
                    val row3 = if (v.annotation(descriptionInfo).nonEmpty || v.annotation(descriptionInfo).nonEmpty || rows2.isEmpty)
                            <row>
					     	    <entry>{ bitPermutationText(bitPermutation, bitRange(bw2)) }</entry>
						 	    <entry>{ (tablePath :+ v.name).map{ _.name }.mkString(".​") }</entry>
					     		<entry>{ val d = term2Xml(v.annotation(descriptionInfo).getOrElse(StringLiteral("")))
					                  v.annotation(titleInfo).map{ t => <formalpara><title>{ t }</title>{ d }</formalpara> }.getOrElse(d)
					                }{ generateTypeDescription(v.`type`) }</entry>
				     		</row>
		                else
		                    Seq()
                    (rows ++ row3 ++ rows2, Application(Application(OrElse(), bw), bw2))
            }
        case BitArrayType(et, u) =>
            (Seq(), Lambda(Irreversible(), Undefined(), Undefined()))
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
    
    def whereTypeDescription(where: Expression): Seq[Node] =
        <para>Valid if { term2Xml(simp.simplify(Block(root, Application(where, Variable(VariableIdentity.setName(new VariableIdentity, 'it), false))), true, defaultContext)._1) }.</para>
    def convertedTypeDescription(converted: Expression): Seq[Node] = 
            <para>Converted via { term2Xml(simp.simplify(Block(root, Application(converted, Variable(VariableIdentity.setName(new VariableIdentity, 'it), false))), true, defaultContext)._1) }.</para>
    def writtenTypeDescription(written: Expression): Seq[Node] =
        <para>Written as { term2Xml(simp.simplify(Block(root, Application(written, Variable(VariableIdentity.setName(new VariableIdentity, 'x), false))), true, defaultContext)._1) }.</para>
    def interpretedTypeDescription(interpretation: BitTypeInterpretation): Seq[Node] =
        interpretation match {
        case i: EnumInterpretation =>
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
        case i: UnitInterpretation =>
            <para>Values are in {i.unit}.</para>
        case i: FixedInterpretation =>
            <para>Must contain the value {
                val it = VariableIdentity.setName(new VariableIdentity, 'it)
                term2Xml(simp.simplify(Block(root, Application(i.fixedValue, Variable(it, false))), true, defaultContext)._1)
            }.</para>
        case i: ContainingInterpretation =>
            <para>Values are of the following structure.
				{ generateTypeDescription(i.containedType) }
			</para>
        }
}