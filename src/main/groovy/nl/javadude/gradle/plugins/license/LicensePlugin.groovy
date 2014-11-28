/* License added by: GRADLE-LICENSE-PLUGIN
 *
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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.ReportingBasePlugin
import org.gradle.api.tasks.SourceSet

class LicensePlugin implements Plugin<Project> {

    private static Logger logger = Logging.getLogger(LicensePlugin);

    static final String DEFAULT_FILE_NAME_FOR_REPORTS_BY_DEPENDENCY = "dependency-license"
    static final String DEFAULT_FILE_NAME_FOR_REPORTS_BY_LICENSE = "license-dependency"
    static final String DEFAULT_DEPENDENCY_CONFIGURATION_TO_HANDLE = "runtime"

    protected Project project
    protected LicenseExtension extension
    protected DownloadLicensesExtension downloadLicensesExtension

    def taskBaseName = 'license'
    def downloadLicenseTaskName = 'downloadLicenses'

    protected Task baseCheckTask
    protected Task baseFormatTask
    protected Task downloadLicenseTask

    void apply(Project project) {
        this.project = project
        project.plugins.apply(ReportingBasePlugin)
        project.plugins.apply(JavaBasePlugin) // First plugin which offers sourceSets

        // Create a single task to run all license checks and reformattings
        baseCheckTask = project.task(taskBaseName)
        baseFormatTask = project.task("${taskBaseName}Format")
        downloadLicenseTask = project.tasks.create(downloadLicenseTaskName, DownloadLicenses)

        extension = createExtension()
        downloadLicensesExtension = createDownloadLicensesExtension()

        configureExtensionRule()
        configureSourceSetRule()
        configureTaskRule()
    }

    protected LicenseExtension createExtension() {
        extension = project.extensions.create(taskBaseName, LicenseExtension)
        extension.with {
            // Default for extension
            header = project.file("LICENSE")
            ignoreFailures = false
            dryRun = false
            skipExistingHeaders = false
            useDefaultMappings = true
            strictCheck = false
        }

        logger.info("Adding license extension");
        return extension
    }

    /**
     * Create and init with defaults downloadLicense extension.
     *
     * @return DownloadLicensesExtension
     */
    protected DownloadLicensesExtension createDownloadLicensesExtension() {
        downloadLicensesExtension = project.extensions.create(downloadLicenseTaskName, DownloadLicensesExtension)

        def html = new LicensesReport(enabled: true, destination: new File("${project.reporting.baseDir.path}/license"))
        def xml = new LicensesReport(enabled: true, destination: new File("${project.reporting.baseDir.path}/license"))

        downloadLicensesExtension.with {
            // Default for extension
            reportByDependency = true
            reportByLicenseType = true
            includeProjectDependencies = false
            reportByDependencyFileName = DEFAULT_FILE_NAME_FOR_REPORTS_BY_DEPENDENCY
            reportByLicenseFileName = DEFAULT_FILE_NAME_FOR_REPORTS_BY_LICENSE
            excludeDependencies = []
            licenses = [:]
            aliases = [:]
            report = new DownloadLicensesReportExtension(html: html, xml: xml)
            dependencyConfiguration = DEFAULT_DEPENDENCY_CONFIGURATION_TO_HANDLE
        }

        logger.info("Adding download licenses extension");
        return downloadLicensesExtension
    }

    /**
     * Establish defaults for extension, which will be all sourceSets
     */
    protected void configureExtensionRule() {
        
        // Reasonable default values, incase the next block doesn't find anything, e.g. lack of 
        extension.conventionMapping.with {
            sourceSets = { [] }
        }

        project.plugins.withType(JavaBasePlugin) {
            // Defaults to use having all the sourceSets
            extension.conventionMapping.sourceSets = { project.sourceSets }
        }
        logger.info("Adding license extension rule");
    }

    /**
     * We'll be creating the tasks by default based on the source sets, but users could define their
     * own, and we'd still want it configured. 
     * TODO: Confirm that user defined tasks will get this configuration, it'd have to be lazily evaluated
     * @param task
     */
    protected void configureTaskRule() {
        project.tasks.withType(License) { License task ->
            logger.info("Applying license defaults to task: ${task.path}");
            configureTaskDefaults(task)
        }
        project.tasks.withType(DownloadLicenses) { DownloadLicenses task ->
            logger.info("Applying defaults to download task: ${task.path}");
            configureTaskDefaults(task)
        }
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
            licenses = { downloadLicensesExtension.licenses }
            aliases = {downloadLicensesExtension.aliases }
            xml = { downloadLicensesExtension.report.xml.enabled }
            html = { downloadLicensesExtension.report.html.enabled }
            excludeDependencies = { downloadLicensesExtension.excludeDependencies }
            xmlDestination = { downloadLicensesExtension.report.xml.destination }
            htmlDestination = { downloadLicensesExtension.report.html.destination }
            dependencyConfiguration = { downloadLicensesExtension.dependencyConfiguration }
        }
    }

    protected void configureTaskDefaults(License task) {
        // Have Task Convention lazily default back to the extension
        task.conventionMapping.with {
            // Defaults for task, which will delegate to project's License extension
            // These can still be explicitly set by the user on the individual tasks
            header = { extension.header }
            ignoreFailures = { extension.ignoreFailures }
            dryRun = { extension.dryRun }
            skipExistingHeaders = { extension.skipExistingHeaders }
            insertNewline = { extension.insertNewline }
            useDefaultMappings = { extension.useDefaultMappings }
            strictCheck = { extension.strictCheck }
            inheritedProperties = { extension.ext.properties }
            inheritedMappings = { extension.internalMappings }
            excludes = { extension.excludePatterns }
            includes = { extension.includePatterns }
        }
    }

    /**
     * Dynamically create a task for each sourceSet, and register with check
     */
    private void configureSourceSetRule() {
        // This follows the other check task pattern
        project.plugins.withType(JavaBasePlugin) {
            extension.sourceSets.all { SourceSet sourceSet ->
                def sourceSetTaskName = sourceSet.getTaskName(taskBaseName, null)
                logger.info("Adding license tasks for sourceSet ${sourceSetTaskName}");

                License checkTask = project.tasks.create(sourceSetTaskName, License)
                checkTask.check = true
                configureForSourceSet(sourceSet, checkTask)
                baseCheckTask.dependsOn checkTask

                // Add independent license task, which will perform format
                def sourceSetFormatTaskName = sourceSet.getTaskName(taskBaseName + 'Format', null)
                License formatTask = project.tasks.create(sourceSetFormatTaskName, License)
                formatTask.check = false
                configureForSourceSet(sourceSet, formatTask)
                baseFormatTask.dependsOn formatTask

                // Add independent clean task to remove headers
                // TODO
            }

            // Add license checking into check lifecycle, since its a type of code quality plugin
            project.tasks[JavaBasePlugin.CHECK_TASK_NAME].dependsOn baseCheckTask

        }
    }

    protected void configureForSourceSet(SourceSet sourceSet, License task) {
        task.with {
            // Explicitly set description
            description = "Scanning license on ${sourceSet.name} files"
        }

        // Default to all source files from SourceSet
        task.source = sourceSet.allSource
    }

}


