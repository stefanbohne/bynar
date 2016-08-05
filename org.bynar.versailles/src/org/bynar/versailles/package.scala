package org.bynar

package object versailles {

    val defaultContext = Map(
            "+" -> Plus(),
            "-" -> Minus(),
            "*" -> Times(),
            "/" -> Divide(),
            "div" -> IntegerDivide(),
            "^" -> Power(),
            "==" -> Equals(),
            "identity" -> Identity(),
            "fix" -> Fix(),
            "janus" -> Janus(),
            "~" -> Inverse(),
            "|" -> OrElse(),
            "forget" -> Forget(),
            "undefined" -> Undefined(),
            "Number" -> NumberType(),
            "String" -> StringType(),
            "Boolean" -> BooleanType()
    ).map{
        case (n, l) =>
            VariableIdentity.setName(new VariableIdentity(), Symbol(n)) -> l
    }

}