package org.bynar.xtext

import org.bynar.PrettyPrinter
import org.bynar.VariableAnalyzer
import org.bynar.Simplifier
import org.bynar.versailles.xtext.VersaillesValueConverterService

class BynarLangRuntimeModule extends AbstractBynarLangRuntimeModule {
    override def bindIValueConverterService(): Class[_ <: org.eclipse.xtext.conversion.IValueConverterService] =
        classOf[VersaillesValueConverterService]
	
    def bindConverter(): Class[_ <: org.bynar.versailles.xtext.Converter] =
	    classOf[Converter]
	def bindVariableAnalyzer(): Class[_ <: org.bynar.versailles.VariableAnalyzer] =
	    classOf[VariableAnalyzer]
	def bindSimplifier(): Class[_ <: org.bynar.versailles.Simplifier] =
	    classOf[Simplifier]
	def bindPrettyPrinter(): Class[_ <: org.bynar.versailles.PrettyPrinter] =
	    classOf[PrettyPrinter]
    def bindDocBookGeneratorFactory(): Class[_ <: org.bynar.versailles.xtext.DocBookGeneratorFactory] =
	    classOf[DocBookGeneratorFactory]
}
