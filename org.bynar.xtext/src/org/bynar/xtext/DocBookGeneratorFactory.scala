package org.bynar.xtext

import org.bynar.versailles.Statement

class DocBookGeneratorFactory extends org.bynar.versailles.xtext.DocBookGeneratorFactory {
    override def create(root: Statement) =
        new org.bynar.DocBookGenerator(root)
}