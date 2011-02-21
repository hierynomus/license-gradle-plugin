package nl.javadude.gradle.plugins.license

import org.gradle.api.Project
import org.gradle.api.Plugin
import nl.javadude.gradle.plugins.license.tasks.LicenseTask

class LicensePlugin implements Plugin<Project> {
	void apply(Project project) {
		project.convention.plugins.license = new LicensePluginConvention(project)

		addLicenseTask(project)
	}

	private def addLicenseTask(Project project) {
		project.tasks.add(name: 'license', type: LicenseTask.class)
	}
}
