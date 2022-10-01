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

import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import nebula.test.IntegrationSpec
import spock.lang.Unroll

class LicenseReportIntegrationTest extends IntegrationSpec {

    File outputDir
    File subProject
    File subProjectBuildFile

    def setup() {
        buildFile << """
plugins {
    id "java"
}

apply plugin: "com.github.hierynomus.license-report"

group = "testGroup"
version = "1.5"

repositories {
    mavenCentral()
}

downloadLicenses {
    licenses = ["testDependency.jar": license("Apache 2")]
    report {
        xml.enabled = true
        html.enabled = false
        json.enabled = true
    }
    dependencyConfiguration = "runtimeClasspath"
}
"""
        subProject = addSubproject("subproject")
        subProjectBuildFile = new File(subProject, "build.gradle")
        subProjectBuildFile << """
plugins {
    id "java"
}

group = "testSubGroup"
version = "1.7"

repositories {
    mavenCentral()
}
"""
        outputDir = directory("build/reports/license")
    }

    def "should handle poms with xlint args"() {
        given:
        buildFile << """
dependencies {
    implementation "com.sun.mail:javax.mail:1.5.4"
}
"""
        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        outputDir.listFiles().length == 4
        dependenciesInReport(xml4LicenseByDependencyReport()) == 2
        licensesInReport(xml4DependencyByLicenseReport()) == 2
    }

    def "should ignore fatal pom parse errors"() {
        given:
        buildFile << """
dependencies {
    // This depends on "org.codehouse.plexus:plexus:1.0.4" whose POM is malformed.
    implementation "org.apache.maven:maven-ant-tasks:2.1.3"
}

downloadLicenses.ignoreFatalParseErrors = true
"""

        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        dependenciesInReport(xml4LicenseByDependencyReport()) == 24
    }

    def "should report on dependencies in subprojects when in multimodule build"() {
        given:
        subProjectBuildFile << """
dependencies {
    implementation "org.jboss.logging:jboss-logging:3.1.3.GA"
    implementation "com.google.guava:guava:15.0"
 }
 """
        buildFile << """
downloadLicenses {
    licenses = [
        "com.google.guava:guava:15.0": license("MY_LICENSE", "MY_URL"),
        "org.jboss.logging:jboss-logging:3.1.3.GA": license("MY_LICENSE", "MY_URL")
    ]
}
"""
        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        def xmlByDependency = xml4LicenseByDependencyReport()
        def xmlByLicense = xml4DependencyByLicenseReport()

        dependenciesInReport(xmlByDependency) == 2
        licensesInReport(xmlByLicense) == 1

        dependencyWithLicensePresent(xmlByDependency, "org.jboss.logging:jboss-logging:3.1.3.GA", "jboss-logging-3.1.3.GA.jar", "MY_LICENSE")
        dependencyWithLicensePresent(xmlByDependency, "com.google.guava:guava:15.0", "guava-15.0.jar", "MY_LICENSE")

        dependencyWithLicenseUrlPresent(xmlByDependency, "org.jboss.logging:jboss-logging:3.1.3.GA", "MY_URL")
        dependencyWithLicenseUrlPresent(xmlByDependency, "com.google.guava:guava:15.0", "MY_URL")
    }

    def "should report project dependency if license specified"() {
        given:
        buildFile << """
dependencies {
    implementation project(":subproject")
}

downloadLicenses.licenses = [
    "testSubGroup:subproject:1.7" : "SbPrj license"
]
downloadLicenses.includeProjectDependencies = true
"""
        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        def xmlByDependency = xml4LicenseByDependencyReport()
        def xmlByLicense = xml4DependencyByLicenseReport()

        dependenciesInReport(xmlByDependency) == 1
        licensesInReport(xmlByLicense) == 1
        dependencyWithLicensePresent(xmlByDependency, "testSubGroup:subproject:1.7", "subproject-1.7.jar", "SbPrj license")
    }

    def "should report project dependency if no license specified"() {
        given:
        buildFile << """
dependencies {
    implementation project(":subproject")
}

downloadLicenses.includeProjectDependencies = true
"""
        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        def xmlByDependency = xml4LicenseByDependencyReport()
        def xmlByLicense = xml4DependencyByLicenseReport()

        dependenciesInReport(xmlByDependency) == 1
        licensesInReport(xmlByLicense) == 1
        dependencyWithLicensePresent(xmlByDependency, "testSubGroup:subproject:1.7", "subproject-1.7.jar", "No license found")
    }

