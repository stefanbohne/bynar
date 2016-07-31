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

class VersaillesLangValidator extends AbstractVersaillesLangValidator {
  
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
    def checkVariables(it: CompilationUnit) {
        val cu = Converter.fromCompilationUnit(it)
        val varAna = new VariableAnalyzer
        val newIt = varAna.analyze(cu, defaultContext.keySet)
        
        def showErrors(it: Term) {
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
}