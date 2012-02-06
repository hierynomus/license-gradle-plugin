/* License added by: GRADLE-LICENSE-PLUGIN
 *
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

package nl.javadude.gradle.plugins.license.tasks

import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.gradle.api.Project
import static org.junit.internal.matchers.IsCollectionContaining.hasItem
import static org.junit.Assert.assertThat
import org.hamcrest.core.Is
import static org.hamcrest.core.IsNull.notNullValue
import org.apache.log4j.Logger
import org.gradle.api.GradleException


class LicenseWarnTaskTest {
	private static Logger logger = Logger.getLogger(LicenseWarnTaskTest.class) 

	Project project
	File fooJava
	File licensedJava, otherLicenseJava
	File barProperties
	File bazProperties
	def taskVar

	@Before
	public void setupProject() {
		project = ProjectBuilder.builder().withProjectDir(new File("testProject")).build()
		project.apply plugin: 'license'
		project.apply plugin: 'java'

		fooJava = new File('testProject/src/main/java/Foo.java').absoluteFile
		licensedJava = new File('testProject/src/main/java/Licensed.java').absoluteFile
		
		project.sourceSets {
			main {
				java {
					include "Foo.java", "Licensed.java"
				}
				resources { include "" }
			}
		}
	    printProject project
		
		taskVar = project.tasks.licenseWarn
		taskVar.licenseLines = taskVar.loadLicense()
		
		println taskVar.licenseLines
	} // end test initializer
	
	
	def printProject( def prj ) {
		logger.debug "printProject"
		project.sourceSets.each { set ->
			set.allSource.each { file ->
				logger.debug file
			}
		}
	} // end method
	
	@Test
	public void shouldScanFilesForLicense() {
		def files = taskVar.scanForFiles()
		assertThat(files, hasItem(fooJava))
		assertThat(files, hasItem(licensedJava))
		assertThat(files.size, Is.is(2))
	} // end test

	@Test
	public void shouldBeLicensed() {
		assertThat(taskVar.shouldBeLicensed(fooJava), Is.is(true))
		assertThat(taskVar.shouldBeLicensed(licensedJava), Is.is(false))
	} // end test

	@Test(expected = GradleException.class)
	public void shouldThrowWhenLicenseHeaderNotFound() {
		taskVar.licenseWarn()
	}
	
} // end test class






