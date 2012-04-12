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

package nl.javadude.gradle.plugins.license

import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import org.gradle.api.Task
import nl.javadude.gradle.plugins.license.License
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue
import static org.hamcrest.CoreMatchers.*
import static org.junit.matchers.JUnitMatchers.*
import org.junit.Before

class LicensePluginTest {
    Project project

    @Before
    public void setupProject() {
        project = ProjectBuilder.builder().withProjectDir(new File("testProject")).build()
        project.apply plugin: 'license'
    }

    @Test
    public void shouldAddLicenseTask() {
        def tasks = project.tasks.withType(License.class).findAll { true }
        assertTrue tasks.isEmpty()
    }

    @Test
    public void shouldFindTwoLicenseTaskPerSourceSet() {
        project.apply plugin: 'java'
        def tasks = project.tasks.withType(License.class).findAll { true }
        assertThat tasks.size(), is(4) // [Main,Test].count * 2
    }

    @Test
    public void shouldFindMainLicenseTask() {
        project.apply plugin: 'java'
        def task = project.tasks.getByName("licenseMain")
        assertThat task, instanceOf(License.class)
    }

    @Test
    public void shouldPointToLicenseFileByDefault() {
        assertThat(project.license.header.name, is("LICENSE"))
    }

    @Test
    public void extensionShouldHaveBooleanDocumentedDefaults() {
        assertThat project.license.ignoreFailures, is(false)
        assertThat project.license.dryRun, is(false)
        assertThat project.license.skipExistingHeaders, is(false)
        assertThat project.license.useDefaultMappings, is(true)
        assertThat project.license.strictCheck, is(false)
    }

    @Test
    public void extensionShouldNotHaveSourceSets() {
        assertThat project.license.sourceSets, is(notNullValue())
        assertThat project.license.sourceSets.size(), equalTo(0)
    }

    @Test
    public void extensionShouldHaveSourceSetsWithJava() {
        project.apply plugin: 'java'
        assertThat project.license.sourceSets.size(), equalTo(project.sourceSets.size())
    }

    @Test
    public void shouldBeAbleToConfigureLicenseToOtherFile() {
        project.license.header = project.file("OTHERLICENSE")
        assertThat(project.license.header.name, is("OTHERLICENSE"))
    }
    
    @Test
    public void shouldConfigureLicenseForTasks() {
        project.apply plugin: 'java'
        def task = project.tasks['licenseMain']
        
        assertThat task.header.name, is("LICENSE")
    }

    @Test
    public void shouldConfigureManuallyConfiguredTask() {
        project.apply plugin: 'java'
        def task = project.tasks.add('licenseManual', License)
        
        assertThat task.header.name, is("LICENSE")
    }

    @Test
    public void manualTaskShouldInheritFromExtension() {
        project.apply plugin: 'java'
        def task = project.tasks.add('licenseManual', License)

        assertThat project.license.ignoreFailures, is(false) // Default
        assertThat task.ignoreFailures, is(false)
        
        project.license.ignoreFailures = true
        assertThat task.isIgnoreFailures(), is(true)
        //assertThat task.getIgnoreFailures(), is(true) // GRADLE-2163, fixed in 1.0-rc1
        assertThat task.ignoreFailures, is(true)
    }

    @Test
    public void shouldRunLicenseDuringCheck() {
        project.apply plugin: 'java'
        def task = project.tasks.add('licenseManual', License)

        Set<Task> dependsOn = project.tasks['check'].getDependsOn()
        assertThat dependsOn, hasItem(project.tasks['licenseMain'])
        assertThat dependsOn, hasItem(project.tasks['licenseTest'])
        
        // Manual tests don't get registered with check
        assertThat dependsOn, not(hasItem(task))
    }
    
    @Test
    public void canAddMappingsAtMultipleLevels() {
        project.apply plugin: 'java'

        def task = project.tasks['licenseMain']
        project.license.mapping {
            map1='VALUE1'
        }
        task.mapping {
            map2='VALUE2'
        }

        def mappings = task.combinedMappings()
        assert mappings.containsKey('map1')
        assert mappings.containsKey('map2')
    }
}