    def "should not report on dependencies in other configurations"() {
        given:
        buildFile << """
dependencies {
    testImplementation project.files("testDependency.jar")
    testRuntimeOnly "com.google.guava:guava:15.0"
    runtimeOnly "org.apache.ivy:ivy:2.3.0",
                "org.jboss.logging:jboss-logging:3.1.3.GA"
}
"""
        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        def xmlByDependency = xml4LicenseByDependencyReport()
        def xmlByLicense = xml4DependencyByLicenseReport()

        dependenciesInReport(xmlByDependency) == 2
        licensesInReport(xmlByLicense) == 2
    }

    def "Test that aliases works well for different dependencies with the same license for string->list mapping"() {
        given:
        file("testDependency1.jar")
        file("testDependency2.jar")
        file("testDependency3.jar")

        buildFile << """
downloadLicenses {
    aliases = [
        "The Apache Software License, Version 2.0": ["Apache 2", "The Apache 2", "Apache"]
    ]
}

downloadLicenses {
    licenses = [
        "testDependency1.jar": license("Apache 2"),
        "testDependency2.jar": license("The Apache 2"),
        "testDependency3.jar": "Apache"
    ]
}

dependencies {
    implementation project.files("testDependency1.jar")
    implementation project.files("testDependency2.jar")
    implementation project.files("testDependency3.jar")
}
"""
        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        def xmlByDependency = xml4LicenseByDependencyReport()
        def xmlByLicense = xml4DependencyByLicenseReport()

        dependenciesInReport(xmlByDependency) == 3
        licensesInReport(xmlByLicense) == 1

        xmlByLicense.license.@name.text() == "The Apache Software License, Version 2.0"
        xmlByLicense.license.dependency.size() == 3

        dependencyWithLicensePresent(xmlByDependency, "testDependency1.jar", "testDependency1.jar", "The Apache Software License, Version 2.0")
        dependencyWithLicensePresent(xmlByDependency, "testDependency2.jar", "testDependency2.jar", "The Apache Software License, Version 2.0")
        dependencyWithLicensePresent(xmlByDependency, "testDependency3.jar", "testDependency3.jar", "The Apache Software License, Version 2.0")
    }

    def "Test that aliases works well for different dependencies with the same license for licenseMetadata->list mapping"() {
        given:
        file("testDependency1.jar")
        file("testDependency2.jar")
        file("testDependency3.jar")

        buildFile << """
downloadLicenses {
    aliases[license("The Apache Software License, Version 2.0", "MY_URL")] = ["Apache 2", "The Apache 2", "Apache"]
}

downloadLicenses {
    licenses = [
        "testDependency1.jar": license("Apache 2"),
        "testDependency2.jar": license("The Apache 2"),
        "testDependency3.jar": "Apache"
    ]
}

dependencies {
    implementation project.files("testDependency1.jar")
    implementation project.files("testDependency2.jar")
    implementation project.files("testDependency3.jar")
}
"""
        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        def xmlByDependency = xml4LicenseByDependencyReport()
        def xmlByLicense = xml4DependencyByLicenseReport()

        dependenciesInReport(xmlByDependency) == 3
        licensesInReport(xmlByLicense) == 1

        xmlByLicense.license.@name.text() == "The Apache Software License, Version 2.0"
        xmlByLicense.license.dependency.size() == 3

        dependencyWithLicensePresent(xmlByDependency, "testDependency1.jar", "testDependency1.jar", "The Apache Software License, Version 2.0")
        dependencyWithLicensePresent(xmlByDependency, "testDependency2.jar", "testDependency2.jar", "The Apache Software License, Version 2.0")
        dependencyWithLicensePresent(xmlByDependency, "testDependency3.jar", "testDependency3.jar", "The Apache Software License, Version 2.0")
    }

    def "should be able to specify mixed aliases"() {
        given:
        file("testDependency1.jar")
        file("testDependency2.jar")
        file("testDependency3.jar")
        buildFile << """
dependencies {
    runtimeOnly project.files("testDependency1.jar")
    runtimeOnly project.files("testDependency2.jar")
    runtimeOnly project.files("testDependency3.jar")
}

downloadLicenses {
    aliases[license("The Apache Software License, Version 2.0", "MY_URL")] = ["Apache 2", license("The Apache 2", "url"), license("Apache", "urrrl")]
    
    licenses = [
        "testDependency1.jar": license("Apache 2"),
        "testDependency2.jar": license("The Apache 2", "url"),
        "testDependency3.jar": license("Apache", "uur")
    ]
}
"""

        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        def xmlByDependency = xml4LicenseByDependencyReport()
        def xmlByLicense = xml4DependencyByLicenseReport()

        dependenciesInReport(xmlByDependency) == 3
        licensesInReport(xmlByLicense) == 2

        dependencyWithLicensePresent(xmlByDependency, "testDependency1.jar", "testDependency1.jar", "The Apache Software License, Version 2.0")
        dependencyWithLicensePresent(xmlByDependency, "testDependency2.jar", "testDependency2.jar", "The Apache Software License, Version 2.0")
        dependencyWithLicensePresent(xmlByDependency, "testDependency3.jar", "testDependency3.jar", "Apache")
        dependencyWithLicenseUrlPresent(xmlByDependency, "testDependency1.jar", "MY_URL")
        dependencyWithLicenseUrlPresent(xmlByDependency, "testDependency2.jar", "MY_URL")
        dependencyWithLicenseUrlPresent(xmlByDependency, "testDependency3.jar", "uur")
    }

