package nl.javadude.gradle.plugins.release

import org.gradle.api.Project
import org.gradle.api.Plugin
import nl.javadude.gradle.plugins.release.tasks.CheckReleaseTask

class ReleasePlugin implements Plugin<Project> {
    void apply(Project project) {
        project.convention.plugins.release = new ReleasePluginConvention(project)

        project.tasks.add(name: 'checkRelease', type: CheckReleaseTask.class)
    }
}
