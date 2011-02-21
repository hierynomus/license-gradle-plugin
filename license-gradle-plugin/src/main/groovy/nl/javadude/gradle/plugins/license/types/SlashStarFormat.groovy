package nl.javadude.gradle.plugins.license.types

import nl.javadude.gradle.plugins.license.License

class SlashStarFormat implements LicenseFormat {


	def License transform(List<String> input) {
		def license = new License()
		license.add('/*')
		input.each {String line ->
			license.add(' * ' + line)
		}
		license.add(' */')

		license
	}
}
