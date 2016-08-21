package org.bynar

import org.bynar.versailles.StringLiteral
import org.bynar.versailles.Term

class TextPrettyPrinter extends PrettyPrinter {
  
    override def doPrettyPrint(term: Term) =
        term match {
        case StringLiteral(s) => 
            result.append(s)
        case _ => 
            super.doPrettyPrint(term)
        }
    
}