package org.bynar.versailles.xtext

import org.bynar.versailles.DocBookGenerator
import org.bynar.versailles.Statement
import org.bynar.versailles.PrettyPrinter
import org.bynar.versailles.Simplifier

class DocBookGeneratorFactory {
    def create(root: Statement): DocBookGenerator = new DocBookGenerator(root) {
        val pp = new PrettyPrinter
        val simp = new Simplifier
    }
}