package org

import org.bynar.versailles.VariableIdentity

package object bynar {
    val defaultContext = Map(
            "bitwidth" -> BitWidth()
    ).map{ case (n, e) => VariableIdentity.setName(new VariableIdentity, n) -> e } ++ org.bynar.versailles.defaultContext 
}