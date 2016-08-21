package org.bynar.versailles.xtext

import org.bynar.versailles.DocGenerator
import org.bynar.versailles.Statement

class DocGeneratorFactory {
    def create(root: Statement): DocGenerator = new DocGenerator(root) 
}