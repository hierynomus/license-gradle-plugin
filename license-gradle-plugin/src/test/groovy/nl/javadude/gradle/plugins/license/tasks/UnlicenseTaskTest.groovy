package nl.javadude.gradle.plugins.license.tasks

import org.junit.Before
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import org.hamcrest.core.Is
import static org.junit.Assert.assertThat

class UnlicenseTaskTest {

    def project

    @Before
    public void setupProject() {
        project = ProjectBuilder.builder().withProjectDir(new File("testProject")).build()
        project.apply plugin: 'license'
        project.apply plugin: 'java'

        def licenseTask = project.tasks.license
        licenseTask.licenseLines = licenseTask.loadLicense()
    }

    @Test
    public void shouldUnlicenseFile() {
        def file = new File('testProject/src/main/java/Bar.java')
        file.createNewFile()
        try {
            file.withWriter { writer ->
                writer.writeLine('import nl.javadude.*;')
            }

            project.tasks.license.licenseFile(file)

            project.tasks.cleanLicense.removeLicense(file)
            def lines = file.readLines()
            assertThat lines[0], Is.is('import nl.javadude.*;')
        } finally {
            file.delete()
        }
    }

    @Test
    public void shouldUnlicenseFileWithLicenseWithoutSuffix() {
        def file = new File('testProject/src/main/resources/temp.properties')
        file.createNewFile()
        try {
            file.withWriter { writer ->
                writer.writeLine('# Temporary file.')
                writer.writeLine('temp=Hello World!')
            }

            project.tasks.license.licenseFile(file)

            project.tasks.cleanLicense.removeLicense(file)
            def lines = file.readLines()
            assertThat lines[0], Is.is('# Temporary file.')
        } finally {
            file.delete()
        }
    }

}
