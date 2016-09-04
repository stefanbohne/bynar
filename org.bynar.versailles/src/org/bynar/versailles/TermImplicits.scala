package org.bynar.versailles

object TermImplicits {
    
    implicit class RichExpression(term: Expression) {
        def apply(argument: Expression) =
            Application(term, argument)
        def +(that: Expression) =
            Application(Application(Plus(), that), term)
        def -(that: Expression) =
            Application(Application(Minus(), that), term)
        def *(that: Expression) =
            Application(Application(Times(), that), term)
        def /(that: Expression) =
            Application(Application(Divide(), that), term)
        def ==(that: Expression) =
            Application(Application(Equals(), term), that)
        def !=(that: Expression) =
            Application(Application(NotEquals(), term), that)
        def <(that: Expression) =
            Application(Application(Less(), term), that)
        def <=(that: Expression) =
            Application(Application(LessOrEquals(), term), that)
        def >(that: Expression) =
            Application(Application(Greater(), term), that)
        def >=(that: Expression) =
            Application(Application(GreaterOrEquals(), term), that)
        def ! =
            Application(Not(), term)
        def &&(that: Expression) =
            Application(Application(And(), term), that)
        def ||(that: Expression) =
            Application(Application(Or(), term), that)
            
        def ++(that: Expression) =
            Application(Application(IndexConcatenation(), term), that)
        def o(that: Expression) =
            Application(Application(IndexComposition(), that), term)
    }
    
    implicit def number2Expression(number: Int) =
        NumberLiteral(number)
    implicit def string2Expression(string: String) =
        StringLiteral(string)
    implicit def boolean2Expression(boolean: Boolean) =
        BooleanLiteral(boolean)
    
    object `+` {
        def unapply(t: Term) = t match {
            case Application(Application(Plus(), b), a) => Some(a, b)
            case _ => None
        }
    }
    object `-` {
        def unapply(t: Term) = t match {
            case Application(Application(Minus(), b), a) => Some(a, b)
            case _ => None
        }
    }
    object `*` {
        def unapply(t: Term) = t match {
            case Application(Application(Times(), b), a) => Some(a, b)
            case _ => None
        }
    }
    object `/` {
        def unapply(t: Term) = t match {
            case Application(Application(Divide(), b), a) => Some(a, b)
            case _ => None
        }
    }
    object `==` {
        def unapply(t: Term) = t match {
            case Application(Application(Equals(), a), b) => Some(a, b)
            case _ => None
        }
    }
    object `!=` {
        def unapply(t: Term) = t match {
            case Application(Application(NotEquals(), a), b) => Some(a, b)
            case _ => None
        }
    }
    object `<` {
        def unapply(t: Term) = t match {
            case Application(Application(Less(), a), b) => Some(a, b)
            case _ => None
        }
    }
    object `<=` {
        def unapply(t: Term) = t match {
            case Application(Application(LessOrEquals(), a), b) => Some(a, b)
            case _ => None
        }
    }
    object `>` {
        def unapply(t: Term) = t match {
            case Application(Application(Greater(), a), b) => Some(a, b)
            case _ => None
        }
    }
    object `>=` {
        def unapply(t: Term) = t match {
            case Application(Application(GreaterOrEquals(), a), b) => Some(a, b)
            case _ => None
        }
    }
    object `!` {
        def unapply(t: Term) = t match {
            case Application(Not(), a) => Some(a)
            case _ => None
        }
    }
    object `&&` {
        def unapply(t: Term) = t match {
            case Application(Application(And(), a), b) => Some(a, b)
            case _ => None
        }
    }
    object `||` {
        def unapply(t: Term) = t match {
            case Application(Application(Or(), a), b) => Some(a, b)
            case _ => None
        }
    }
    object `++` {
        def unapply(t: Term) = t match {
            case Application(Application(IndexConcatenation(), a), b) => Some(a, b)
            case _ => None
        }
    }
    object o {
        def unapply(t: Term) = t match {
            case Application(Application(IndexComposition(), b), a) => Some(a, b)
            case _ => None
        }
    }        
        
    object lam {
        def apply(p: Expression, b: Expression) =
            Lambda(Irreversible(), p, b)
        def unapply(t: Term) = t match {
            case Lambda(Irreversible(), p, b) => Some(p, b)
            case _ => None
        }
    }
    object reverse {
        def apply(a: Expression) =
            Application(Reverse(), a)
        def unapply(t: Term) = t match {
            case Application(Reverse(), a) => Some(a)
            case _ => None
        }
    }    
    object singletonIndex {
        def apply(a: Expression) =
            Application(SingletonIndex(), a)
        def unapply(t: Term) = t match {
            case Application(SingletonIndex(), a) => Some(a)
            case _ => None
        }
    }    
    object rangeIndex {
        def apply(a: Expression)(b: Expression) =
            Application(Application(RangeIndex(), a), b)
        def unapply(t: Term) = t match {
            case Application(Application(RangeIndex(), a), b) => Some(a, b)
            case _ => None
        }
    }        
    object infiniteIndex {
        def apply(a: Expression) =
            Application(InfiniteIndex(), a)
        def unapply(t: Term) = t match {
            case Application(InfiniteIndex(), a) => Some(a)
            case _ => None
        }
    }
    object length {
        def apply(a: Expression) =
            Application(Length(), a)
        def unapply(t: Term) = t match {
            case Application(Length(), a) => Some(a)
            case _ => None
        }
    }
    object `if` {
        def apply(a: Expression)(b: Expression)(c: Expression) =
            Application(Application(Application(Variable(defaultContextByName('if), false), a), b), c)
    }
    object min {
        def apply(a: Expression)(b: Expression) =
            Application(Application(Variable(defaultContextByName('min), false), a), b)
    }
    object max {
        def apply(a: Expression)(b: Expression) =
            Application(Application(Variable(defaultContextByName('max), false), a), b)
    }
  
}