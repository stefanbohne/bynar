package org.bynar.xtext.tests

import scala.collection.JavaConversions._
import com.google.inject.Inject
import org.bynar.versailles.xtext.versaillesLang.CompilationUnit
import org.bynar.versailles.xtext.Converter
import org.bynar.versailles.VariableAnalyzer
import org.bynar.versailles.Expression
import org.bynar.versailles.JanusClass
import org.bynar.versailles.Irreversible
import org.junit.Test
import org.junit.runner.RunWith
import org.bynar.versailles.Statement
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.util.ParseHelper

@RunWith(classOf[XtextRunner])
@InjectWith(classOf[BynarLangInjectorProvider])
class BynarVariableAnalyzerTest {
	
	@Inject
	val parseHelper: ParseHelper[CompilationUnit] = null
	@Inject
    val converter: Converter = null
    @Inject
    val variableAnalyzer: VariableAnalyzer = null
	
	def doAnalyze(source: String, pattern: Boolean, janusClass: JanusClass, context: variableAnalyzer.Context): (Statement, variableAnalyzer.Context) = {
	    val parsed = parseHelper.parse(source)
	    val converted = converter.fromStatements(parsed.getStatements, parsed)
	    val ctx0 = variableAnalyzer.analyzeDefinitions(converted, context)
	    variableAnalyzer.analyze(converted, pattern, janusClass, ctx0)
	}
	
	@Test
	def testRecordLeak() {
	    val (e, ctx) = doAnalyze("type X = record { component x: T; def y: T = 1; let ?z = 1; };", false, Irreversible(), variableAnalyzer.Context(Map.empty))
	    assert(!ctx.containsVariable('x))
	    assert(!ctx.containsVariable('y))
	    assert(!ctx.containsVariable('z))
	}
	
	@Test
	def testUnionLeak() {
	    val (e, ctx) = doAnalyze("type X = union { variant x: T; def y: T = 1; let ?z = 1; };", false, Irreversible(), variableAnalyzer.Context(Map.empty))
	    assert(!ctx.containsVariable('x))
	    assert(!ctx.containsVariable('y))
	    assert(!ctx.containsVariable('z))
	}
	
	@Test
	def testRegisterLeak() {
	    val (e, ctx) = doAnalyze("type X = register 1 { component [0] x: T; def y: T = 1; let ?z = 1; };", false, Irreversible(), variableAnalyzer.Context(Map.empty))
	    assert(!ctx.containsVariable('x))
	    assert(!ctx.containsVariable('y))
	    assert(!ctx.containsVariable('z))
	}
	
	@Test
	def testValueLeak() {
	    val (e, ctx) = doAnalyze("type X = T is one of { value x = 0; def y: T = 1; let ?z = 1; };", false, Irreversible(), variableAnalyzer.Context(Map.empty))
	    assert(!ctx.containsVariable('x))
	    assert(!ctx.containsVariable('y))
	    assert(!ctx.containsVariable('z))
	}
	
}