    def "should apply aliases for dependencies with specific license urls"() {
        given:
        buildFile << """
dependencies {
    runtimeOnly 'org.apache.ivy:ivy:2.3.0'
    runtimeOnly 'net.java.dev.jna:jna:4.0.0'
    implementation 'cglib:cglib:2.2.2'
}

downloadLicenses {
    aliases[license("Apache 2", "MY_URL")] = [
            "ASF 2.0",
            license("ASL, version 2", "http://www.apache.org/licenses/LICENSE-2.0.txt"),
            "The Apache Software License, Version 2.0"]
}
"""
        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        def xmlByDependency = xml4LicenseByDependencyReport()
        dependencyWithLicensePresent(xmlByDependency, "org.apache.ivy:ivy:2.3.0", "ivy-2.3.0.jar", "Apache 2")
        dependencyWithLicensePresent(xmlByDependency, "net.java.dev.jna:jna:4.0.0", "jna-4.0.0.jar", "ASL, version 2")
        dependencyWithLicensePresent(xmlByDependency, "cglib:cglib:2.2.2", "cglib-2.2.2.jar", "Apache 2")

        dependencyWithLicenseUrlPresent(xmlByDependency, "org.apache.ivy:ivy:2.3.0", "MY_URL")
        dependencyWithLicenseUrlPresent(xmlByDependency, "net.java.dev.jna:jna:4.0.0", "http://www.apache.org/licenses/")
        dependencyWithLicenseUrlPresent(xmlByDependency, "cglib:cglib:2.2.2", "MY_URL")

    }

    def "should override license from dependency"() {
        given:
        buildFile << """
dependencies {
    implementation "org.jboss.logging:jboss-logging:3.1.3.GA"
    implementation "com.google.guava:guava:15.0"
}

downloadLicenses {
    licenses = [
        "com.google.guava:guava:15.0": license("MY_LICENSE", "MY_URL"),
        "org.jboss.logging:jboss-logging:3.1.3.GA": license("MY_LICENSE", "MY_URL")
    ]
}
"""

        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        def xmlByDependency = xml4LicenseByDependencyReport()
        def xmlByLicense = xml4DependencyByLicenseReport()

        dependenciesInReport(xmlByDependency) == 2
        licensesInReport(xmlByLicense) == 1
        dependencyWithLicensePresent(xmlByDependency, "org.jboss.logging:jboss-logging:3.1.3.GA", "jboss-logging-3.1.3.GA.jar", "MY_LICENSE")
        dependencyWithLicensePresent(xmlByDependency, "com.google.guava:guava:15.0", "guava-15.0.jar", "MY_LICENSE")
        dependencyWithLicenseUrlPresent(xmlByDependency, "org.jboss.logging:jboss-logging:3.1.3.GA", "MY_URL")
        dependencyWithLicenseUrlPresent(xmlByDependency, "com.google.guava:guava:15.0", "MY_URL")

        xmlByLicense.license.@name.text() == "MY_LICENSE"
        xmlByLicense.license.@url.text() == "MY_URL"
    }

    def "should override license for entire groupId"() {
        given:
        buildFile << """
dependencies {
    implementation "org.jboss.logging:jboss-logging:3.1.3.GA"
    implementation 'org.jboss.logging:jboss-logging-log4j:2.1.2.GA'
}

downloadLicenses {
    licenses = [(group("org.jboss.logging")): "MY_LICENSE"]
}
"""

        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        def xmlByDependency = xml4LicenseByDependencyReport()
        dependencyWithLicensePresent(xmlByDependency, "org.jboss.logging:jboss-logging:3.1.3.GA", "jboss-logging-3.1.3.GA.jar", "MY_LICENSE")
        dependencyWithLicensePresent(xmlByDependency, "org.jboss.logging:jboss-logging-log4j:2.1.2.GA", "jboss-logging-log4j-2.1.2.GA.jar", "MY_LICENSE")
    }

