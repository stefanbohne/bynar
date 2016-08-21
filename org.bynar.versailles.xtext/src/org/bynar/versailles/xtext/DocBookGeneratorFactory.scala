package org.bynar.versailles.xtext

import org.bynar.versailles.DocBookGenerator
import org.bynar.versailles.Statement

class DocBookGeneratorFactory {
    def create(root: Statement): DocBookGenerator = new DocBookGenerator(root) 
}