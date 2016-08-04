package org.bynar

import scala.collection._
import org.bynar.versailles.Application
import org.bynar.versailles.VariableIdentity
import org.bynar.versailles.Expression
import org.bynar.versailles.Lambda
import org.bynar.versailles.Irreversible
import org.bynar.versailles.Variable

class Simplifier extends org.bynar.versailles.Simplifier {
  
    override def defaultContext = org.bynar.defaultContext
    
    override def simplifyApplication(app: Application, forward: Boolean, context: Map[VariableIdentity, Expression]): (Expression, Map[VariableIdentity, Expression]) =
        (app.function, app.argument) match {
        case (BitWidth(), bt: BitTypeExpression) =>
            val x = VariableIdentity.setName(new VariableIdentity(), "_")
            (Lambda(Irreversible(), Variable(x, true), bt.dependentBitWidth(Variable(x, false))), context)
        case _ => super.simplifyApplication(app, forward, context)
        }
    
}