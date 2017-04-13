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
