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

import com.google.common.collect.Iterables
import com.google.common.io.Files
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

class LicenseIntegTest {

    Project project
    License licenseTask
    License licenseFormatTask
    File projectDir

    @Before
    public void setupProject() {
        projectDir = Files.createTempDir()
        project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.apply plugin: 'java'
        def plugin = project.plugins.apply(LicensePlugin)
        plugin.configureSourceSetRule()

        project.license.ignoreFailures = true

        licenseTask = project.tasks.licenseMain
        licenseFormatTask = project.tasks.licenseFormatMain
        createLicenseFile()
    }

    @After
    public void cleanupProject() {
        new AntBuilder().delete(dir: projectDir)
    }

    @Test
    public void shouldWorkOnEmptyProject() {
        licenseTask.execute()

        assert licenseTask.altered  != null
        assert licenseTask.altered.size()  == 0
    }

    @Test
    public void shouldFindSingleFile() {
        File propFile = createPropertiesFile()

        licenseTask.execute()

        assert licenseTask.altered != null
        assert licenseTask.altered.size()  == 1
        assert Iterables.get(licenseTask.altered, 0).equals(propFile)
    }

    @Test
    public void shouldOnlyFindMatchingExts() {
        File propFile = createPropertiesFile()
        createTestingFile()

        licenseTask.execute()

        assert licenseTask.altered.size()  == 1
        assert Iterables.get(licenseTask.altered, 0).equals(propFile)
    }

    @Test
    public void canAddMapping() {
        File propFile = createTestingFile()

        project.license.mapping {
            testing='SCRIPT_STYLE'
        }
        // or
        //licenseTask.mapping {
        //    testing='SCRIPT_STYLE'
        //}
        licenseTask.execute()

        assert licenseTask.altered.size()  == 1
        assert Iterables.get(licenseTask.altered, 0).equals(propFile)
    }

    @Test
    public void canAddMappingForDoubleExtension() {
      File freemarker = createFreemarkerShellFile()

      project.license.mapping("sh.ftl",'SCRIPT_STYLE')

      licenseTask.execute()
      assert licenseTask.altered.size() == 1
      assert Iterables.get(licenseTask.altered, 0).equals(freemarker)

    }

    @Test
    public void shouldFindMultipleFiles() {
        createPropertiesFile()
        createJavaFile()
        createTestingFile()

        licenseTask.execute()

        assert licenseTask.altered.size()  == 2
    }

    @Test(expected=RuntimeException.class)
    public void shouldFailWithException() { // Hopefully not StopActionException which gradle will ignore
        project.license.ignoreFailures = false
        File propFile = createPropertiesFile()

        licenseTask.execute()
    }

    @Test
    public void shouldNotFailOnExcludedFileWithMissingHeader_1() {
        project.license.ignoreFailures = false
        project.license.excludes(["**/*.properties"])
        createPropertiesFile()
        licenseTask.execute()
    }

    @Test
    public void shouldNotFailOnExcludedFileWithMissingHeader_2() {
        project.license.ignoreFailures = false
        project.license.exclude "**/*.properties"
        createPropertiesFile()
        licenseTask.execute()
    }

    @Test
    public void shouldNotFailOnFileThatDoesNotFitIncludesPattern_1() {
        project.license.ignoreFailures = false
        licenseTask.ext.year = 2012
        project.license.include "**/header.properties"
        // Should be included
        createPropertiesFileWithHeader()
        // Should be ignored
        createPropertiesFile()
        licenseTask.execute()
    }

    @Test
    public void shouldNotFailOnFileThatDoesNotFitIncludesPattern_2() {
        project.license.ignoreFailures = false
        licenseTask.ext.year = 2012
        project.license.includes(["**/header.properties"])
        // Should be included
        createPropertiesFileWithHeader()
        // Should be ignored
        createPropertiesFile()
        licenseTask.execute()
    }

    @Test
    public void shouldCorrectlyMixIncludesAndExcludes() {
        project.license.ignoreFailures = false
        licenseTask.ext.year = 2012
        project.license.includes(["**/*.properties"])
        project.license.excludes(["**/test.properties"])
        // Should be included
        createPropertiesFileWithHeader()
        // Should be ignored (not matching include)
        createJavaFile()
        // Should be ignored (matching include and exclude)
        createPropertiesFile()
        licenseTask.execute()
    }

    @Test
    public void canAddHeader() {
        File propFile = createPropertiesFile()

        licenseFormatTask.execute()

        assert licenseFormatTask.altered.size()  == 1
        assert Iterables.get(licenseFormatTask.altered, 0).equals(propFile)
        assert propFile.getText().equals('''#
# This is a sample license created in ${year}
#

key1 = value1
key2 = value2
''');
    }

    @Test
    public void canAddJavaHeader() {
        File javaFile = createJavaFile()

        licenseFormatTask.execute()

        assert licenseFormatTask.altered.size()  == 1
        assert Iterables.get(licenseFormatTask.altered, 0).equals(javaFile)
        def expected = '''/**
 * This is a sample license created in ${year}
 */
public class Test {
        static { System.out.println("Hello") }
}
'''
        assert javaFile.getText().equals(expected);
    }

    @Test
    public void canAddHeaderWithVariable() {
        File propFile = createPropertiesFile()

        project.license.ext.year = 2012
        //licenseFormatTask.ext.year = 2012
        licenseFormatTask.execute()

        assert licenseFormatTask.altered.size()  == 1
        assert Iterables.get(licenseFormatTask.altered, 0).equals(propFile)
        assert propFile.getText().equals('''#
# This is a sample license created in 2012
#

key1 = value1
key2 = value2
''');
    }

    @Test
    public void shouldIgnoreExistingHeader() {
        createPropertiesFileWithHeader()

        licenseTask.ext.year = 2012
        licenseTask.execute()

        assert licenseTask.altered.size()  == 0
    }

    @Test
    public void shouldDetectMissingHeaderIfCheckIsTrueAndSkipExistingHeadersIsTrue() {
        createPropertiesFile()

        licenseTask.check = true
        licenseTask.skipExistingHeaders = true
        licenseTask.execute()

        assert licenseTask.altered.size() == 1
    }

    public File createLicenseFile() {
        File file = project.file("LICENSE")
        Files.createParentDirs(file);
        file << '''This is a sample license created in ${year}
'''
        file
    }

    public File createJavaFile() {
        File file = project.file("src/main/java/Test.java")
        Files.createParentDirs(file);
        file << '''public class Test {
        static { System.out.println("Hello") }
}
'''
    }

    public File createTestingFile() {
        File file = project.file("src/main/resources/prop.testing")
        Files.createParentDirs(file);
        file << '''keyA = valueB
keyB = valueB
'''
    }

    public File createFreemarkerShellFile() {
        File file = project.file("src/main/resources/prop.sh.ftl")
        Files.createParentDirs(file);
        file << '''#!/bin/bash
echo "Hello world!"
'''
    }

    public File createPropertiesFile() {
        File file = project.file("src/main/resources/test.properties")
        Files.createParentDirs(file);
        file << '''key1 = value1
key2 = value2
'''
    }

    public File createPropertiesFileWithHeader() {
        File file = project.file("src/main/resources/header.properties")
        Files.createParentDirs(file);
        file << '''# This is a sample license created in 2012
key3 = value3
key4 = value4
'''
        file
    }

}

