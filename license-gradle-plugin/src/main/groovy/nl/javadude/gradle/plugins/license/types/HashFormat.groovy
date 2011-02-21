package nl.javadude.gradle.plugins.license.types

class HashFormat implements LicenseFormat {
	def List<String> transform(List<String> input) {
		List<String> output = []
		input.each { line ->
			output.add('# ' + line)
		}
		return output
	}
}
