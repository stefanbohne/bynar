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

    def paren(left: String, right: String, minPrecedence: Int, print: => Unit) {
        val savePrecedence = precedence
        if (minPrecedence <= precedence)
            result.append(left)
        print
        precedence = savePrecedence
        if (minPrecedence <= precedence)
            result.append(right)
    }
    def paren(minPrecedence: Int, print: => Unit) {
        paren("(", ")", minPrecedence, print)
    }
    def binOpLeft(op: String, left: Term, right: Term, opPrecedence: Int) =
        paren(opPrecedence, {
            precedence = opPrecedence - 1
            doPrettyPrint(left)
            result.append(op)
            precedence = opPrecedence
            doPrettyPrint(right)
        })
    def binOpRight(op: String, left: Term, right: Term, opPrecedence: Int) =
        paren(opPrecedence, {
            precedence = opPrecedence
            doPrettyPrint(left)
            result.append(op)
            precedence = opPrecedence - 1
            doPrettyPrint(right)
        })
    def binOpNone(op: String, left: Term, right: Term, opPrecedence: Int) =
        paren(opPrecedence, {
            precedence = opPrecedence
            doPrettyPrint(left)
            result.append(op)
            precedence = opPrecedence
            doPrettyPrint(right)
        })
    def prefixOp(op: String, operand: Term, opPrecedence: Int) =
        paren(opPrecedence, {
            result.append(op)
            precedence = opPrecedence - 1
            doPrettyPrint(operand)
        })
    def suffixOp(op: String, operand: Term, opPrecedence: Int) =
        paren(opPrecedence, {
            precedence = opPrecedence - 1
            doPrettyPrint(operand)
            result.append(op)
        })

    def doPrettyPrint(term: Term) {
        term match {
        case stmt: Statement => doPrettyPrintStatement(stmt)
        case expr: Literal =>
            result.append(expr.toString)
        case Variable(id, l) =>
            if (l)
                result.append("?")
            doPrettyPrintName(VariableIdentity.getName(id))
            result.append("_")
            result.append(id.hashCode.toHexString)
        case Application(Member(n), b) =>
            suffixOp("." + n.name, b, 60)
        case Tuple(cs@_*) =>
            paren(0, {
                var first = true
                for (c <- cs) {
                    if (!first)
                        result.append(", ")
                    else
                        first = false
                    doPrettyPrint(c)
                }
                if (cs.size == 1)
                    result.append(",")
            })
        case TupleType(cts@_*) =>
            paren("<[", "]>", 0, {
                var first = true
                for (c <- cts) {
                    if (!first)
                        result.append(", ")
                    else
                        first = false
                    doPrettyPrint(c)
                }
                if (cts.size == 1)
                    result.append(",")
            })
        case term@Application(Application(OrElse(), l), r) =>
            paren("{\n", indentText * indent + "}", 0, {
                indent += 1
                var tmp: Expression = term
                while (tmp match {
                    case Application(Application(OrElse(), l), r) =>
                        result.append(indentText * indent)
                        result.append("case ")
                        doPrettyPrint(l)
                        result.append(";\n")
                        tmp = r
                        true
                    case _ => false
                }) {}
                result.append(indentText * indent)
                result.append("case ")
                doPrettyPrint(tmp)
                result.append(";\n")
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
        case Application(Application(Equals(), l), r)
            if term.annotation(applicationInfo).getOrElse(ApplicationAsOperator) == ApplicationAsOperator =>
                binOpNone(" == ", l, r, 5)
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
                    result.append("<[")
                precedence = 0
                doPrettyPrint(a)
                if (!a.isInstanceOf[TupleType])
                    result.append("]>")
            })
        case Application(f, a) =>
            paren(40, {
                precedence = 40
                doPrettyPrint(f)
                if (!a.isInstanceOf[Tuple])
                    result.append("(")
                precedence = 0
                doPrettyPrint(a)
                if (!a.isInstanceOf[Tuple])
                    result.append(")")
            })
        case Lambda(Inverse(), Undefined(), Undefined()) =>
            result.append("{}")
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
                result.append(indentText * indent)
                result.append("return ")
                doPrettyPrint(s)
                result.append("\n")
                indent -= 1
            })
        }
    }

    def doPrettyPrintStatement(stmt: Statement) {
        stmt match {
        case Let(Application(Application(Forget(), Lambda(Irreversible(), Tuple(), v)), Tuple()), p) =>
            result.append(indentText * indent)
            result.append("forget ")
            doPrettyPrint(p)
            result.append(" = ")
            doPrettyPrint(v)
            result.append(";\n")
        case Let(p, Application(Application(Reverse(), Application(Forget(), Lambda(Irreversible(), Tuple(), v))), Tuple())) =>
            result.append(indentText * indent)
            result.append("remember ")
            doPrettyPrint(p)
            result.append(" = ")
            doPrettyPrint(v)
            result.append(";\n")
        case Let(p, v) =>
            result.append(indentText * indent)
            result.append("let ")
            doPrettyPrint(p)
            result.append(" = ")
            doPrettyPrint(v)
            result.append(";\n")
        case Fail() =>
            result.append(indentText * indent)
            result.append("fail;\n")
        case Sequence() =>
            result.append(indentText * indent)
            result.append("pass;\n")
        case Sequence(ss@_*) =>
            for (s <- ss)
                doPrettyPrintStatement(s)
        case Def(id, t) =>
            result.append(indentText * indent)
            result.append("def ")
            doPrettyPrintTypeName(VariableIdentity.getName(id))
            result.append("_")
            result.append(id.hashCode.toHexString)
            result.append(" = ")
            doPrettyPrint(t)
            result.append(";\n")

        case IfStmt(c, t, e, a) =>
            result.append(indentText * indent)
            result.append("if ")
            doPrettyPrint(c)
            result.append(" then\n")
            indent += 1
            doPrettyPrintStatement(t)
            indent -= 1
            if (e != Sequence()) {
                result.append(indentText * indent)
                result.append("else\n")
                indent += 1
                doPrettyPrintStatement(e)
                indent -= 1
            }
            result.append(indentText * indent)
            result.append("fi")
            if (a != Undefined()) {
                result.append(" ")
                doPrettyPrint(a)
            }
            result.append(";\n")
        }
    }

    def doPrettyPrintName(name: Symbol) {
        if (name.name.matches("[a-z][a-zA-Z_0-9]*"))
            result.append(name.name)
        else {
            result.append("`")
            result.append(name.name.replace("`", "``"))
            result.append("`")
        }
    }

    def doPrettyPrintTypeName(name: Symbol) {
        if (name.name.matches("[A-Z][a-zA-Z_0-9]*"))
            result.append(name.name)
        else {
            result.append("´")
            result.append(name.name.replace("´", "´´"))
            result.append("´")
        }
    }
}

object PrettyPrinter {
    trait ApplicationInfo
    case object ApplicationAsApplication extends ApplicationInfo
    case object ApplicationAsTypeApplication extends ApplicationInfo
    case object ApplicationAsOperator extends ApplicationInfo
    case object ApplicationAsMatch extends ApplicationInfo
    val applicationInfo = new AnnotationKey[ApplicationInfo]()
}