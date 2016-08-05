package org

import org.bynar.versailles.VariableIdentity

package object bynar {
    val defaultContext = Map(
            "bitwidth" -> BitWidth()
    ).map{ case (n, e) => VariableIdentity.setName(new VariableIdentity, Symbol(n)) -> e } ++ org.bynar.versailles.defaultContext
}