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
        project.apply plugin: 'java'
        project.apply plugin: 'license'

        def licenseTask = project.tasks.licenseMain
        //licenseTask.licenseLines = licenseTask.loadLicense()
    }
//
//    @Test
//    public void shouldUnlicenseFile() {
//        def file = new File('testProject/src/main/java/Bar.java')
//        file.createNewFile()
//        try {
//            file.withWriter { writer ->
//                writer.writeLine('import nl.javadude.*;')
//            }
//
//            project.tasks.license.licenseFile(file)
//
//            project.tasks.licenseClean.removeLicense(file)
//            def lines = file.readLines()
//            assertThat lines[0], Is.is('import nl.javadude.*;')
//        } finally {
//            file.delete()
//        }
//    }
//
//    @Test
//    public void shouldUnlicenseFileWithLicenseWithoutSuffix() {
//        def file = new File('testProject/src/main/resources/temp.properties')
//        file.createNewFile()
//        try {
//            file.withWriter { writer ->
//                writer.writeLine('# Temporary file.')
//                writer.writeLine('temp=Hello World!')
//            }
//
//            project.tasks.license.licenseFile(file)
//
//            project.tasks.licenseClean.removeLicense(file)
//            def lines = file.readLines()
//            assertThat lines[0], Is.is('# Temporary file.')
//        } finally {
//            file.delete()
//        }
//    }
//
//    @Test
//    public void shouldForciblyUnlicenseFileWithoutScrewingUp() {
//        def file = new File('testProject/src/main/java/Bar.java')
//        file.createNewFile()
//        try {
//            file.withWriter { writer ->
//                writer.writeLine('/*')
//                writer.writeLine(' * Some license')
//                writer.writeLine(' */')
//                writer.writeLine('public class Bar {}')
//            }
//
//            project.tasks.licenseClean.removeLicense(file)
//            def lines = file.readLines()
//            assertThat lines[0], Is.is('public class Bar {}')
//        } finally {
//            file.delete()
//        }
//    }
}





