package nl.javadude.gradle.plugins.license.tasks

import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.gradle.api.Project
import static org.junit.internal.matchers.IsCollectionContaining.hasItem
import static org.junit.Assert.assertThat
import org.hamcrest.core.Is
import static org.hamcrest.core.IsNull.notNullValue
import org.gradle.api.GradleException

class LicenseTaskTest {
	Project project
	File fooJava
	File licensedJava
	File barProperties
	File bazProperties

	@Before
	public void setupProject() {
		project = ProjectBuilder.builder().withProjectDir(new File("testProject")).build()
		project.apply plugin: 'license'
		project.apply plugin: 'java'

		fooJava = new File('testProject/src/main/java/Foo.java').absoluteFile
		licensedJava = new File('testProject/src/main/java/Licensed.java').absoluteFile
		barProperties = new File('testProject/src/main/resources/bar.properties').absoluteFile
		bazProperties = new File('testProject/src/main/resources/baz.properties').absoluteFile

		project.tasks.license.init()
	}

	@Test
	public void shouldScanFilesForLicense() {
		def files = project.tasks.license.scanForFiles()
		assertThat(files, hasItem(fooJava))
		assertThat(files, hasItem(barProperties))
		assertThat(files.size, Is.is(4))
	}

	@Test
	public void shouldReadLicense() {
		def license = project.tasks.license.loadLicense()
		assertThat(license, notNullValue())
		assertThat(license, hasItem("This is a sample license"))
		assertThat(license.size, Is.is(2))
	}

	@Test
	public void shouldBeLicensed() {
		assertThat(project.tasks.license.shouldBeLicensed(fooJava), Is.is(true))
		assertThat(project.tasks.license.shouldBeLicensed(licensedJava), Is.is(false))
		assertThat(project.tasks.license.shouldBeLicensed(barProperties), Is.is(true))
		assertThat(project.tasks.license.shouldBeLicensed(bazProperties), Is.is(true))
	}

	@Test(expected = GradleException.class)
	public void shouldThrowWhenLicenseNotFound() {
		project.license = new File("NOTHERE").absoluteFile
		project.tasks.license.init()		
	}
}
