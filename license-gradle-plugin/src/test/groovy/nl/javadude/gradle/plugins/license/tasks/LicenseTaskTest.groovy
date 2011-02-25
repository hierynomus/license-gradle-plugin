/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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

		def licenseTask = project.tasks.license
		licenseTask.licenseLines = licenseTask.loadLicense()
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
		project.tasks.license.loadLicense()
	}

	@Test
	public void shouldRegisterAdditionalType() {
		project.registerLicense('txt', project.licenseFormat('#'))
		def files = project.tasks.license.scanForFiles()
		assertThat(files, hasItem(new File('testProject/src/main/resources/Other.txt').absoluteFile))
	}

    @Test
    public void shouldAddLicense() {
        def file = new File('testProject/src/main/java/Bar.java')
        file.createNewFile()
        try {
            file.withWriter { writer ->
                writer.writeLine('import nl.javadude.*;')
            }

            project.tasks.license.licenseFile(file)

            def lines = file.readLines()
            assertThat(lines, hasItem(' * This is a sample license'))
        } finally {
            file.delete()
        }
    }
}

