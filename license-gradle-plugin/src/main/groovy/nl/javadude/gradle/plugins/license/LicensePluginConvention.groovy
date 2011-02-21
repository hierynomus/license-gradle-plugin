package nl.javadude.gradle.plugins.license

import org.gradle.api.Project

class LicensePluginConvention {
	File license

	def LicensePluginConvention(Project project) {
		license = new File(project.projectDir, "LICENSE")
	}
}