    def "should have no license by default for file dependency"() {
        given:
        file("nolicense.jar")
        buildFile << """
dependencies {
    runtimeOnly project.files("nolicense.jar")
}
"""
        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        def xmlByDependency = xml4LicenseByDependencyReport()
        def xmlByLicense = xml4DependencyByLicenseReport()

        dependenciesInReport(xmlByDependency) == 1
        licensesInReport(xmlByLicense) == 1

        dependencyWithLicensePresent(xmlByDependency, "nolicense.jar", "nolicense.jar", "No license found")
    }

    @Unroll
    def "should exclude file dependencies"() {
        given:
        file("dep1.jar")
        file("dep2.jar")
        file("dep3.jar")
        buildFile << """
dependencies {
    runtimeOnly project.files("dep1.jar")
    runtimeOnly project.files("dep2.jar")
    runtimeOnly project.files("dep3.jar")
}

downloadLicenses {
    excludeDependencies = [$exclusion]
}
"""
        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        def xmlByDependency = xml4LicenseByDependencyReport()
        def xmlByLicense = xml4DependencyByLicenseReport()

        dependenciesInReport(xmlByDependency) == 1
        licensesInReport(xmlByLicense) == 1

        dependencyWithLicensePresent(xmlByDependency, "dep3.jar", "dep3.jar", "No license found")

        where:
        exclusion << ["'dep[12].*'", "'dep1.jar', 'dep2.jar'"]
    }

    @Unroll
    def "should exclude dependencies"() {
        given:
        buildFile << """
dependencies {
        implementation 'com.google.guava:guava:15.0'
}

downloadLicenses {
    excludeDependencies = [$exclusion]
}
"""

        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        dependenciesInReport(xml4LicenseByDependencyReport()) == 0
        licensesInReport(xml4DependencyByLicenseReport()) == 0

        where:
        exclusion << ["'com.google.guava:guava:15.0'", "'.*jboss.*', 'com.google.*'"]
    }

    def "should ignore non-existing excluded dependencies"() {
        given:
        file("dep1.jar")
        file("dep2.jar")
        file("dep3.jar")
        buildFile << """
dependencies {
    runtimeOnly project.files("dep1.jar")
    runtimeOnly project.files("dep2.jar")
    runtimeOnly project.files("dep3.jar")
}

downloadLicenses {
    excludeDependencies = ["non-existing1", "wrong-other"]
}
"""

        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        def xmlByDependency = xml4LicenseByDependencyReport()
        def xmlByLicense = xml4DependencyByLicenseReport()

        dependenciesInReport(xmlByDependency) == 3
        licensesInReport(xmlByLicense) == 1
        dependencyWithLicensePresent(xmlByDependency, "dep1.jar", "dep1.jar", "No license found")
        dependencyWithLicensePresent(xmlByDependency, "dep2.jar", "dep2.jar", "No license found")
        dependencyWithLicensePresent(xmlByDependency, "dep3.jar", "dep3.jar", "No license found")
    }

    def "should report all licenses of a single dependency"() {
        given:
        buildFile << """
dependencies {
    implementation "org.codehaus.jackson:jackson-jaxrs:1.9.13"
}
"""

        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        def xmlByDependency = xml4LicenseByDependencyReport()
        def xmlByLicense = xml4DependencyByLicenseReport()

        xmlByDependency.dependency.find {
            it.file.text() == 'jackson-jaxrs-1.9.13.jar'
        }.license.size() == 2

        xmlByLicense.license.dependency.findAll {
            it.text() == 'jackson-jaxrs-1.9.13.jar'
        }.size() == 2

    }

    def "should omit license url from report if dependency has none"() {
        given:
        buildFile << """
dependencies {
    implementation project.files("testDependency.jar")
}
"""

        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        def xmlByDependency = xml4LicenseByDependencyReport()
        dependenciesInReport(xmlByDependency) == 1
        licensesInReport(xml4DependencyByLicenseReport()) == 1

        !xmlByDependency.dependency.license.find { it['@url'] == "testDependency.jar" }
    }

    def "should report parent license if dependency has no license, but parent has"() {
        given:
        buildFile << """
dependencies {
    implementation "org.springframework.vault:spring-vault-core:1.1.1.RELEASE"
}
"""

        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        def xmlByDependency = xml4LicenseByDependencyReport()
        dependencyWithLicensePresent(xmlByDependency, "org.springframework.vault:spring-vault-core:1.1.1.RELEASE", "spring-vault-core-1.1.1.RELEASE.jar", "Apache License, Version 2.0")
    }

