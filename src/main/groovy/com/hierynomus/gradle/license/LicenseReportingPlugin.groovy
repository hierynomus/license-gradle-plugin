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
package com.hierynomus.gradle.license

import nl.javadude.gradle.plugins.license.DownloadLicenses
import nl.javadude.gradle.plugins.license.DownloadLicensesExtension
import nl.javadude.gradle.plugins.license.DownloadLicensesReportExtension
import nl.javadude.gradle.plugins.license.LicensesReport
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ReportingBasePlugin

class LicenseReportingPlugin implements Plugin<Project> {
    static final String DOWNLOAD_LICENSES_TASK_NAME = 'downloadLicenses'
    static final String DEFAULT_FILE_NAME_FOR_REPORTS_BY_DEPENDENCY = "dependency-license"
    static final String DEFAULT_FILE_NAME_FOR_REPORTS_BY_LICENSE = "license-dependency"

    protected DownloadLicensesExtension downloadLicensesExtension

    protected Task downloadLicenseTask
    private Project project

    @Override
    void apply(Project project) {
        this.project = project
        project.plugins.apply(ReportingBasePlugin)
        // Create a single task to run all license checks and reformattings
        downloadLicenseTask = project.tasks.create(DOWNLOAD_LICENSES_TASK_NAME, DownloadLicenses)

        downloadLicenseTask.group = "License"
        downloadLicenseTask.description = "Generates reports on your runtime dependencies."
        downloadLicensesExtension = createDownloadLicensesExtension()


        project.tasks.withType(DownloadLicenses) { DownloadLicenses task ->
            project.logger.info("Applying defaults to download task: ${task.path}");
            configureTaskDefaults(task)
        }
    }

    /**
     * Create and init with defaults downloadLicense extension.
     *
     * @return DownloadLicensesExtension
     */
    protected DownloadLicensesExtension createDownloadLicensesExtension() {
        downloadLicensesExtension = project.extensions.create(DOWNLOAD_LICENSES_TASK_NAME, DownloadLicensesExtension)

        def html = new LicensesReport(enabled: true, destination: { -> "${project.reporting.baseDir.path}/license" })
        def xml = new LicensesReport(enabled: true, destination: { -> "${project.reporting.baseDir.path}/license" })
        def json = new LicensesReport(enabled: true, destination: { -> "${project.reporting.baseDir.path}/license" })

        downloadLicensesExtension.with {
            // Default for extension
            reportByDependency = true
            reportByLicenseType = true
            includeProjectDependencies = false
            ignoreFatalParseErrors = false
            reportByDependencyFileName = DEFAULT_FILE_NAME_FOR_REPORTS_BY_DEPENDENCY
            reportByLicenseFileName = DEFAULT_FILE_NAME_FOR_REPORTS_BY_LICENSE
            excludeDependencies = []
            licenses = [:]
            aliases = [:]
            report = new DownloadLicensesReportExtension(html: html, xml: xml, json: json)
            configurationDependencies = [LicenseBasePlugin.DEFAULT_DEPENDENCY_CONFIGURATION_TO_HANDLE]
        }

        project.logger.info("Adding download licenses extension");
        return downloadLicensesExtension
    }


    /**
     * Configure convention mapping.
     *
     * @param task download license task
     */
    protected void configureTaskDefaults(DownloadLicenses task) {
        task.conventionMapping.with {
            reportByDependency = { downloadLicensesExtension.reportByDependency }
            reportByLicenseType = { downloadLicensesExtension.reportByLicenseType }
            reportByDependencyFileName = { downloadLicensesExtension.reportByDependencyFileName }
            reportByLicenseFileName = { downloadLicensesExtension.reportByLicenseFileName }
            includeProjectDependencies = {downloadLicensesExtension.includeProjectDependencies}
            ignoreFatalParseErrors = {downloadLicensesExtension.ignoreFatalParseErrors}
            licenses = { downloadLicensesExtension.licenses }
            aliases = {downloadLicensesExtension.aliases }
            xml = { downloadLicensesExtension.report.xml.enabled }
            html = { downloadLicensesExtension.report.html.enabled }
            json = { downloadLicensesExtension.report.json.enabled }
            excludeDependencies = { downloadLicensesExtension.excludeDependencies }
            xmlDestination = { new File("${downloadLicensesExtension.report.xml.destination}") }
            htmlDestination = { new File("${downloadLicensesExtension.report.html.destination}") }
            jsonDestination = { new File("${downloadLicensesExtension.report.json.destination}") }
            configurationDependencies = { downloadLicensesExtension.configurationDependencies }
        }
    }


}
