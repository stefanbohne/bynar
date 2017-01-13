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
import org.bynar.versailles.Tuple
import org.bynar.versailles.Length
import org.bynar.versailles.SingletonIndex
import org.bynar.versailles.ZeroaryExpression

class DocBookGenerator(root1: Statement) extends {
    val simp = new Simplifier()
    val pp = new TextPrettyPrinter()
} with org.bynar.versailles.DocBookGenerator(simp.simplifyStatement(root1, defaultContext, true)._1) {
    import org.bynar.versailles.DocBookGenerator._
    import org.bynar.versailles.TermImplicits._
    import TermImplicits._

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
            def functionDescr(t: Expression): (Seq[Seq[Node]], Seq[Node]) = 
                t match {
                case t: Lambda =>
                    val (args, body) = functionDescr(t.body)
                    t.pattern match {
                    case Tuple(patterns@_*) =>
                        (patterns.map(p => <listitem>{ term2Xml(p) }</listitem>) ++ args, body)
                    case pattern => 
                        (<listitem>{ term2Xml(pattern) }</listitem> ++ args, body) 
                    }
                    
                case t: BitTypeExpression => (Seq(), generateMainTypeDescription(t, title))
                case t => (Seq(), <literallayout>{ term2Xml(simp.simplify(Block(root, t), true, defaultContext)._1) }</literallayout>)
                }
            val (args, body) = functionDescr(v)
            Seq(<section id={ path.map{ _.name }.mkString(".") }>
				<title>{ title }</title>
			    { descr }
			    { if (args.nonEmpty) <para>Parameters:<orderedlist>{ args }</orderedlist></para> }
			    { body }
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
        case t@Application(_, _) =>
            <para>A { term2Xml(t) }.</para>
        }
    }

    def generateMainTypeDescription(t: Expression, title: String): Seq[Node] = {
        def generateTableDescription(entries: Seq[(Expression, String, Seq[Node])], bw: Expression): Seq[Node] = {
            val it = VariableIdentity.setName(new VariableIdentity, 'it)
			<para>Bit width: { term2Xml(simp.simplify(Block(root, Application(bw, Variable(it, false))), true, defaultContext)._1) }</para> ++
            (if (entries.nonEmpty) {
                def isSimpleOrElseRange(e: Expression): Boolean =
                    e match {
                    case OrElseValue(a, b) => isSimpleOrElseRange(a) && isSimpleOrElseRange(b)
                    case Application(Application(RangeIndex(), _: ZeroaryExpression), _: ZeroaryExpression) => true
                    case Application(SingletonIndex(), _: ZeroaryExpression) => true
                    case Block(Let(BooleanLiteral(true), _), a) => isSimpleOrElseRange(a)
                    case _ => false
                }
                def firstIndex(e: Expression): Expression =
                    e match {
                    case e@OrElseValue(a, b) => e.copy(firstIndex(a), firstIndex(b))
                    case Application(Application(RangeIndex(), i), _) => i
                    case e: Block => e.copy(scope = firstIndex(e.scope))
                    }
                def indexSize(e: Expression): Expression =
                    simp.simplify(Length()(e), true, Map())._1
                def removeIfs1(expr: Expression): Seq[Expression] =
                    expr match {
                    case OrElseValue(a, b) =>
                        removeIfs1(a) ++ removeIfs1(b)
                    case Block(Let(BooleanLiteral(true), _), x) =>
                        removeIfs1(x)
                    case expr => Seq(expr)
                    }
                def removeIfs(e: Expression): Expression =
                    removeIfs1(e).distinct.reduce{ OrElseValue(_, _) }
                if (entries.forall{ case (bp, _, _) => isSimpleOrElseRange(bp) })
                <table pgwide="1">
					<title>Structure of a { title }</title>
					<tgroup cols="4" colsep="1" rowsep="1">
					<colspec colwidth="0.75*"/>
					<colspec colwidth="0.75*"/>
					<colspec colwidth="2*"/>
					<colspec colwidth="3*"/>
            		<thead><row><entry>Offset</entry><entry>Size</entry><entry>Name</entry><entry>Description</entry></row></thead>
					<tbody>{             
					    for ((bp, n, d) <- entries) 
                            yield <row>
								<entry>{ term2Xml(removeIfs(firstIndex(bp))) }</entry>
								<entry>{ term2Xml(removeIfs(indexSize(bp))) }</entry>
								<entry>{ n }</entry>
								<entry>{ d }</entry>
							</row> 
                     }</tbody>
					</tgroup>
				</table>
					else
                <table pgwide="1">
					<title>Structure of a { title }</title>
					<tgroup cols="3" colsep="1" rowsep="1">
					<colspec colwidth="1.5*"/>
					<colspec colwidth="2*"/>
					<colspec colwidth="3*"/>
            		<thead><row><entry>Bit Position</entry><entry>Name</entry><entry>Description</entry></row></thead>
					<tbody>{             
					    for ((bp, n, d) <- entries) 
                            yield <row>
								<entry>{ term2Xml(removeIfs(bp)) }</entry>
								<entry>{ n }</entry>
								<entry>{ d }</entry>
							</row> 
                     }</tbody>
					</tgroup>
				</table>
            } else
                <para>A { title } has no fields.</para>)
        }
        t match {
        case BitRecordType(b) =>
            val (entries, bw) = generateTableEntries(t, Lambda(Irreversible(), Undefined(), Application(InfiniteIndex(), NumberLiteral(0))), Seq())
            generateTableDescription(entries, bw)
        case BitRegisterType(bw, b) =>
            val (entries, _) = generateTableEntries(t, Lambda(Irreversible(), Undefined(), rangeIndex(NumberLiteral(0), bw)), Seq())
            generateTableDescription(entries, bw)
        case BitUnionType(b) =>
            val (entries, bw) = generateTableEntries(t, Lambda(Irreversible(), Undefined(), Application(InfiniteIndex(), NumberLiteral(0))), Seq())
            generateTableDescription(entries, bw)
        case BitFieldType(bw) =>
            <para>A { title } is a bit field of { 
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
            <para>A { title } is an alias for { term2Xml(t) }.</para>
        case _ => Seq()
        }
    }

    def bitRange(bitWidth: Expression): Expression = {
        val x = VariableIdentity.setName(new VariableIdentity, 'it)
        Lambda(Irreversible(), Variable(x, true),
            rangeIndex(0, bitWidth(Variable(x, false))))
    }

    def bitPermutationText(bitPermutation: Expression, bitPositions: Expression): Expression = {
        simp.simplify(Block(root, {
            val it = VariableIdentity.setName(new VariableIdentity, 'it)
            Application(Application(IndexComposition(), Application(bitPositions, Variable(it, false))),
                    Application(bitPermutation, Variable(it, false)))
        }), true, defaultContext)._1
    }

    def generateTableEntries(`type`: Expression,
                             bitPermutation: Expression,
                             tablePath: Seq[Symbol]): (Seq[(Expression, String, Seq[Node])], Expression) =
        `type` match {
        case BitFieldType(bw) =>
            (Seq(), bw)
        case t: BitRecordType =>
            t.foldComponents(Seq[(Expression, String, Seq[Node])](), Lambda(Irreversible(), Undefined(), NumberLiteral(0))){
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
                            Some(bitPermutationText(bp, bitRange(bw)),
                                    (tablePath :+ c.name).map{ _.name }.mkString(".​"),
                                    d)
				         else 
				             None
                    (rows ++ row3 ++ rows2, {
                        val x = VariableIdentity.setName(new VariableIdentity, '_)
                        Lambda(Irreversible(), Variable(x, true), Application(Application(Plus(), Application(ofs, Variable(x, false))), Application(bw, Variable(x, false))))
                    })
            }
        case t: BitRegisterType =>
            val bp = {
                val x = VariableIdentity.setName(new VariableIdentity, '_)
                Lambda(Irreversible(), Variable(x, true),
                    bitPermutation(Variable(x, false)) o rangeIndex(NumberLiteral(0), t.bitWidth))
            }
            (t.foldComponents(Seq[(Expression, String, Seq[Node])]()){
                case (c, rows) =>
                    val (rows2, _) = generateTableEntries(c.`type`, {
                            val x = VariableIdentity.setName(new VariableIdentity, '_)
                            Lambda(Irreversible(), Variable(x, true),
                                Application(Application(IndexComposition(), c.position), Application(bp, Variable(x, false))))
                        }, tablePath :+ c.name)
                    val d = { val d = term2Xml(c.annotation(descriptionInfo).getOrElse(StringLiteral("")))
    					              c.annotation(titleInfo).map{ t => <formalpara><title>{ t }</title>{ d }</formalpara> }.getOrElse(d)
    					            } ++ generateTypeDescription(c.`type`)
                    val row3 = if (c.annotation(descriptionInfo).nonEmpty || c.annotation(descriptionInfo).nonEmpty || rows2.isEmpty)
                             Some(bitPermutationText(bp, Lambda(Irreversible(), Undefined(), c.position)),
                                     (tablePath :+ c.name).map{ _.name }.mkString(".​"),
                                     d)
				         else 
				             None
                    rows ++ row3 ++ rows2
            }, Lambda(Irreversible(), Undefined(), t.bitWidth))
        case t: BitUnionType =>
            t.foldVariants(Seq[(Expression, String, Seq[Node])](), (Lambda(Irreversible(), Undefined(), Undefined()): Expression)){
                case (v, (rows, bw)) =>
                    val (rows2, bw2) = generateTableEntries(v.`type`, bitPermutation, tablePath :+ v.name)
                    val d = { val d = term2Xml(v.annotation(descriptionInfo).getOrElse(StringLiteral("")))
    					              v.annotation(titleInfo).map{ t => <formalpara><title>{ t }</title>{ d }</formalpara> }.getOrElse(d)
    					            } ++ generateTypeDescription(v.`type`)
                    val row3 = if (v.annotation(descriptionInfo).nonEmpty || v.annotation(descriptionInfo).nonEmpty || rows2.isEmpty)
                            Some(bitPermutationText(bitPermutation, bitRange(bw2)),
                                    (tablePath :+ v.name).map{ _.name }.mkString(".​"),
                                    d)
		                else
		                    None
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
        <para>Only if { term2Xml(simp.simplify(Block(root, Application(where, Variable(VariableIdentity.setName(new VariableIdentity, 'it), false))), true, defaultContext)._1) }.</para>
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