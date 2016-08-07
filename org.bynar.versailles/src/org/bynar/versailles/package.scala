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
            "if" -> {
                val t = VariableIdentity.setName(new VariableIdentity(), 't)
                val e = VariableIdentity.setName(new VariableIdentity(), 'e)
                Application(
                    Application(
                        OrElse(), 
                        Lambda(
                            Irreversible(),
                            BooleanLiteral(true),
                            Lambda(
                                Irreversible(),
                                Variable(t, true),
                                Lambda(
                                    Irreversible(),
                                    Variable(e, true),
                                    Application(Variable(t, false), Tuple())
                                )
                            )
                        )
                    ),
                    Lambda(
                        Irreversible(),
                        BooleanLiteral(false),
                        Lambda(
                            Irreversible(),
                            Variable(t, true),
                            Lambda(
                                Irreversible(),
                                Variable(e, true),
                                Application(Variable(e, false), Tuple())
                            )
                        )
                    )
                )
            },
            "Number" -> NumberType(),
            "String" -> StringType(),
            "Boolean" -> BooleanType()
    ).map{
        case (n, l) =>
            VariableIdentity.setName(new VariableIdentity(), Symbol(n)) -> l
    }

}