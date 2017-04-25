/*
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

import nebula.test.IntegrationSpec
import org.gradle.testkit.runner.GradleRunner

class DownloadLicensesTestKitSpec extends IntegrationSpec {
    def "Should correctly take project.buildDir into account for generated reports"() {
        given:
        buildFile << """
apply plugin: 'com.github.hierynomus.license'
apply plugin: 'java'
buildDir = "generated"

dependencies {
    compile 'com.google.guava:guava:14.0'
}

repositories { mavenCentral() }
"""
        when:
        runTasksSuccessfully('downloadLic')

        then:
        new File(projectDir, "generated/reports").exists()
        !new File(projectDir, "build").exists()
    }
}
