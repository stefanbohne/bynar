package org.bynar.xtext.ide

import com.google.inject.Guice
import org.bynar.xtext.BynarLangRuntimeModule
import org.bynar.xtext.BynarLangStandaloneSetup
import org.eclipse.xtext.util.Modules2

/**
 * Initialization support for running Xtext languages as language servers.
 */
class BynarLangIdeSetup extends BynarLangStandaloneSetup {

	override def createInjector() = {
		Guice.createInjector(Modules2.mixin(new BynarLangRuntimeModule, new BynarLangIdeModule))
	}
	
}
