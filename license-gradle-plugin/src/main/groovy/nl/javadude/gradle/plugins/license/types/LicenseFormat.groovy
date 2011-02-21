package nl.javadude.gradle.plugins.license.types

import nl.javadude.gradle.plugins.license.License

interface LicenseFormat {
	License transform(List<String> input)
}
