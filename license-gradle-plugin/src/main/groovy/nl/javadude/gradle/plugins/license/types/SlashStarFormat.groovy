package nl.javadude.gradle.plugins.license.types

class SlashStarFormat implements LicenseFormat {


	def List<String> transform(List<String> input) {
		List<String> output = []
		output.add('/*')
		input.each {String line ->
			output.add(' * ' + line)
		}
		output.add(' */')

		return output
	}
}
