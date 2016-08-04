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

class VersaillesLangGenerator extends AbstractGenerator {

    override def doGenerate(resource: Resource, fsa: IFileSystemAccess2, context: IGeneratorContext) {

        val cu = new Converter().fromCompilationUnit(resource.getContents.get(0).asInstanceOf[CompilationUnit])
        val pp = new PrettyPrinter()
        fsa.generateFile("pp.txt", pp.prettyPrint(cu))
        val va = new VariableAnalyzer()
        val analyzed = va.analyze(cu, defaultContext.keySet)
        fsa.generateFile("va.txt", pp.prettyPrint(analyzed))
        val simp = new Simplifier()
        fsa.generateFile("simp.txt", pp.prettyPrint(simp.simplify(analyzed, true, defaultContext)._1))
    }

}