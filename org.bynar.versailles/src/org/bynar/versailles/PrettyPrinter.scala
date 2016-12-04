package org.bynar.versailles

class PrettyPrinter {
    import PrettyPrinter._

    val indentText = "  "
    val result = new StringBuilder
    var precedence: Int = 0
    var indent: Int = 0

    def prettyPrint(term: Term): String = {
        result.clear()
        precedence = 0
        indent = 0
        doPrettyPrint(term)
        result.toString()
    }

    def append(text: Any) {
        result.append(text)
    }
    def paren(left: String, right: String, minPrecedence: Int, print: => Unit) {
        val savePrecedence = precedence
        if (minPrecedence <= precedence)
            append(left)
        print
        precedence = savePrecedence
        if (minPrecedence <= precedence)
            append(right)
    }
    def paren(minPrecedence: Int, print: => Unit) {
        paren("(", ")", minPrecedence, print)
    }
    def binOpLeft(op: String, left: Term, right: Term, opPrecedence: Int) =
        paren(opPrecedence, {
            precedence = opPrecedence - 1
            doPrettyPrint(left)
            append(op)
            precedence = opPrecedence
            doPrettyPrint(right)
        })
    def binOpRight(op: String, left: Term, right: Term, opPrecedence: Int) =
        paren(opPrecedence, {
            precedence = opPrecedence
            doPrettyPrint(left)
            append(op)
            precedence = opPrecedence - 1
            doPrettyPrint(right)
        })
    def binOpNone(op: String, left: Term, right: Term, opPrecedence: Int) =
        paren(opPrecedence, {
            precedence = opPrecedence
            doPrettyPrint(left)
            append(op)
            precedence = opPrecedence
            doPrettyPrint(right)
        })
    def binOpBoth(op: String, left: Term, right: Term, opPrecedence: Int) =
        paren(opPrecedence, {
            precedence = opPrecedence - 1
            doPrettyPrint(left)
            append(op)
            precedence = opPrecedence - 1
            doPrettyPrint(right)
        })
    def prefixOp(op: String, operand: Term, opPrecedence: Int) =
        paren(opPrecedence, {
            append(op)
            precedence = opPrecedence - 1
            doPrettyPrint(operand)
        })
    def suffixOp(op: String, operand: Term, opPrecedence: Int) =
        paren(opPrecedence, {
            precedence = opPrecedence - 1
            doPrettyPrint(operand)
            append(op)
        })

