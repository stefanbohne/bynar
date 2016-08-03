package org.bynar

import org.bynar.versailles.Term

class PrettyPrinter extends org.bynar.versailles.PrettyPrinter {
  
    override def prettyPrint(result: StringBuilder, term: Term, indent: Int, precedence: Int) {
        term match {
        case BitFieldType(bw) =>
            result.append("bits ")
            prettyPrint(result, bw, indent, 0)
        case BitRecordType(cs@_*) =>
            result.append("record {\n")
            for (c <- cs) {
                prettyPrint(result, c, indent + 1, 0)
                result.append("\n")
            }
            result.append(indentText * indent)
            result.append("}")
        case term =>
            super.prettyPrint(result, term, indent, precedence)
        }
    }
    
}