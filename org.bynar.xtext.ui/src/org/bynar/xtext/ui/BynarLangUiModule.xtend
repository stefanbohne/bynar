/*
 * generated by Xtext 2.10.0
 */
package org.bynar.xtext.ui

import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfiguration
import org.eclipse.xtext.ide.editor.syntaxcoloring.ISemanticHighlightingCalculator
import org.bynar.versailles.xtext.ui.syntaxcoloring.VersaillesHighlightingConfiguration
import org.bynar.versailles.xtext.ui.syntaxcoloring.VersaillesSemanticHighlightingCalculator
import org.eclipse.xtext.ide.editor.syntaxcoloring.AbstractAntlrTokenToAttributeIdMapper
import org.bynar.versailles.xtext.ui.syntaxcoloring.VersaillesAntlrTokenToAttributeIdMapper

/**
 * Use this class to register components to be used within the Eclipse IDE.
 */
@FinalFieldsConstructor
class BynarLangUiModule extends AbstractBynarLangUiModule {
	
	def Class<? extends AbstractAntlrTokenToAttributeIdMapper> bindAbstractAntlrTokenToAttributeIdMapper() {
		return VersaillesAntlrTokenToAttributeIdMapper
	}
	def Class<? extends IHighlightingConfiguration> bindIHighlightingConfiguration() {
		return VersaillesHighlightingConfiguration;
	}
	def Class<? extends ISemanticHighlightingCalculator> bindISemanticHighlightingCalculator() {
		return VersaillesSemanticHighlightingCalculator;
	}
}
