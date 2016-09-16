package org.bynar.versailles.xtext

import org.eclipse.xtext.common.services.DefaultTerminalConverters
import org.eclipse.xtext.conversion.IValueConverter
import org.eclipse.xtext.conversion.ValueConverter
import org.eclipse.xtext.nodemodel.INode
import org.eclipse.xtext.conversion.ValueConverterException
import org.eclipse.xtext.util.Strings
import java.math.BigDecimal
import java.util.regex.Pattern
import java.math.BigInteger

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

	@ValueConverter(rule="NUMBER")
	def IValueConverter<BigDecimal> NUMBER() {
		return new IValueConverter<BigDecimal> {
			val regex = Pattern.compile("(0D|0X|0O|0B)?([0-9A-F]+)(?:\\.([0-9A-F]+))?(?:(?:E|P)((?:\\+|-)?[0-9A-F]+))?")
			override def toValue(String string, INode node) {
				val matcher = regex.matcher(string.replace("_", "").toUpperCase)
				if (!matcher.find)
					throw new ValueConverterException("Couldn't convert number", node, null)
				val base =
					if (matcher.group(1).nullOrEmpty) 10
					else switch (matcher.group(1)) {
						case "0X": 16
						case "0O": 8
						case "0B": 2
						default: 10
					}
				var result = new BigDecimal(new BigInteger(matcher.group(2), base))
				if (!matcher.group(3).nullOrEmpty)
					result += new BigDecimal(new BigInteger(matcher.group(3), base)) /
							new BigDecimal(base).pow(matcher.group(3).length)
				if (!matcher.group(4).nullOrEmpty)
					result *= new BigDecimal(base).pow(Integer.parseInt(matcher.group(4), base))
				return result
			}
			override def toString(BigDecimal value) {
				return value.toString
			}
		}
	}
}