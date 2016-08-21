package org.bynar.versailles.xtext

import org.bynar.versailles.PrettyPrinter
import org.bynar.versailles.Simplifier
import org.bynar.versailles.VariableAnalyzer

class VersaillesLangRuntimeModule extends AbstractVersaillesLangRuntimeModule {
    
    override def bindIValueConverterService(): Class[_ <: org.eclipse.xtext.conversion.IValueConverterService] =
        classOf[VersaillesValueConverterService]
	
	def bindConverter(): Class[_ <: Converter] =
	    classOf[Converter]
	def bindVariableAnalyzer(): Class[_ <: VariableAnalyzer] =
	    classOf[VariableAnalyzer]
	def bindSimplifier(): Class[_ <: Simplifier] =
	    classOf[Simplifier]
	def bindPrettyPrinter(): Class[_ <: PrettyPrinter] =
	    classOf[PrettyPrinter]
    def bindDocGeneratorFactory(): Class[_ <: DocGeneratorFactory] =
        classOf[DocGeneratorFactory]

}