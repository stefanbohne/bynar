package org.bynar.versailles.xtext.validation

import scala.collection.JavaConversions._
import org.eclipse.xtext.validation.Check
import org.bynar.versailles.xtext.versaillesLang.BlockExpr
import org.bynar.versailles.xtext.versaillesLang.CaseStmt
import org.bynar.versailles.xtext.versaillesLang.CompilationUnit
import org.bynar.versailles.xtext.Converter
import org.bynar.versailles.VariableAnalyzer
import org.bynar.versailles.defaultContext
import org.bynar.versailles.VariableIdentity
import org.bynar.versailles.Term
import org.bynar.versailles.Irreversible
import org.bynar.versailles.Messages
import org.bynar.versailles.Variable
import org.bynar.versailles.xtext.versaillesLang.MatchExpr
import org.bynar.versailles.xtext.Converter
import com.google.inject.Inject
import org.bynar.versailles.xtext.versaillesLang.InterpolatedString
import org.eclipse.emf.ecore.EStructuralFeature
import org.xml.sax.SAXParseException
import org.bynar.versailles.xtext.versaillesLang.VersaillesLangPackage
import scala.xml.XML
import org.bynar.versailles.xtext.versaillesLang.TypeStmt
import org.bynar.versailles.xtext.versaillesLang.DefStmt
import org.bynar.versailles.xtext.versaillesLang.ModuleStmt

class VersaillesLangValidator extends AbstractVersaillesLangValidator {

    @Inject
    val converter: Converter = null
    @Inject
    val variableAnalyzer: VariableAnalyzer = null
    
    def checkDescription(description: InterpolatedString, feature: EStructuralFeature) {
        if (description != null && description.getStrings != null) {
            val text = description.getStrings().map{ s => s.substring(1, s.size - 1) }.mkString("")
    		try {
    		    XML.loadString("<root>" + text + "</root>")
    		} catch {
    		    case e: SAXParseException => 
    		        error(e.getLocalizedMessage, feature)
    		}
        }
    }

    @Check
	def checkBlock(block: BlockExpr) {
		val message = "A block expression must either be empty, end with return, or contain only case statements.";
		val countCase = block.getStatements.getStatements.count{ _.isInstanceOf[CaseStmt] }
		if (block.getScope() == null) {
			if (block.getStatements().getStatements().size() != countCase)
				error(message, block, null);
		} else
			if (countCase > 0)
			    error(message, block.getStatements.getStatements.filter{ _.isInstanceOf[CaseStmt] }.head, null);
	}

    @Check
	def checkBlock(it: MatchExpr) {
		val message = "A match expression must contain only case statements.";
		for (stmt <- it.getStatements.getStatements)
		    if (!stmt.isInstanceOf[CaseStmt]) {
		        error(message, stmt, null);
		        return;
		    }
	}

    @Check
    def checkVariables(it: CompilationUnit) {
        val cu = converter.fromCompilationUnit(it)
        val newIt = variableAnalyzer.analyze(cu, false, Irreversible())._1

        def showErrors(it: Term) {
            assert(it.annotation(Converter.source).nonEmpty)
            for (msg <- Messages.get(it))
                msg.level match {
                case Messages.Error => error(msg.toString, it.annotation(Converter.source).get, null)
                case Messages.Warning => warning(msg.toString, it.annotation(Converter.source).get, null)
                case Messages.Info => info(msg.toString, it.annotation(Converter.source).get, null)
                }
            if (it.isInstanceOf[Variable])
                for (msg <- Messages.get(it.asInstanceOf[Variable].variable))
                    msg.level match {
                    case Messages.Error => error(msg.toString, it.annotation(Converter.source).get, null)
                    case Messages.Warning => warning(msg.toString, it.annotation(Converter.source).get, null)
                    case Messages.Info => info(msg.toString, it.annotation(Converter.source).get, null)
                    }
            for (child <- it.children.values)
                showErrors(child)
        }
        showErrors(newIt)
    }
    
    @Check 
    def checkTypeStmtDescription(stmt: TypeStmt) {
        checkDescription(stmt.getDescription, VersaillesLangPackage.eINSTANCE.getTypeStmt_Description)
    }
    @Check 
    def checkDefStmtDescription(stmt: DefStmt) {
        checkDescription(stmt.getDescription, VersaillesLangPackage.eINSTANCE.getDefStmt_Description)
        checkDescription(stmt.getDescription2, VersaillesLangPackage.eINSTANCE.getDefStmt_Description2)
    }
    @Check 
    def checkModuleStmtDescription(stmt: ModuleStmt) {
        checkDescription(stmt.getDescription, VersaillesLangPackage.eINSTANCE.getModuleStmt_Description)
    }
}