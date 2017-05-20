package org.bynar.versailles.xtext.ide

import com.google.inject.Guice
import org.bynar.versailles.xtext.VersaillesLangRuntimeModule
import org.bynar.versailles.xtext.VersaillesLangStandaloneSetup
import org.eclipse.xtext.util.Modules2

/**
 * Initialization support for running Xtext languages as language servers.
 */
class VersaillesLangIdeSetup extends VersaillesLangStandaloneSetup {

	override def createInjector() = {
		Guice.createInjector(Modules2.mixin(new VersaillesLangRuntimeModule, new VersaillesLangIdeModule))
	}
	
}
