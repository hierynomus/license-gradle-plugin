package nl.javadude.gradle.plugins.release.tasks

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.StopExecutionException

class CheckReleaseTask extends ConventionTask {

    @TaskAction
    def checkSnapshotDependencies() {
        def allConfigs = project.configurations.all
        def allDeps = allConfigs.allDependencies.flatten()
        def snapshots = allDeps.findAll {
            it.version.contains('SNAPSHOT')
        }

        snapshots.each {
            printf("Found SNAPSHOT version %s\n", it)
        }

        if (!snapshots.isEmpty()) {
            throw new StopExecutionException("Found snapshot dependencies")
        }
    }
}
