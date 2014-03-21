package nl.javadude.gradle.plugins.license

import groovy.lang.Closure;

class LicensesReportHtml {
	
    boolean enabled
    File destination
	
	Closure dependencyToLicenseHtmlHeadRenderer = LicenseReporter.defaultDependencyToLicenseHtmlHeadRenderer
	Closure dependencyToLicenseTableHeadRenderer = LicenseReporter.defaultDependencyToLicenseTableHeadRenderer
	Closure dependencyToLicenseTableRowRenderer = LicenseReporter.defaultDependencyToLicenseTableRowRenderer
	
}
