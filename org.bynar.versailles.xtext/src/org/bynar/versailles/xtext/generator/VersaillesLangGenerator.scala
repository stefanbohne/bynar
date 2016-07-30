package org.bynar.versailles.xtext.generator

import org.eclipse.xtext.generator.AbstractGenerator
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.generator.IFileSystemAccess2
import org.eclipse.xtext.generator.IGeneratorContext
import org.bynar.versailles.xtext.versaillesLang.CompilationUnit
import org.bynar.versailles.xtext.Converter
import org.bynar.versailles.PrettyPrinter

class VersaillesLangGenerator extends AbstractGenerator {
    
    override def doGenerate(resource: Resource, fsa: IFileSystemAccess2, context: IGeneratorContext) {
         
        val cu = Converter.fromCompilationUnit(resource.getContents.get(0).asInstanceOf[CompilationUnit])
        fsa.generateFile("pp.txt", new PrettyPrinter().prettyPrint(cu))
        
    }
  
}