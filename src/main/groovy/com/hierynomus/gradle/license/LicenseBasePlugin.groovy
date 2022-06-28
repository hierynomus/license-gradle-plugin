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

package com.hierynomus.gradle.license

import nl.javadude.gradle.plugins.license.License
import nl.javadude.gradle.plugins.license.LicenseExtension
import nl.javadude.gradle.plugins.license.PluginHelper
import nl.javadude.gradle.plugins.license.header.HeaderDefinitionBuilder
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSet

class LicenseBasePlugin implements Plugin<Project> {

    private static Logger logger = Logging.getLogger(LicenseBasePlugin);

    static final String DEFAULT_DEPENDENCY_CONFIGURATION_TO_HANDLE = "runtime"

    static final String LICENSE_TASK_BASE_NAME = 'license'
    static final String FORMAT_TASK_BASE_NAME = 'licenseFormat'

    protected Project project
    protected LicenseExtension extension

    void apply(Project project) {
        this.project = project

        extension = createExtension()

        project.plugins.with {
            withType(JavaBasePlugin) {
                configureJava()
            }

            PluginHelper.withAndroidPlugin(project) { pc ->
                configureAndroid()
            }

//            ['com.android.build.gradle.AppPlugin', 'com.android.build.gradle.LibraryPlugin'].each { c ->
//                PluginHelper.withOptionalPlugin(c, project) {
//                    configureAndroid(Class.forName(c))
//                }
//            }
        }

        configureTaskRule()
    }

    protected LicenseExtension createExtension() {
        extension = project.extensions.create(LICENSE_TASK_BASE_NAME, LicenseExtension)
        extension.with {
            // Default for extension
            header = project.file("LICENSE")
            headerURI = null
            ignoreFailures = false
            dryRun = false
            skipExistingHeaders = false
            useDefaultMappings = true
            keywords = ['copyright']
            strictCheck = false
            encoding = System.properties['file.encoding']
            sourceSets = project.container(SourceSet)
//            conventionMapping.with {
//                sourceSets = { [] as DomainObjectCollection<SourceSet> }
//            }
            headerDefinitions = project.container(HeaderDefinitionBuilder)
        }

        logger.info("Adding license extension");
        return extension
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
    }

    protected void configureTaskDefaults(License task) {
        // Have Task Convention lazily default back to the extension
        task.conventionMapping.with {
            // Defaults for task, which will delegate to project's License extension
            // These can still be explicitly set by the user on the individual tasks
            header = { extension.header }
            headerURI = { extension.headerURI }
            ignoreFailures = { extension.ignoreFailures }
            dryRun = { extension.dryRun }
            skipExistingHeaders = { extension.skipExistingHeaders }
            useDefaultMappings = { extension.useDefaultMappings }
            keywords = { extension.keywords }
            strictCheck = { extension.strictCheck }
            inheritedProperties = { extension.ext.properties }
            inheritedMappings = { extension.internalMappings }
            excludes = { extension.excludePatterns }
            includes = { extension.includePatterns }
            encoding = { extension.encoding }
            headerDefinitions = { extension.headerDefinitions }
        }
    }

    private void configureJava() {
        configureSourceSetRule(project.sourceSets, "", { ss -> ss.allSource })
    }

    private void configureAndroid() {
        configureSourceSetRule(project.android.sourceSets, "Android", { ss -> ss.java.sourceFiles + ss.res.sourceFiles })
    }

    /**
     * Dynamically create a task for each sourceSet, and register with check
     */
    private void configureSourceSetRule(sourceSetContainer, String taskInfix, Closure<Iterable<File>> sourceSetSources) {
        // This follows the other check task pattern
        sourceSetContainer.all { sourceSet ->
            def sourceSetTaskName = "${LICENSE_TASK_BASE_NAME}${taskInfix}${sourceSet.name.capitalize()}"
            logger.info("Adding ${sourceSetTaskName} task for sourceSet ${sourceSet.name}");

            License checkTask = project.tasks.create(sourceSetTaskName, LicenseCheck)
            configureForSourceSet(sourceSet, checkTask, sourceSetSources)

            // Add independent license task, which will perform format
            def sourceSetFormatTaskName = "${FORMAT_TASK_BASE_NAME}${taskInfix}${sourceSet.name.capitalize()}"
            License formatTask = project.tasks.create(sourceSetFormatTaskName, LicenseFormat)
            configureForSourceSet(sourceSet, formatTask, sourceSetSources)

            // Add independent clean task to remove headers
            // TODO
        }
    }

    protected void configureForSourceSet(sourceSet, License task, Closure<Iterable<File>> sourceSetSources) {
        task.with {
            // Explicitly set description
            description = "Scanning license on ${sourceSet.name} files"
        }

        // Default to all source files from SourceSet
        task.source = sourceSetSources(sourceSet)
    }
}


