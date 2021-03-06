package org.bynar

package object versailles {

    val (defaultContext, defaultContextByName) = {
        import TermImplicits._ 
        val map1 = Map(
            'true -> BooleanLiteral(true),
            'false -> BooleanLiteral(false),
            '+ -> Plus(),
            '- -> Minus(),
            '* -> Times(),
            '/ -> Divide(),
            'div -> IntegerDivide(),
            'pow -> Power(),
            '== -> Equals(),
            'identity -> Identity(),
            'fix -> Fix(),
            'janus -> Janus(),
            '~ -> Inverse(),
            '| -> OrElse(),
            'forget -> Forget(),
            'undefined -> Undefined(),
            'cons -> Cons(),
            'nil -> Nil(),
            'length -> Length(),
            'if -> {
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
            'muladd -> {
                val k = VariableIdentity.setName(new VariableIdentity(), 'k);
                val a = VariableIdentity.setName(new VariableIdentity(), 'a);
                val b = VariableIdentity.setName(new VariableIdentity(), 'b);
                val w = VariableIdentity.setName(new VariableIdentity(), 'w);
                val y = VariableIdentity.setName(new VariableIdentity(), 'y);
                Lambda(Irreversible(), Variable(k, true),
                       Lambda(Inverse(), Tuple(Variable(a, true), Variable(b, true)), Block(Sequence(
                                 Let(Variable(y, true), Plus()(Times()(Variable(k, false))(Variable(a, false)))(Variable(b, true))),
                                 Let(Tuple(), Forget()(Lambda(Irreversible(), Tuple(),
                                         IntegerDivide()(Variable(y, false))(Variable(k, false)))))
                             ),
                             Variable(y, true))))
            },
            'Number -> NumberType(),
            'String -> StringType(),
            'Boolean -> BooleanType()
        )
        val map1Ids = Map(map1.keySet.toSeq.map{
            case n =>
                n -> VariableIdentity.setName(new VariableIdentity(), n)
        }:_*)
      
        val map2 = Map(
            'min -> {
                val a = VariableIdentity.setName(new VariableIdentity(), 'a)
                val b = VariableIdentity.setName(new VariableIdentity(), 'b)
                Lambda(Irreversible(), Variable(a, true),
                            Lambda(Irreversible(), Variable(b, true),
                            Application(Application(Application(Variable(map1Ids('if), false),
                                Application(Application(LessOrEquals(), Variable(a, false)), Variable(b, false))),
                                Lambda(Irreversible(), Tuple(), Variable(a, false))),
                                Lambda(Irreversible(), Tuple(), Variable(b, false)))))
            },
            'max -> {
                val a = VariableIdentity.setName(new VariableIdentity(), 'a)
                val b = VariableIdentity.setName(new VariableIdentity(), 'b)
                Lambda(Irreversible(), Variable(a, true),
                            Lambda(Irreversible(), Variable(b, true),
                            Application(Application(Application(Variable(map1Ids('if), false),
                                Application(Application(GreaterOrEquals(), Variable(a, false)), Variable(b, false))),
                                Lambda(Irreversible(), Tuple(), Variable(a, false))),
                                Lambda(Irreversible(), Tuple(), Variable(b, false)))))
            },
            'divmod -> Reverse()(Variable(map1Ids('muladd), false))
        )
        val map2Ids = Map(map2.keySet.toSeq.map{
            case n =>
                n -> VariableIdentity.setName(new VariableIdentity(), n)
        }:_*)
        
        (map1Ids.map{ case (n, id) => id -> map1(n) } ++ map2Ids.map{ case (n, id) => id -> map2(n) },
            map1Ids ++ map2Ids)
    }

}