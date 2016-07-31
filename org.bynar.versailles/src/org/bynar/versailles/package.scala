package org.bynar

package object versailles {
  
    val defaultContext = Map(
            "+" -> Plus(),
            "-" -> Minus(),
            "*" -> Times(),
            "/" -> Divide(),
            "div" -> IntegerDivide(),
            "==" -> Equals(),
            "identity" -> Identity(),
            "fix" -> Fix(),
            "janus" -> Janus(),
            "~" -> Inverse(),
            "|" -> OrElse(),
            "forget" -> Forget(),
            "undefined" -> Undefined()
    ) 
        
}