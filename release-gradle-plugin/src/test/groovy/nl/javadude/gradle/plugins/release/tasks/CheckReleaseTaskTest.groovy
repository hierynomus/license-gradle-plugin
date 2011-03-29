package nl.javadude.gradle.plugins.release.tasks

import org.junit.Before
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import org.junit.Test
import org.gradle.api.tasks.StopExecutionException

class CheckReleaseTaskTest {

    Project project

    @Before
    public void setupProject() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'release'
        project.apply plugin: 'java'
    }

    @Test(expected = StopExecutionException.class)
    public void shouldDetectSnapshotDependency() {
        project.dependencies {
            compile 'com.bar:foo:1.0'
            compile 'com.foo:bar:1.0-SNAPSHOT'
        }

        project.tasks.checkRelease.checkSnapshotDependencies()
    }

    @Test
    public void shouldFindNoSnapshotDeps() {
        project.dependencies {
            compile 'com.bar:foo:1.0'
        }

        project.tasks.checkRelease.checkSnapshotDependencies()
    }

}
