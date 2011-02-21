package nl.javadude.gradle.plugins.license.types

interface LicenseFormat {
	List<String> transform(List<String> input)
}
