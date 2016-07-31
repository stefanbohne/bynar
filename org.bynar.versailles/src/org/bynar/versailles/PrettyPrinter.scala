package org.bynar.versailles

class PrettyPrinter {
    val indentText = "  "

    def prettyPrint(term: Term, indent: Int = 0, precedence: Int = 0): String = {
        val result = new StringBuilder
        prettyPrint(result, term, indent, precedence)
        result.toString
    }

    def prettyPrint(result: StringBuilder, term: Term, indent: Int, precedence: Int) {
        def paren(minPrecedence: Int, print: => Unit) {
            if (minPrecedence <= precedence)
                result.append("(")
            print
            if (minPrecedence <= precedence)
                result.append(")")
        }
        def binOpLeft(op: String, left: Term, right: Term, opPrecedence: Int) =
            paren(opPrecedence, {
                prettyPrint(result, left, indent, opPrecedence - 1)
                result.append(" ").append(op).append(" ")
                prettyPrint(result, right, indent, opPrecedence)
            })
        def binOpRight(op: String, left: Term, right: Term, opPrecedence: Int) =
            paren(opPrecedence, {
                prettyPrint(result, left, indent, opPrecedence)
                result.append(" ").append(op).append(" ")
                prettyPrint(result, right, indent, opPrecedence - 1)
            })
        def binOpNone(op: String, left: Term, right: Term, opPrecedence: Int) =
            paren(opPrecedence, {
                prettyPrint(result, left, indent, opPrecedence)
                result.append(" ").append(op).append(" ")
                prettyPrint(result, right, indent, opPrecedence)
            })
        def prefixOp(op: String, operand: Term, opPrecedence: Int) =
            paren(opPrecedence, {
                result.append(op)
                prettyPrint(result, operand, indent, opPrecedence - 1)
            })
        term match {
        case stmt: Statement => prettyPrintStatement(result, stmt, indent)
        case expr: Literal =>
            result.append(expr.toString)
        case Variable(id, l) =>
            if (l)
                result.append("?")
            prettyPrintName(result, VariableIdentity.getName(id))
            result.append("_")
            result.append(id.hashCode.toHexString)
        case Tuple(cs@_*) =>
            result.append("(")
            var first = true
            for (c <- cs) {
                if (!first)
                    result.append(", ")
                else
                    first = false
                prettyPrint(result, c, indent, 0)
            }
            if (cs.size == 1)
                result.append(",")
            result.append(")")
        case Application(Application(Plus(), r), l) => binOpLeft("+", l, r, 10)
        case Application(Application(Minus(), r), l) => binOpLeft("-", l, r, 10)
        case Application(Application(Times(), r), l) => binOpLeft("*", l, r, 20)
        case Application(Application(Divide(), r), l) => binOpLeft("/", l, r, 20)
        case Application(Application(Equals(), l), r) => binOpNone("==", l, r, 5)
        case Application(Reverse(), a) => prefixOp("~", a, 50)
        case Application(f, a: Tuple) => paren(40, {
                prettyPrint(result, f, indent, 40)
                prettyPrint(result, a, indent, 0)
            })
        case Application(f, a) => paren(40, {
                prettyPrint(result, f, indent, 40)
                result.append("(")
                prettyPrint(result, a, indent, 0)
                result.append(")")
            })
        case Lambda(Inverse(), Undefined(), Undefined()) => 
            result.append("{}")
        case Lambda(jc, p, b) =>
            binOpRight(jc.toString, p, b, 1)
        case Block(Sequence(), s) =>
            result.append("{ return ")
            prettyPrint(result, s, indent, 0)
            result.append(" }")
        case Block(b, s) =>
            result.append("{\n")
            prettyPrintStatement(result, b, indent + 1)
            result.append(indentText * (indent + 1))
            result.append("return ")
            prettyPrint(result, s, indent + 1, 0)
            result.append("\n")
            result.append(indentText * indent)
            result.append("}")
        }
    }

    def prettyPrintStatement(result: StringBuilder, stmt: Statement, indent: Int) {
        stmt match {
        case Let(Application(Application(Forget(), Lambda(Irreversible(), Tuple(), v)), Tuple()), p) =>
            result.append(indentText * indent)
            result.append("forget ")
            prettyPrint(result, p, indent, 0)
            result.append(" = ")
            prettyPrint(result, v, indent, 0)
            result.append(";\n")
        case Let(p, Application(Application(Reverse(), Application(Forget(), Lambda(Irreversible(), Tuple(), v))), Tuple())) =>
            result.append(indentText * indent)
            result.append("remember ")
            prettyPrint(result, p, indent, 0)
            result.append(" = ")
            prettyPrint(result, v, indent, 0)
            result.append(";\n")
        case Let(p, v) =>
            result.append(indentText * indent)
            result.append("let ")
            prettyPrint(result, p, indent, 0)
            result.append(" = ")
            prettyPrint(result, v, indent, 0)
            result.append(";\n")
        case Fail() =>
            result.append("fail;\n")
        case Sequence() =>
            result.append("pass;\n")
        case Sequence(ss@_*) =>
            for (s <- ss)
                prettyPrintStatement(result, s, indent)
        }
    }

    def prettyPrintName(result: StringBuilder, name: String) {
        if (name.matches("[a-z][a-zA-Z_0-9]*"))
            result.append(name)
        else {
            result.append("`")
            result.append(name.replace("`", "``"))
            result.append("`")
        }
    }
}