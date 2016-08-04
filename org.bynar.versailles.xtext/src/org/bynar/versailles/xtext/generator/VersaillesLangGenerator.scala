package org.bynar.versailles.xtext.generator

import org.eclipse.xtext.generator.AbstractGenerator
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.generator.IFileSystemAccess2
import org.eclipse.xtext.generator.IGeneratorContext
import org.bynar.versailles.xtext.versaillesLang.CompilationUnit
import org.bynar.versailles.xtext.Converter
import org.bynar.versailles.PrettyPrinter
import org.bynar.versailles.VariableAnalyzer
import org.bynar.versailles.Simplifier
import org.bynar.versailles.defaultContext
import org.bynar.versailles.VariableIdentity
import org.bynar.versailles.Irreversible
import com.google.inject.Inject

class VersaillesLangGenerator extends AbstractGenerator {
    
    @Inject
    val converter: Converter = null
    @Inject
    val variableAnalyzer: VariableAnalyzer = null
    @Inject
    val prettyPrinter: PrettyPrinter = null
    @Inject
    val simplifier: Simplifier = null

    override def doGenerate(resource: Resource, fsa: IFileSystemAccess2, context: IGeneratorContext) {
        val cu = converter.fromCompilationUnit(resource.getContents.get(0).asInstanceOf[CompilationUnit])
        fsa.generateFile("pp.txt", prettyPrinter.prettyPrint(cu))
        val analyzed = variableAnalyzer.analyze(cu, false, Irreversible())._1
        fsa.generateFile("va.txt", prettyPrinter.prettyPrint(analyzed))
        fsa.generateFile("simp.txt", prettyPrinter.prettyPrint(simplifier.simplify(analyzed, true)._1))
    }

}