    def doPrettyPrint(term: Term) {
        def isList(term: Term): Boolean =
            term match {
            case Application(Cons(), Tuple(_, l)) => isList(l)
            case Nil() => true
            case _ => false
            }
        
        term match {
        case _ if term.annotation(sourceRepresentationInfo).nonEmpty =>
            append(term.annotation(sourceRepresentationInfo).get)
        case stmt: Statement => doPrettyPrintStatement(stmt)
        case expr: Literal =>
            append(expr.toString)
        case Variable(id, l) =>
            if (l)
                append("?")
            doPrettyPrintName(VariableIdentity.getName(id))
            append("_")
            append(id.hashCode.toHexString)
        case Application(Member(n), b) =>
            suffixOp("." + n.name, b, 60)
        case Tuple(cs@_*) =>
            paren(0, {
                var first = true
                for (c <- cs) {
                    if (!first)
                        append(", ")
                    else
                        first = false
                    doPrettyPrint(c)
                }
                if (cs.size == 1)
                    append(",")
            })
        case TupleType(cts@_*) =>
            paren("<[", "]>", 0, {
                var first = true
                for (c <- cts) {
                    if (!first)
                        append(", ")
                    else
                        first = false
                    doPrettyPrint(c)
                }
                if (cts.size == 1)
                    append(",")
            })
        case OrElseValue(f, s) =>
            paren(40, {
                precedence = 40
                append("or_else")
                append("(")
                precedence = 0
                doPrettyPrint(f)
                append(", ")
                doPrettyPrint(s)
                append(")")
            })
        case term@Application(Application(OrElse(), l), r) =>
            paren("{\n", indentText * indent + "}", 0, {
                indent += 1
                precedence = 0
                def printCase(c: Expression) {
                    c match {
                    case Application(Application(OrElse(), l), r) =>
                        printCase(l)
                        printCase(r)
                    case c =>
                        append(indentText * indent)
                        append("case ")
                        doPrettyPrint(c)
                        append(";\n")
                    }
                }
                printCase(term)
                indent -= 1
            })
        case Application(Application(Plus(), r), l)
            if term.annotation(applicationInfo).getOrElse(ApplicationAsOperator) == ApplicationAsOperator =>
                binOpLeft(" + ", l, r, 10)
        case Application(Application(Minus(), r), l)
            if term.annotation(applicationInfo).getOrElse(ApplicationAsOperator) == ApplicationAsOperator =>
                binOpLeft(" - ", l, r, 10)
        case Application(Application(Times(), r), l)
            if term.annotation(applicationInfo).getOrElse(ApplicationAsOperator) == ApplicationAsOperator =>
                binOpLeft(" * ", l, r, 20)
        case Application(Application(Divide(), r), l)
            if term.annotation(applicationInfo).getOrElse(ApplicationAsOperator) == ApplicationAsOperator =>
                binOpLeft(" / ", l, r, 20)
        case term if term.annotation(applicationInfo).getOrElse(ApplicationAsApplication) == ApplicationAsList && isList(term) =>
            paren("[", "]", 0, {
                def ppList(term: Term) { 
                    term match {
                        case Application(Cons(), Tuple(i, Nil())) =>
                            doPrettyPrint(i)
                        case Application(Cons(), Tuple(i, l)) =>
                            doPrettyPrint(i)
                            append(", ")
                            ppList(l)
                        case Nil() => {}
                    }
                }
                ppList(term)
            })
        case Application(SingletonIndex(), i) =>
            paren("[", "]", 0, {
                doPrettyPrint(i)
            })
        case Application(Application(RangeIndex(), f), t) =>
            paren("[", "]", 0, {
                doPrettyPrint(f)
                append(" .. ")
                doPrettyPrint(t)
            })
        case Application(InfiniteIndex(), f) =>
            paren("[", "]", 0, {
                doPrettyPrint(f)
                append(" .. ")
            })
        case Application(Application(IndexConcatenation(), l), r) =>
            paren("[", "]", 0, {
                precedence = -1
                doPrettyPrint(l)
                append(", ")
                doPrettyPrint(r)
            })
        case Application(Application(IndexComposition(), r), l)
            if term.annotation(applicationInfo).getOrElse(ApplicationAsOperator) == ApplicationAsOperator =>
                binOpLeft(" *..* ", l, r, 15)
        case Application(Application(Equals(), l), r)
            if term.annotation(applicationInfo).getOrElse(ApplicationAsOperator) == ApplicationAsOperator =>
                binOpNone(" == ", l, r, 5)
        case Application(Application(NotEquals(), l), r)
            if term.annotation(applicationInfo).getOrElse(ApplicationAsOperator) == ApplicationAsOperator =>
                binOpNone(" != ", l, r, 5)
        case Application(Application(Less(), l), r)
            if term.annotation(applicationInfo).getOrElse(ApplicationAsOperator) == ApplicationAsOperator =>
               binOpNone(" < ", l, r, 5)
        case Application(Application(LessOrEquals(), l), r)
            if term.annotation(applicationInfo).getOrElse(ApplicationAsOperator) == ApplicationAsOperator =>
                binOpNone(" <= ", l, r, 5)
        case Application(Application(Greater(), l), r)
            if term.annotation(applicationInfo).getOrElse(ApplicationAsOperator) == ApplicationAsOperator =>
                binOpNone(" > ", l, r, 5)
        case Application(Application(GreaterOrEquals(), l), r)
            if term.annotation(applicationInfo).getOrElse(ApplicationAsOperator) == ApplicationAsOperator =>
                binOpNone(" >= ", l, r, 5)
        case Application(Application(And(), l), r)
            if term.annotation(applicationInfo).getOrElse(ApplicationAsOperator) == ApplicationAsOperator =>
                binOpLeft(" && ", l, r, 4)
        case Application(Application(Or(), l), r)
            if term.annotation(applicationInfo).getOrElse(ApplicationAsOperator) == ApplicationAsOperator =>
                binOpRight(" || ", l, r, 3)
        case Application(Reverse(), a)
            if term.annotation(applicationInfo).getOrElse(ApplicationAsOperator) == ApplicationAsOperator =>
                prefixOp("~", a, 50)
        case Application(Application(Typed(), r), l)
            if term.annotation(applicationInfo).getOrElse(ApplicationAsOperator) == ApplicationAsOperator =>
                binOpNone(": ", l, r, 8)
        case Application(f, a)
            if term.annotation(applicationInfo).getOrElse(ApplicationAsApplication) == ApplicationAsMatch =>
                binOpLeft(" match ", a, f, 2)
        case Application(f, a)
            if term.annotation(applicationInfo).getOrElse(ApplicationAsApplication) == ApplicationAsTypeApplication =>
            paren(50, {
                precedence = 50
                doPrettyPrint(f)
                if (!a.isInstanceOf[TupleType])
                    append("<[")
                precedence = 0
                doPrettyPrint(a)
                if (!a.isInstanceOf[TupleType])
                    append("]>")
            })
        case Application(f, a) =>
            paren(40, {
                precedence = 39
                doPrettyPrint(f)
                if (!a.isInstanceOf[Tuple])
                    append("(")
                precedence = 0
                doPrettyPrint(a)
                if (!a.isInstanceOf[Tuple])
                    append(")")
            })
        case Lambda(Inverse(), Undefined(), Undefined()) =>
            append("{}")
        case Lambda(jc, p, b) =>
            binOpRight(" " + jc.toString + " ", p, b, 1)
        case Block(Sequence(), s) =>
            paren("{ return ", " }", 0, {
                doPrettyPrint(s)
            })
        case Block(b, s) =>
            paren("{\n", indentText * indent + "}", 0, {
                indent += 1
                doPrettyPrintStatement(b)
                append(indentText * indent)
                append("return ")
                doPrettyPrint(s)
                append("\n")
                indent -= 1
            })
        case Module(s) =>
            paren("module {\n", indentText * indent + "}", 0, {
                indent += 1
                doPrettyPrintStatement(s)
                append("\n")
                indent -= 1
            })
        }
    }

