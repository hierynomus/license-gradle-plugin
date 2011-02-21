package nl.javadude.gradle.plugins.license.tasks

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.TaskAction

class LicenseTask extends ConventionTask {
	Properties filetypes

	def LicenseTask() {
		filetypes = new Properties()
		filetypes.load this.getClass().getClassLoader().getResourceAsStream("filetypes.properties")
	}

	@TaskAction
	protected void process() {
		project.sourceSets.each { set ->
			set.allSource.each { file ->
				def ext = file.name.substring(file.name.indexOf('.') + 1)
				if (filetypes.hasProperty(ext)) {
					println("License add to: " + file)
				}
			}
		}
	}
}
