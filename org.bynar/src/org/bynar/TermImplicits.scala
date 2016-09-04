package org.bynar

import org.bynar.versailles.Expression
import org.bynar.versailles.Application
import org.bynar.versailles.Term

object TermImplicits {
    object bitWidth {
        def apply(t: Expression) =
            Application(BitWidth(), t)
        def unapply(term: Term) = term match {
            case Application(BitWidth(), t) => Some(t)
            case _ => None
        }
    }
}