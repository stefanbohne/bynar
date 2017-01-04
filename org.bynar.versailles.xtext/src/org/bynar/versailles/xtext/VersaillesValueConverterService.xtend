package org.bynar.versailles.xtext

import org.eclipse.xtext.common.services.DefaultTerminalConverters
import org.eclipse.xtext.conversion.IValueConverter
import org.eclipse.xtext.conversion.ValueConverter
import org.eclipse.xtext.nodemodel.INode
import org.eclipse.xtext.conversion.ValueConverterException
import org.eclipse.xtext.util.Strings

class VersaillesValueConverterService extends DefaultTerminalConverters {
	@ValueConverter(rule="Name")
	def IValueConverter<String> Name() {
		return new IValueConverter<String> {
			override def String toValue(String string, INode node) {
				if (Strings.isEmpty(string)) {
                    throw new ValueConverterException("Couldn't convert empty id", node, null)
                } else if (string.length >= 2 && string.substring(0, 1) == "`") {
                	return string.substring(1, string.length - 1)
            	} else
            		return string
			}
			override def String toString(String string) {
				if (string.matches("[a-z][a-zA-Z0-9_]*"))
					return string
				else
					return "`" + string + "`"
			}
		}
	}

	@ValueConverter(rule="TypeName")
	def IValueConverter<String> TypeName() {
		return new IValueConverter<String> {
			override def toValue(String string, INode node) {
				if (Strings.isEmpty(string)) {
                    throw new ValueConverterException("Couldn't convert empty id", node, null)
                } else if (string.length > 2 && string.charAt(0) == '´') {
                	return string.substring(1, string.length - 2)
            	} else
            		return string
			}
			override def toString(String string) {
				if (string.matches("[A-Z][a-zA-Z0-9_]*"))
					return string
				else
					return "´" + string + "´"
			}
		}
	}
}