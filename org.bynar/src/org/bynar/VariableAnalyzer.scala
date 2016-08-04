package org.bynar

import org.bynar.versailles.Expression
import org.bynar.versailles.JanusClass
import org.bynar.versailles.Irreversible
import org.bynar.versailles.Messages
import org.bynar.versailles.Message
import org.bynar.versailles.Statement
import org.bynar.versailles.VariableIdentity

class VariableAnalyzer extends org.bynar.versailles.VariableAnalyzer {
    
    import VariableAnalyzer._
    import org.bynar.versailles.VariableAnalyzer._
    
    override def defaultContext = Context(Map(org.bynar.defaultContext.keySet.toSeq.map{ 
        id => VariableIdentity.getName(id) -> ContextEntry(id, false) 
    }:_*)) 
    
    override def analyze(it: Expression, pattern: Boolean, janusClass: JanusClass, context: Context): (Expression, Context) =
        it match {
        case it@BitFieldType(bw) =>
            if (pattern || janusClass != Irreversible())
                (Messages.add(it, IllegalUseOfType), context)
            else {
                val (bw1, ctx1) = analyze(bw, pattern, janusClass, context)
                (it.copy(bw1), ctx1)
            }
        case it@BitRecordType(cs@_*) =>
            if (pattern || janusClass != Irreversible())
                (Messages.add(it, IllegalUseOfType), context)
            else {
                val (cs1, ctx1) = ((Seq[BitRecordComponent](), context) /: cs){
                case ((cs2, ctx2), c) =>
                    val (c3, ctx3) = analyze(c, pattern, janusClass, ctx2)
                    (cs2 :+ c3.asInstanceOf[BitRecordComponent], ctx3)
                }
                (it.copy(cs1:_*), ctx1)
            }
        case it => super.analyze(it, pattern, janusClass, context)
        }
    
    override def analyze(it: Statement, pattern: Boolean, janusClass: JanusClass, context: Context): (Statement, Context) =
        it match {
        case it@BitRecordComponent(n, t) =>
            if (pattern || janusClass != Irreversible())
                (Messages.add(it, IllegalUseOfType), context)
            else {
                val (t1, ctx1) = analyze(t, pattern, janusClass, context)
                (it.copy(`type` = t1), ctx1)
            }
        case it => super.analyze(it, pattern, janusClass, context)
        }
        
}

object VariableAnalyzer {
    case object IllegalUseOfType extends Message {
        override def level = Messages.Error
        override def toString = "Illegal use of type"
    }
}