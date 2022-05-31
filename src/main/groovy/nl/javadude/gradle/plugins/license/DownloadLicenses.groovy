/*
 * Copyright (C)2011 - Jeroen van Erp <jeroen@javadude.nl>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.javadude.gradle.plugins.license

import org.gradle.api.DefaultTask
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*

import static nl.javadude.gradle.plugins.license.DownloadLicensesExtension.license

/**
 * Task for downloading dependency licenses and generating reports.
 */
public class DownloadLicenses extends ConventionTask {

    /**
     * Custom license mapping that overrides existent if needed.
     */
    @Input Map<Object, Object> licenses

    /**
     * Aliases for licences that has different names spelling.
     */
    @Input Map<Object, List<Object>> aliases

    /**
     * Generate report for each dependency.
     */
    @Input boolean reportByDependency

    /**
     * Generate report for each license type.
     */
    @Input boolean reportByLicenseType

    /**
     * Include project dependencies in reports.
     */
    @Input boolean includeProjectDependencies

    /**
     * Ignore fatal errors when parsing POMs of transitive dependencies.
     */
    @Input boolean ignoreFatalParseErrors

    /**
     * List of dependencies that will be omitted in the report.
     */
    @Input List<String> excludeDependencies

    /**
     * Output directory for xml reports.
     */
    @OutputDirectory File xmlDestination

    /**
     * Output directory for html reports.
     */
    @OutputDirectory File htmlDestination

    /**
     * Output directory for json reports.
     */
    @OutputDirectory File jsonDestination

    /**
     * File name for reports by dependency.
     */
    @Input String reportByDependencyFileName

    /**
     * File name for reports by license.
     */
    @Input String reportByLicenseFileName

    /**
     * Is xml reports are enabled.
     */
    @Input boolean xml

    /**
     * Is html reports are enabled.
     */
    @Input boolean html

    /**
     * Are json reports enabled.
     */
    @Input boolean json

    /**
     * List of configurations to report on.
     */
    @Input List<String> configurationDependencies

    @TaskAction
    def downloadLicenses() {
        if (!enabled || (!isReportByDependency() && !isReportByLicenseType())
           || (!isXml() && !isHtml() && !isJson())) {
            didWork = false;
            return;
        }

        // Lazy dependency resolving
        def dependencyLicensesSet = {
            def licenseResolver = new LicenseResolver(project: project,
                                                      includeProjectDependencies: getIncludeProjectDependencies(),
                                                      ignoreFatalParseErrors: getIgnoreFatalParseErrors(),
                                                      aliases: aliases.collectEntries {
                                                          new MapEntry(resolveAliasKey(it.key), it.value)
                                                      },
                                                      licenses: getLicenses(),
                                                      dependenciesToIgnore: excludeDependencies,
                    configurationDependencies: configurationDependencies)
            licenseResolver.provideLicenseMap4Dependencies()
        }.memoize()

        // Lazy reporter resolving
        def reporter = { new LicenseReporter(xmlOutputDir: getXmlDestination(), htmlOutputDir: getHtmlDestination(), jsonOutputDir: getJsonDestination()) }

        // Generate report that groups dependencies
        if (isReportByDependency()) {
            if(isHtml()) {
                reporter().generateHTMLReport4DependencyToLicense(
                        dependencyLicensesSet(), getReportByDependencyFileName() + ".html")
            }
            if(isXml()) {
                reporter().generateXMLReport4DependencyToLicense(
                        dependencyLicensesSet(), getReportByDependencyFileName() + ".xml")
            }
            if(isJson()) {
                reporter().generateJSONReport4DependencyToLicense(
                        dependencyLicensesSet(), getReportByDependencyFileName() + ".json")
            }
        }

        // Generate report that groups licenses
        if (isReportByLicenseType()) {
            if(isHtml()) {
                reporter().generateHTMLReport4LicenseToDependency(
                        dependencyLicensesSet(), getReportByLicenseFileName() + ".html")
            }
            if( isXml()) {
                reporter().generateXMLReport4LicenseToDependency(
                        dependencyLicensesSet(), getReportByLicenseFileName() + ".xml")
            }
            if(isJson()) {
                reporter().generateJSONReport4LicenseToDependency(
                        dependencyLicensesSet(), getReportByLicenseFileName() + ".json")
            }
        }
    }

    LicenseMetadata resolveAliasKey(key) {
        if(key instanceof String) {
            license(key)
        } else {
            key
        }
    }
}
