package nl.javadude.gradle.plugins.license.types

import nl.javadude.gradle.plugins.license.License

class HashFormat implements LicenseFormat {
	def License transform(List<String> input) {
		def license = new License()
		input.each { line ->
			license.add('# ' + line)
		}
		license
	}
}
