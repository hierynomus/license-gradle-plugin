package nl.javadude.gradle.plugins.license

import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import nl.javadude.gradle.plugins.license.tasks.LicenseTask
import static org.hamcrest.CoreMatchers.instanceOf
import static org.junit.Assert.assertThat
import org.hamcrest.core.Is
import org.junit.Before

class LicensePluginTest {
	Project project

	@Before
	public void setupProject() {
		project = ProjectBuilder.builder().withProjectDir(new File("testProject")).build()
		project.apply plugin: 'license'
		project.apply plugin: 'java'
		project.sourceSets.main.java.srcDirs = ['src/main/java']
	}

	@Test
	public void shouldAddLicenseTask() {
		assertThat(project.tasks.license, instanceOf(LicenseTask.class))
	}

	@Test
	public void shouldPointToLicenseFileByDefault() {
		assertThat(project.convention.plugins.license.license.name, Is.is("LICENSE"))
	}

	@Test
	public void shouldBeAbleToConfigureLicenseToOtherFile() {
		project.license = new File(project.projectDir, "OTHERLICENSE")
		assertThat(project.convention.plugins.license.license.name, Is.is("OTHERLICENSE"))
	}

	@Test
	public void shouldScanFilesForLicense() {
		project.tasks.license.process()
	}
}
