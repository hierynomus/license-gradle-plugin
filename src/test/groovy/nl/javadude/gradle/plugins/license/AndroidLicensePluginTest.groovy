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

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.hierynomus.gradle.license.LicenseBasePlugin
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertThat

@RunWith(Parameterized.class)
class AndroidLicensePluginTest {
    Project project
    String pluginName
    Class pluginClass

    @Parameterized.Parameters(name =  "Plugin {0}")
    public static Collection<Object[]> pluginTypes() {
        Object[][] params = [
            [ 'com.android.application', AppPlugin ],
            [ 'com.android.library', LibraryPlugin ]
        ];
        return Arrays.asList(params);
    }

    public AndroidLicensePluginTest(pluginName, pluginClass) {
        this.pluginName = pluginName
        this.pluginClass = pluginClass
    }

    @Before
    public void setupProject() {
        project = ProjectBuilder.builder().withProjectDir(new File("testProject")).build()
        def plugin = project.plugins.apply(LicenseBasePlugin)
        project.apply plugin: pluginName

        // Otherwise we'd need a project.evaluate() which would trigger Android SDK detection
//        plugin.configureSourceSetRule(pluginClass, "Android", { ss -> ss.java.sourceFiles + ss.res.sourceFiles })
    }

    @Test
    public void shouldAddLicenseTask() {
        def tasks = project.tasks.withType(License.class).findAll { true }
        print(tasks)
        assertFalse tasks.isEmpty()
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
    public void shouldBeAbleToConfigureLicenseToOtherFile() {
        project.license.header = project.file("OTHERLICENSE")
        assertThat(project.license.header.name, is("OTHERLICENSE"))
    }


    @Test
    public void shouldFindTwoLicenseTaskPerSourceSet() {
        def tasks = project.tasks.withType(License.class).findAll { true }
        assertThat tasks.size(), is(14) // [androidTest, debug, main, release, test, testDebug, testRelease].count * 2
    }

    @Test
    public void shouldFindMainLicenseTask() {
        def task = project.tasks.getByName("licenseAndroidMain")
        assertThat task, instanceOf(License.class)
    }

    @Test
    public void shouldFindDebugLicenseTask() {
        def task = project.tasks.getByName("licenseAndroidDebug")
        assertThat task, instanceOf(License.class)
    }

    @Test
    public void shouldFindReleaseLicenseTask() {
        def task = project.tasks.getByName("licenseAndroidRelease")
        assertThat task, instanceOf(License.class)
    }

    @Test
    public void shouldFindTestLicenseTask() {
        def task = project.tasks.getByName("licenseAndroidAndroidTest")
        assertThat task, instanceOf(License.class)
    }

    @Test
    public void extensionShouldHaveSourceSetsWithJava() {
        assertThat project.license.sourceSets.size(), equalTo(project.sourceSets.size())
    }

    @Test
    public void shouldConfigureLicenseForTasks() {
        def task = project.tasks['licenseAndroidMain']

        assertThat task.header.name, is("LICENSE")
    }

    @Test
    public void shouldConfigureManuallyConfiguredTask() {
        def task = project.tasks.create('licenseManual', LicenseCheck)

        assertThat task.header.name, is("LICENSE")
    }

    @Test
    public void manualTaskShouldInheritFromExtension() {
        def task = project.tasks.create('licenseManual', LicenseCheck)

        assertThat project.license.ignoreFailures, is(false) // Default
        assertThat task.ignoreFailures, is(false)

        project.license.ignoreFailures = true
        assertThat task.isIgnoreFailures(), is(true)
        //assertThat task.getIgnoreFailures(), is(true) // GRADLE-2163, fixed in 1.0-rc1
        assertThat task.ignoreFailures, is(true)
    }

    @Test
    public void shouldRunLicenseDuringCheck() {
        project.apply plugin: 'com.github.hierynomus.license'
        def task = project.tasks.create('licenseManual', LicenseCheck)

        Set<Task> dependsOn = project.tasks['check'].getDependsOn()
        assertThat dependsOn, hasItem(project.tasks['license'])

        // Manual tests don't get registered with check
        assertThat dependsOn, not(hasItem(task))
    }

    @Test
    public void shouldRunLicenseFromBaseTasks() {
        project.apply plugin: "com.github.hierynomus.license"
        def manual = project.tasks.create('licenseManualCheck', LicenseCheck)

        def manualFormat = project.tasks.create('licenseManualFormat', LicenseFormat)

        Set<Task> dependsOn = project.tasks['license'].getDependsOn()
        assertThat dependsOn, hasItem(project.tasks['licenseAndroidMain'])
        assertThat dependsOn, hasItem(project.tasks['licenseAndroidAndroidTest'])

        // Manual tests also get registered with check.
        assertThat dependsOn, hasItem(manual)
        assertThat dependsOn, not(hasItem(manualFormat))

        Set<Task> dependsOnFormat = project.tasks['licenseFormat'].getDependsOn()
        assertThat dependsOnFormat, hasItem(project.tasks['licenseFormatAndroidMain'])
        assertThat dependsOnFormat, hasItem(project.tasks['licenseFormatAndroidAndroidTest'])

        // Manual test does not get registered with format, as it does not have the correct name.
        assertThat dependsOnFormat, not(hasItem(manual))
        assertThat dependsOnFormat, hasItem(manualFormat)
    }

    @Test
    public void canAddMappingsAtMultipleLevels() {
        def task = project.tasks['licenseAndroidMain']
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