    def doPrettyPrintStatement(stmt: Statement) {
        stmt match {
        case Let(Variable(id, true), t) if stmt.annotation(letInfo).getOrElse(LetAsLet) == LetAsType =>
            append(indentText * indent)
            append("let type ")
            doPrettyPrintTypeName(VariableIdentity.getName(id))
            append(" = ")
            doPrettyPrint(t)
            append(";\n")
        case Let(BooleanLiteral(true), a) if stmt.annotation(letInfo).getOrElse(LetAsAssert) == LetAsAssert =>
            append(indentText * indent)
            append("let ")
            doPrettyPrint(a)
            append(";\n")
        case Let(Application(Application(Forget(), Lambda(Irreversible(), Tuple(), v)), Tuple()), p) =>
            append(indentText * indent)
            append("forget ")
            doPrettyPrint(p)
            append(" = ")
            doPrettyPrint(v)
            append(";\n")
        case Let(p, Application(Application(Reverse(), Application(Forget(), Lambda(Irreversible(), Tuple(), v))), Tuple())) =>
            append(indentText * indent)
            append("remember ")
            doPrettyPrint(p)
            append(" = ")
            doPrettyPrint(v)
            append(";\n")
        case Let(p, v) =>
            append(indentText * indent)
            append("let ")
            doPrettyPrint(p)
            append(" = ")
            doPrettyPrint(v)
            append(";\n")
        case Fail() =>
            append(indentText * indent)
            append("fail;\n")
        case Sequence() =>
            append(indentText * indent)
            append("pass;\n")
        case Sequence(ss@_*) =>
            for (s <- ss)
                doPrettyPrintStatement(s)
        case Def(id, Module(s)) =>
            append(indentText * indent)
            append("module ")
            doPrettyPrintTypeName(VariableIdentity.getName(id))
            append("_")
            append(id.hashCode.toHexString)
            paren(" {\n", indentText * indent + "};\n", 0, {
                indent += 1
                doPrettyPrintStatement(s)
                append("\n")
                indent -= 1
            })
        case Def(id, t) =>
            append(indentText * indent)
            append("def ")
            doPrettyPrintTypeName(VariableIdentity.getName(id))
            append("_")
            append(id.hashCode.toHexString)
            append(" = ")
            doPrettyPrint(t)
            append(";\n")

        case IfStmt(c, t, e, a) =>
            append(indentText * indent)
            append("if ")
            doPrettyPrint(c)
            append(" then\n")
            indent += 1
            doPrettyPrintStatement(t)
            indent -= 1
            if (e != Sequence()) {
                append(indentText * indent)
                append("else\n")
                indent += 1
                doPrettyPrintStatement(e)
                indent -= 1
            }
            append(indentText * indent)
            append("fi")
            if (a != Undefined()) {
                append(" ")
                doPrettyPrint(a)
            }
            append(";\n")
        }
    }

    def doPrettyPrintName(name: Symbol) {
        if (name.name.matches("[a-z][a-zA-Z_0-9]*"))
            append(name.name)
        else {
            append("`")
            append(name.name.replace("`", "``"))
            append("`")
        }
    }

    def doPrettyPrintTypeName(name: Symbol) {
        if (name.name.matches("[A-Z][a-zA-Z_0-9]*"))
            append(name.name)
        else {
            append("´")
            append(name.name.replace("´", "´´"))
            append("´")
        }
    }
}

object PrettyPrinter {
    sealed trait ApplicationInfo
    case object ApplicationAsApplication extends ApplicationInfo
    case object ApplicationAsTypeApplication extends ApplicationInfo
    case object ApplicationAsOperator extends ApplicationInfo
    case object ApplicationAsMatch extends ApplicationInfo
    case object ApplicationAsList extends ApplicationInfo
    val applicationInfo = new AnnotationKey[ApplicationInfo]()

    trait LetInfo
    case object LetAsLet extends LetInfo
    case object LetAsAssert extends LetInfo
    case object LetAsType extends LetInfo
    val letInfo = new AnnotationKey[LetInfo]
    
    val sourceRepresentationInfo = new AnnotationKey[String]()
}