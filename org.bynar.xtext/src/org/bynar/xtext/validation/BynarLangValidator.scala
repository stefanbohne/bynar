package org.bynar.xtext.validation

import org.eclipse.xtext.validation.Check;
import org.bynar.versailles.xtext.versaillesLang.TypeStmt
import scala.xml.XML
import scala.collection.JavaConversions._
import org.xml.sax.SAXParseException
import org.bynar.xtext.bynarLang.BynarLangPackage
import org.bynar.versailles.xtext.versaillesLang.VersaillesLangPackage
import org.bynar.xtext.bynarLang.RecordComponent
import org.bynar.xtext.bynarLang.RegisterComponent
import org.bynar.xtext.bynarLang.UnionVariant
import org.bynar.xtext.bynarLang.EnumValue

class BynarLangValidator extends AbstractBynarLangValidator {

    @Check 
    def checkRecordComponentDescription(stmt: RecordComponent) {
        checkDescription(stmt.getDescription, BynarLangPackage.eINSTANCE.getRecordComponent_Description)
    }
    @Check 
    def checkRegisterComponentDescription(stmt: RegisterComponent) {
        checkDescription(stmt.getDescription, BynarLangPackage.eINSTANCE.getRegisterComponent_Description)
    }
    @Check 
    def checkUnionVariantDescription(stmt: UnionVariant) {
        checkDescription(stmt.getDescription, BynarLangPackage.eINSTANCE.getUnionVariant_Description)
    }
    @Check 
    def checkEnumValueDescription(stmt: EnumValue) {
        checkDescription(stmt.getDescription, BynarLangPackage.eINSTANCE.getEnumValue_Description)
    }
 
}
