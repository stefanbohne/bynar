package org.bynar.versailles.xtext.ui.syntaxcoloring

import org.eclipse.xtext.ide.editor.syntaxcoloring.DefaultSemanticHighlightingCalculator
import org.eclipse.xtext.ide.editor.syntaxcoloring.IHighlightedPositionAcceptor
import org.eclipse.xtext.util.CancelIndicator
import org.eclipse.emf.ecore.EObject
import org.bynar.versailles.xtext.versaillesLang.Variable
import org.bynar.versailles.xtext.versaillesLang.VersaillesLangPackage
import org.eclipse.xtext.ide.editor.syntaxcoloring.HighlightingStyles
import org.bynar.versailles.xtext.versaillesLang.DefStmt
import org.bynar.versailles.xtext.versaillesLang.TypeStmt
import org.bynar.versailles.xtext.versaillesLang.MemberAccessExpr
import org.bynar.versailles.xtext.versaillesLang.MemberAccessType
import org.bynar.versailles.xtext.versaillesLang.TupleComponent
import org.bynar.versailles.xtext.versaillesLang.JanusClassVariable
import org.bynar.versailles.xtext.versaillesLang.TypeVariable
import org.bynar.versailles.xtext.versaillesLang.TupleTypeComponent
import org.bynar.versailles.xtext.versaillesLang.NamePath
import org.eclipse.xtext.ui.editor.syntaxcoloring.DefaultHighlightingConfiguration
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfigurationAcceptor
import org.eclipse.xtext.ui.editor.utils.TextStyle
import org.eclipse.xtext.ide.editor.syntaxcoloring.DefaultAntlrTokenToAttributeIdMapper
import org.eclipse.swt.SWT

public class VersaillesAntlrTokenToAttributeIdMapper extends DefaultAntlrTokenToAttributeIdMapper {
	override protected String calculateId(String tokenName, int tokenType) {
		if (tokenName.startsWith("RULE_INTERPOL_"))
			return HighlightingStyles.STRING_ID
		return super.calculateId(tokenName, tokenType)
	}
}

public class VersaillesHighlightingConfiguration extends DefaultHighlightingConfiguration {
	public static final String TYPE_ID = "type"
	
	override public void configure(IHighlightingConfigurationAcceptor acceptor) {
		super.configure(acceptor)
		acceptor.acceptDefaultHighlighting(TYPE_ID, "Type", typeTextStyle());
	}
	
	def TextStyle typeTextStyle() {
		val textStyle = defaultTextStyle().copy();
		textStyle.style = SWT.ITALIC
		return textStyle;
	}
	
}

public class VersaillesSemanticHighlightingCalculator extends DefaultSemanticHighlightingCalculator  {
	
	override protected boolean highlightElement(EObject object, IHighlightedPositionAcceptor acceptor,
			CancelIndicator cancelIndicator) {
	    switch object {
    	NamePath:
    		highlightFeature(acceptor, object, object.eClass.getEStructuralFeature(VersaillesLangPackage.NAME_PATH__STEPS), HighlightingStyles.DEFAULT_ID)
    	Variable:
    		highlightFeature(acceptor, object, object.eClass.getEStructuralFeature(VersaillesLangPackage.VARIABLE__NAME), HighlightingStyles.DEFAULT_ID)
    	TupleComponent:
    		highlightFeature(acceptor, object, object.eClass.getEStructuralFeature(VersaillesLangPackage.TUPLE_COMPONENT__NAME), HighlightingStyles.DEFAULT_ID)
    	MemberAccessExpr:
    		highlightFeature(acceptor, object, object.eClass.getEStructuralFeature(VersaillesLangPackage.MEMBER_ACCESS_EXPR__MEMBER_NAME), HighlightingStyles.DEFAULT_ID)    		
    	DefStmt: {
    		highlightFeature(acceptor, object, object.eClass.getEStructuralFeature(VersaillesLangPackage.DEF_STMT__NAME), HighlightingStyles.DEFAULT_ID)
    		highlightFeature(acceptor, object, object.eClass.getEStructuralFeature(VersaillesLangPackage.DEF_STMT__NAME2), HighlightingStyles.DEFAULT_ID)
    	}
    	TypeStmt:
    		highlightFeature(acceptor, object, object.eClass.getEStructuralFeature(VersaillesLangPackage.TYPE_STMT__NAME), VersaillesHighlightingConfiguration.TYPE_ID)
    	MemberAccessType:
    		highlightFeature(acceptor, object, object.eClass.getEStructuralFeature(VersaillesLangPackage.MEMBER_ACCESS_TYPE__MEMBER_NAME), VersaillesHighlightingConfiguration.TYPE_ID)    		
    	TypeVariable:
    		highlightFeature(acceptor, object, object.eClass.getEStructuralFeature(VersaillesLangPackage.TYPE_VARIABLE__NAME), VersaillesHighlightingConfiguration.TYPE_ID)    		
    	TupleTypeComponent:
    		highlightFeature(acceptor, object, object.eClass.getEStructuralFeature(VersaillesLangPackage.TUPLE_TYPE_COMPONENT__NAME), VersaillesHighlightingConfiguration.TYPE_ID)    		
    	JanusClassVariable:
    		highlightFeature(acceptor, object, object.eClass.getEStructuralFeature(VersaillesLangPackage.JANUS_CLASS_VARIABLE__NAME), VersaillesHighlightingConfiguration.TYPE_ID)    		
	    }
		return false
  	}
	
}