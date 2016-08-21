package org.bynar.xtext

import org.bynar.PrettyPrinter
import org.bynar.VariableAnalyzer
import org.bynar.Simplifier

class BynarLangRuntimeModule extends AbstractBynarLangRuntimeModule {
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
