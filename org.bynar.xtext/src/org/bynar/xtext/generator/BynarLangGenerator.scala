package org.bynar.xtext.generator

import org.eclipse.xtext.generator.AbstractGenerator
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.generator.IFileSystemAccess2
import org.eclipse.xtext.generator.IGeneratorContext
import org.bynar.xtext.Converter
import org.bynar.VariableAnalyzer
import org.bynar.PrettyPrinter
import org.bynar.Simplifier
import com.google.inject.Inject
import org.bynar.versailles.Irreversible
import org.bynar.versailles.xtext.versaillesLang.CompilationUnit
import org.bynar.versailles.Block
import org.bynar.versailles.xtext.DocBookGeneratorFactory

class BynarLangGenerator extends AbstractGenerator {

    @Inject
    val converter: Converter = null
    @Inject
    val variableAnalyzer: VariableAnalyzer = null
    @Inject
    val prettyPrinter: PrettyPrinter = null
    @Inject
    val simplifier: Simplifier = null
    @Inject
    val dbGen: DocBookGeneratorFactory = null

    def doGenerate(resource: Resource, fsa: IFileSystemAccess2, context: IGeneratorContext) {
	    val cu = converter.fromCompilationUnit(resource.getContents.get(0).asInstanceOf[CompilationUnit])
        fsa.generateFile("pp.txt", prettyPrinter.prettyPrint(cu))
        val analyzed = variableAnalyzer.analyze(cu, false, Irreversible())._1
        fsa.generateFile("va.txt", prettyPrinter.prettyPrint(analyzed))
        fsa.generateFile("simp.txt", prettyPrinter.prettyPrint(simplifier.simplify(analyzed, true)._1))

        val docGen = dbGen.create(analyzed.asInstanceOf[Block].block)
        fsa.generateFile(resource.getURI.trimFileExtension().devicePath() + ".xml", 
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + docGen.generate().toString)
    }
}
