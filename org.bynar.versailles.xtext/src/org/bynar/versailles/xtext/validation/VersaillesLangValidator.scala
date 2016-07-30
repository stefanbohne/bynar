package org.bynar.versailles.xtext.validation

import scala.collection.JavaConversions._
import org.eclipse.xtext.validation.Check
import org.bynar.versailles.xtext.versaillesLang.BlockExpr
import org.bynar.versailles.xtext.versaillesLang.CaseStmt

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
    
}