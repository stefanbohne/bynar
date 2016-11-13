package org.bynar.versailles.xtext.tests

import com.google.inject.Inject
import org.eclipse.xtext.junit4.util.ParseHelper
import org.bynar.versailles.xtext.versaillesLang.CompilationUnit
import org.bynar.versailles.xtext.Converter
import org.bynar.versailles.VariableAnalyzer
import org.bynar.versailles.Expression
import org.bynar.versailles.JanusClass
import org.bynar.versailles.Irreversible
import org.junit.Test
import org.junit.runner.RunWith
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.XtextRunner

@RunWith(classOf[XtextRunner])
@InjectWith(classOf[VersaillesLangInjectorProvider])
class VersaillesVariableAnalyzerTest {
	
	@Inject
	val parseHelper: ParseHelper[CompilationUnit] = null
	@Inject
    val converter: Converter = null
    @Inject
    val variableAnalyzer: VariableAnalyzer = null
	
	def doAnalyze(source: String, pattern: Boolean, janusClass: JanusClass, context: variableAnalyzer.Context): (Expression, variableAnalyzer.Context) = {
	    val parsed = parseHelper.parse(source)
	    val converted = converter.fromCompilationUnit(parsed)
	    variableAnalyzer.analyze(converted, pattern, janusClass, context)
	}
	
	@Test
	def testLetLeak() {
	    val (e, ctx) = doAnalyze("return { let ?x = 1; return x }", false, Irreversible(), variableAnalyzer.Context(Map.empty))
	    assert(!ctx.containsVariable('x))
	}
	
	@Test
	def testDefLeak() {
	    val (e, ctx) = doAnalyze("return { def x: T = 1; return x }", false, Irreversible(), variableAnalyzer.Context(Map.empty))
	    assert(!ctx.containsVariable('x))
	}

	@Test
	def testModuleLeak() {
	    val (e, ctx) = doAnalyze("module M { let ?y = 1; def x: T = y };", false, Irreversible(), variableAnalyzer.Context(Map.empty))
	    assert(!ctx.containsVariable('x))
	    assert(!ctx.containsVariable('y))
	}
}