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
import com.hierynomus.gradle.license.LicenseReportingPlugin;
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskProvider

class LicensePlugin implements Plugin<Project> {
    private static final TASK_GROUP = "License"

    protected TaskProvider baseCheckTask
    protected TaskProvider baseFormatTask
    @Override
    void apply(Project project) {
        project.apply plugin: LicenseBasePlugin
        project.apply plugin: LicenseReportingPlugin

        baseCheckTask = project.tasks.register(LicenseBasePlugin.LICENSE_TASK_BASE_NAME) { task ->
            task.group = TASK_GROUP
            task.description = "Checks for header consistency."
        }
        baseFormatTask = project.tasks.register(LicenseBasePlugin.FORMAT_TASK_BASE_NAME) { task ->
            task.group = TASK_GROUP
            task.description = "Applies the license found in the header file in files missing the header."
        }

        // Add license checking into check lifecycle, since its a type of code quality plugin

        project.plugins.withType(JavaBasePlugin) {
            linkTasks(project)
        }

        PluginHelper.withAndroidPlugin(project) {
            linkTasks(project)
        }
    }

    private void linkTasks(Project project) {
        project.tasks.named(JavaBasePlugin.CHECK_TASK_NAME).configure { task ->
            task.dependsOn baseCheckTask
        }
        baseCheckTask.configure { task ->
            // Tasks are eagerly resolved here since running the base check task is expected to run
            // all the LicenseCheck tasks
            task.dependsOn project.tasks.withType(LicenseCheck)
        }
        baseFormatTask.configure { task ->
            // Tasks are eagerly resolved here since running the base format task is expected to
            // run all the LicenseFormat tasks
            task.dependsOn project.tasks.withType(LicenseFormat)
        }
    }
}
