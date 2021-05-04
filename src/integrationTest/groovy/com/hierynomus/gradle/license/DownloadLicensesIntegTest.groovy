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
package com.hierynomus.gradle.license

import com.hierynomus.gradle.license.LicenseReportingPlugin
import groovy.util.slurpersupport.GPathResult
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Shared
import spock.lang.Specification

import static nl.javadude.gradle.plugins.license.DownloadLicensesExtension.group
import static nl.javadude.gradle.plugins.license.DownloadLicensesExtension.license

/**
 * Integration test for {@link DownloadLicenses}.
 */
class DownloadLicensesIntegTest extends Specification {

    static final String LICENSE_REPORT = "licenseReport"

    @Shared def downloadLicenses
    @Shared Project project
    @Shared Project subproject
    @Shared File projectDir = new File("rootPrj")
    @Shared File subProjectDir = new File(projectDir, "subproject1")
    @Shared File outputDir = new File(LICENSE_REPORT)
    @Shared AntBuilder ant = new AntBuilder()

    def setup() {
        configureProjects()

        project.apply plugin: 'java'
        project.apply plugin: 'license'

        project.group = "testGroup"
        project.version = "1.5"

        project.repositories {
            mavenCentral()
        }

        subproject.apply plugin: 'java'
        subproject.apply plugin: 'license'

        subproject.group = "testGroup"
        subproject.version = "1.7"

        subproject.repositories {
            mavenCentral()
        }

        configurePlugin()
    }

    def cleanup() {
        ant.delete(dir: outputDir)
        ant.delete(dir: subProjectDir)
        ant.delete(dir: projectDir)
    }

    def "Test correctness of defaults"() {
        expect:
        downloadLicenses.reportByLicenseType
        downloadLicenses.reportByDependency
        downloadLicenses.enabled
    }

    def configureProjects() {
        projectDir.mkdir()
        subProjectDir.mkdir()

        project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()

        subproject = ProjectBuilder.builder()
                .withParent(project)
                .withName("subproject1")
                .withProjectDir(subProjectDir)
                .build()

        File dependencyJar = new File(projectDir, "testDependency.jar")
        dependencyJar.createNewFile()
    }

    def configurePlugin() {
        downloadLicenses = project.tasks.downloadLicenses
        project.downloadLicenses {
            licenses = ["testDependency.jar": license("Apache 2")]
        }
        project.downloadLicenses.report {
            xml.enabled = true
            html.enabled = false
            json.enabled = true
            xml.destination = this.outputDir
            html.destination = this.outputDir
            json.destination = this.outputDir
        }
    }

    def xml4DependencyByLicenseReport(File reportDir) {
        File reportByLicense = new File(reportDir, LicenseReportingPlugin.DEFAULT_FILE_NAME_FOR_REPORTS_BY_LICENSE + ".xml")
        new XmlSlurper().parse(reportByLicense)
    }

    def xml4LicenseByDependencyReport(File reportDir) {
        File reportByDependency = new File(reportDir, LicenseReportingPlugin.DEFAULT_FILE_NAME_FOR_REPORTS_BY_DEPENDENCY + ".xml")
        new XmlSlurper().parse(reportByDependency)
    }

    def getLicenseReportFolder() {
        new File(LICENSE_REPORT)
    }

    def dependencyWithLicensePresent(GPathResult xmlByDependency, String d, String jar, String l) {
        xmlByDependency.dependency.find {
            it.@name.text() == d && it.file.text() == jar
        }.license.any {
            it.@name == l
        }
    }

    def dependencyWithLicenseUrlPresent(GPathResult xmlByDependency, String d, String lUrl) {
        xmlByDependency.dependency.find {
            it.@name.text() == d
        }.license.any {
            it.@url == lUrl
        }
    }

    def assertLicenseReportsExist(File f) {
        f.exists()
        f.listFiles().length == 4
    }

    def dependenciesInReport(GPathResult xmlByDependency) {
        xmlByDependency.dependency.size()
    }

    def licensesInReport(GPathResult xmlByLicense) {
        xmlByLicense.license.size()
    }
}
