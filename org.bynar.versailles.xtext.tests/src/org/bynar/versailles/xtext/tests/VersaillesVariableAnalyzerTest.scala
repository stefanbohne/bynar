package org.bynar.versailles.xtext.tests

import com.google.inject.Inject
import org.bynar.versailles.xtext.versaillesLang.CompilationUnit
import org.bynar.versailles.xtext.Converter
import org.bynar.versailles.VariableAnalyzer
import org.bynar.versailles.Expression
import org.bynar.versailles.JanusClass
import org.bynar.versailles.Irreversible
import org.junit.Test
import org.junit.runner.RunWith
import org.bynar.versailles.xtext.versaillesLang.Statements
import org.bynar.versailles.Statement
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.util.ParseHelper

@RunWith(classOf[XtextRunner])
@InjectWith(classOf[VersaillesLangInjectorProvider])
class VersaillesVariableAnalyzerTest {
	
	@Inject
	val parseHelper: ParseHelper[CompilationUnit] = null
	@Inject
    val converter: Converter = null
    @Inject
    val variableAnalyzer: VariableAnalyzer = null
	
	def doAnalyze(source: String, pattern: Boolean, janusClass: JanusClass, context: variableAnalyzer.Context): (Statement, variableAnalyzer.Context) = {
	    val parsed = parseHelper.parse(source)
	    val converted = converter.fromStatements(parsed.getStatements)
	    val ctx0 = variableAnalyzer.analyzeDefinitions(converted, context)
	    variableAnalyzer.analyze(converted, pattern, janusClass, ctx0)
	}
	
	@Test
	def testLetLeak() {
	    val (e, ctx) = doAnalyze("let ?z = { let ?x = 1; return 0};", false, Irreversible(), variableAnalyzer.Context(Map.empty))
	    assert(ctx.containsVariable('z))
	    assert(!ctx.containsVariable('x))
	}
	
	@Test
	def testDefLeak() {
	    val (e, ctx) = doAnalyze("def z: T = { let ?x = 1; return 0};", false, Irreversible(), variableAnalyzer.Context(Map.empty))
	    assert(ctx.containsVariable('z))
	    assert(!ctx.containsVariable('x))
	}

	@Test
	def testModuleLeak() {
	    val (e, ctx) = doAnalyze("module M { let ?y = 1; def x: T = y };", false, Irreversible(), variableAnalyzer.Context(Map.empty))
	    assert(ctx.containsVariable('M))
	    assert(!ctx.containsVariable('x))
	    assert(!ctx.containsVariable('y))
	}
}