package nl.javadude.gradle.plugins.license

import nebula.test.IntegrationSpec
import org.gradle.testkit.runner.GradleRunner

class DownloadLicensesTestKitSpec extends IntegrationSpec {
    def "Should correctly take project.buildDir into account for generated reports"() {
        given:
        buildFile << """
apply plugin: 'com.github.hierynomus.license'
apply plugin: 'java'
buildDir = "generated"

dependencies {
    compile 'com.google.guava:guava:14.0'
}

repositories { mavenCentral() }
"""
        when:
        runTasksSuccessfully('downloadLic')

        then:
        new File(projectDir, "generated/reports").exists()
        !new File(projectDir, "build").exists()
    }
}