    def "should report license not found if dependency and none of its parents have a license"() {
        given:
        buildFile << """
dependencies {
    implementation "org.antlr:antlr-runtime:3.4"
}
"""

        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        def xmlByDependency = xml4LicenseByDependencyReport()
        dependencyWithLicensePresent(xmlByDependency, "org.antlr:antlr-runtime:3.4", "antlr-runtime-3.4.jar", "No license found")
    }

    def "should exclude dependency from local repository without pom"() {
        given:
        directory("libs")
        file("libs/mydep-1.0.0.jar")
        buildFile << """
repositories {
    flatDir name: "local_repo", dir: "libs"
}

dependencies {
    implementation ":mydep:1.0.0"
}

downloadLicenses {
    excludeDependencies = [":mydep:1.0.0"]
}
"""
        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        dependenciesInReport(xml4LicenseByDependencyReport()) == 0
        licensesInReport(xml4DependencyByLicenseReport()) == 0

    }

    def "should work if no dependencies in project"() {
        given:
        buildFile << """
dependencies {
}
"""

        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        outputDir.listFiles().length == 4
        dependenciesInReport(xml4LicenseByDependencyReport()) == 0
        licensesInReport(xml4DependencyByLicenseReport()) == 0
    }

    def "should generate all reports"() {
        given:
        buildFile << """
downloadLicenses {
    reportByDependency = true
    reportByLicenseType = true
    
    report {
        xml.enabled = true
        json.enabled = true
        html.enabled = true
    }
}

dependencies {
    implementation 'com.google.guava:guava:15.0'
}
"""

        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        outputDir.listFiles().length == 6
    }

    def "should put reports in project.buildDir if that is changed"() {
        given:
        buildFile << """
downloadLicenses {
    reportByDependency = true
    reportByLicenseType = true
    
    report {
        xml.enabled = true
        json.enabled = true
        html.enabled = false
    }
}

project.buildDir = "target"

dependencies {
    implementation 'com.google.guava:guava:15.0'
}
"""

        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        outputDir.listFiles().length == 0
        directory("target/reports/license").listFiles().length == 4

    }

    def "should not generate reports if no report types enabled"() {
        given:
        buildFile << """
downloadLicenses {
    reportByDependency = false
    reportByLicenseType = false
    
    report {
        xml.enabled = true
        json.enabled = true
        html.enabled = true
    }
}

dependencies {
    implementation 'com.google.guava:guava:15.0'
}
"""

        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        outputDir.listFiles().length == 0
    }

    def "should not generate reports if no report formats enabled"() {
        given:
        buildFile << """
downloadLicenses {
    reportByDependency = true
    reportByLicenseType = true
    
    report {
        xml.enabled = false
        json.enabled = false
        html.enabled = false
    }
}

dependencies {
    implementation 'com.google.guava:guava:15.0'
}
"""

        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        outputDir.listFiles().length == 0
    }

    def "should not generate report if task disabled"() {
        given:
        buildFile << """
tasks.downloadLicenses.enabled = false

dependencies {
    implementation 'com.google.guava:guava:15.0'
}
"""
        when:
        runTasksSuccessfully("downloadLicenses")

        then:
        outputDir.listFiles().length == 0
    }

    def xml4DependencyByLicenseReport() {
        File reportByLicense = new File(outputDir, LicenseReportingPlugin.DEFAULT_FILE_NAME_FOR_REPORTS_BY_LICENSE + ".xml")
        new XmlSlurper().parse(reportByLicense)
    }

    def xml4LicenseByDependencyReport() {
        File reportByDependency = new File(outputDir, LicenseReportingPlugin.DEFAULT_FILE_NAME_FOR_REPORTS_BY_DEPENDENCY + ".xml")
        new XmlSlurper().parse(reportByDependency)
    }

    static def dependenciesInReport(GPathResult xmlByDependency) {
        xmlByDependency.dependency.size()
    }

    static def licensesInReport(GPathResult xmlByLicense) {
        xmlByLicense.license.size()
    }


    static def dependencyWithLicensePresent(GPathResult xmlByDependency, String d, String jar, String l) {
        xmlByDependency.dependency.find {
            it.@name.text() == d && it.file.text() == jar
        }.license.any {
            it.@name == l
        }
    }

    static def dependencyWithLicenseUrlPresent(GPathResult xmlByDependency, String d, String lUrl) {
        xmlByDependency.dependency.find {
            it.@name.text() == d
        }.license.any {
            it.@url == lUrl
        }
    }

}
