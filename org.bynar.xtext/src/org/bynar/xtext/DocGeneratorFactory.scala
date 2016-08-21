package org.bynar.xtext

import org.bynar.versailles.Statement

class DocGeneratorFactory extends org.bynar.versailles.xtext.DocGeneratorFactory {
    override def create(root: Statement) =
        new org.bynar.DocGenerator(root)
}