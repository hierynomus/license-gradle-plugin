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

import org.apache.log4j.Logger
import org.gradle.api.GradleException


class LicenseWarnTaskTest {
    private static Logger logger = Logger.getLogger(LicenseWarnTaskTest.class)

    Project project
    File fooJava
    File licensedJava, otherLicenseJava
    File barProperties
    File bazProperties
    def warnTask
/*
    @Before
    public void setupProject() {
        project = ProjectBuilder.builder().withProjectDir(new File("testProject")).build()
        project.apply plugin: 'java'
        project.apply plugin: 'license'

        fooJava = new File('testProject/src/main/java/Foo.java').canonicalFile
        licensedJava = new File('testProject/src/main/java/Licensed.java').canonicalFile

        project.sourceSets {
            main {
                java { include "Foo.java", "Licensed.java" }
                resources { include "" }
            }
        }
        printProject project

        warnTask = project.tasks.licenseWarn
        warnTask.licenseLines = warnTask.loadLicense()
    }


    def printProject( def prj ) {
        logger.debug "printProject"
        project.sourceSets.each { set ->
            set.allSource.each { file -> logger.debug file }
        }
    }

    @Test
    public void shouldScanFilesForLicense() {
        def files = warnTask.scanForFiles()
        assertThat(files, hasItem(fooJava))
        assertThat(files, hasItem(licensedJava))
        assertThat(files.size, Is.is(2))
    }

    @Test
    public void shouldBeLicensed() {
        assertThat(warnTask.shouldBeLicensed(fooJava), Is.is(true))
        assertThat(warnTask.shouldBeLicensed(licensedJava), Is.is(false))
    }

    @Test(expected = GradleException.class)
    public void shouldThrowWhenLicenseHeaderNotFound() {
        warnTask.licenseWarn()
    }
    */
}

