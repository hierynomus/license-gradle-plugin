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

import com.hierynomus.gradle.license.LicenseBasePlugin
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaBasePlugin

class LicensePlugin implements Plugin<Project> {
    protected Task baseCheckTask
    protected Task baseFormatTask
    @Override
    void apply(Project project) {
        project.apply plugin: 'com.github.hierynomus.license-base'
        project.apply plugin: 'com.github.hierynomus.license-report'

        baseCheckTask = project.task(LicenseBasePlugin.LICENSE_TASK_BASE_NAME)
        baseFormatTask = project.task(LicenseBasePlugin.FORMAT_TASK_BASE_NAME)

        baseCheckTask.group = baseFormatTask.group = "License"
        baseCheckTask.description = "Checks for header consistency."
        baseFormatTask.description = "Applies the license found in the header file in files missing the header."

        // Add license checking into check lifecycle, since its a type of code quality plugin

        project.plugins.withType(JavaBasePlugin) {
            linkTasks(project)
        }

        PluginHelper.withAndroidPlugin(project) {
            linkTasks(project)
        }
    }

    private void linkTasks(Project project) {
        project.tasks[JavaBasePlugin.CHECK_TASK_NAME].dependsOn baseCheckTask
        project.tasks.withType(LicenseCheck) { lt ->
            baseCheckTask.dependsOn lt
        }
        project.tasks.withType(LicenseFormat) { lt ->
            baseFormatTask.dependsOn lt
        }
    }
}
