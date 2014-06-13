package nl.javadude.gradle.plugins.license

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

    def "Test that report generating in multi module build doesn't include unrelated subprojects dependencies"() {
        setup:
        subproject.dependencies {
            compile "org.jboss.logging:jboss-logging:3.1.3.GA"
            compile "com.google.guava:guava:15.0"
        }

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        dependenciesInReport(xml4LicenseByDependencyReport(f)) == 0
        licensesInReport(xml4DependencyByLicenseReport(f)) == 0
    }

    def "Test that report in multi module build includes transitive prj dependencies, prj dependencies included and specified"() {
        setup:
        subproject.dependencies {
            compile "org.jboss.logging:jboss-logging:3.1.3.GA"
            compile "com.google.guava:guava:15.0"
        }

        project.dependencies {
            compile project.project(":subproject1")
        }
        downloadLicenses.licenses = [
                "com.google.guava:guava:15.0": license("MY_LICENSE", "MY_URL"),
                "org.jboss.logging:jboss-logging:3.1.3.GA": license("MY_LICENSE", "MY_URL"),
                "testGroup:subproject1:1.7" : "SbPrj license"
        ]
        downloadLicenses.includeProjectDependencies = true

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 3
        licensesInReport(xmlByLicense) == 2

        dependencyWithLicensePresent(xmlByDependency, "org.jboss.logging:jboss-logging:3.1.3.GA", "jboss-logging-3.1.3.GA.jar", "MY_LICENSE")
        dependencyWithLicensePresent(xmlByDependency, "com.google.guava:guava:15.0", "guava-15.0.jar", "MY_LICENSE")
        dependencyWithLicensePresent(xmlByDependency, "testGroup:subproject1:1.7", "subproject1-1.7.jar", "SbPrj license")

        dependencyWithLicenseUrlPresent(xmlByDependency, "org.jboss.logging:jboss-logging:3.1.3.GA", "MY_URL")
        dependencyWithLicenseUrlPresent(xmlByDependency, "com.google.guava:guava:15.0", "MY_URL")
    }

    def "Test that report in multi module build includes transitive prj dependencies, prj dependencies included and not specified"() {
        setup:
        subproject.dependencies {
            compile "org.jboss.logging:jboss-logging:3.1.3.GA"
            compile "com.google.guava:guava:15.0"
        }

        project.dependencies {
            compile project.project(":subproject1")
        }
        downloadLicenses.licenses = [
                "com.google.guava:guava:15.0": license("MY_LICENSE", "MY_URL"),
                "org.jboss.logging:jboss-logging:3.1.3.GA": license("MY_LICENSE", "MY_URL")
        ]
        downloadLicenses.includeProjectDependencies = true

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 3
        licensesInReport(xmlByLicense) == 2

        dependencyWithLicensePresent(xmlByDependency, "org.jboss.logging:jboss-logging:3.1.3.GA", "jboss-logging-3.1.3.GA.jar", "MY_LICENSE")
        dependencyWithLicensePresent(xmlByDependency, "com.google.guava:guava:15.0", "guava-15.0.jar", "MY_LICENSE")
        dependencyWithLicensePresent(xmlByDependency, "testGroup:subproject1:1.7", "subproject1-1.7.jar", "No license found")

        dependencyWithLicenseUrlPresent(xmlByDependency, "org.jboss.logging:jboss-logging:3.1.3.GA", "MY_URL")
        dependencyWithLicenseUrlPresent(xmlByDependency, "com.google.guava:guava:15.0", "MY_URL")
    }

    def "Test that report in multi module build includes transitive prj dependencies, prj dependencies not included"() {
        setup:
        subproject.dependencies {
            compile "org.jboss.logging:jboss-logging:3.1.3.GA"
            compile "com.google.guava:guava:15.0"
        }

        project.dependencies {
            compile project.project(":subproject1")
        }
        downloadLicenses.licenses = [
                "com.google.guava:guava:15.0": license("MY_LICENSE", "MY_URL"),
                "org.jboss.logging:jboss-logging:3.1.3.GA": license("MY_LICENSE", "MY_URL"),
                "testGroup:subproject1:1.7" : "SbPrj license"
        ]
        downloadLicenses.includeProjectDependencies = false

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 2
        licensesInReport(xmlByLicense) == 1

        dependencyWithLicensePresent(xmlByDependency, "org.jboss.logging:jboss-logging:3.1.3.GA", "jboss-logging-3.1.3.GA.jar", "MY_LICENSE")
        dependencyWithLicensePresent(xmlByDependency, "com.google.guava:guava:15.0", "guava-15.0.jar", "MY_LICENSE")

        dependencyWithLicenseUrlPresent(xmlByDependency, "org.jboss.logging:jboss-logging:3.1.3.GA", "MY_URL")
        dependencyWithLicenseUrlPresent(xmlByDependency, "com.google.guava:guava:15.0", "MY_URL")
    }

    def "Test that dependencyConfiguration defaults to runtime"() {
        setup:
        project.dependencies {
            testCompile project.files("testDependency.jar")
            testRuntime "com.google.guava:guava:15.0"
            runtime "org.apache.ivy:ivy:2.3.0",
                    "org.jboss.logging:jboss-logging:3.1.3.GA"
        }

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 2
        licensesInReport(xmlByLicense) == 2
    }

    def "Test that explicitly-specified dependencyConfiguration is respected"() {
        setup:
        project.dependencies {
            runtime project.files("testDependency.jar")
            testRuntime "com.google.guava:guava:15.0"
            testCompile "org.apache.ivy:ivy:2.3.0",
                        "org.jboss.logging:jboss-logging:3.1.3.GA"
        }
        downloadLicenses.dependencyConfiguration = "testCompile"

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 2
        licensesInReport(xmlByLicense) == 2
    }

    def "Test that aliases works well for different dependencies with the same license for string->list mapping"() {
        setup:
        File dependencyJar1 = new File(projectDir, "testDependency1.jar")
        dependencyJar1.createNewFile()

        File dependencyJar2 = new File(projectDir, "testDependency2.jar")
        dependencyJar2.createNewFile()

        File dependencyJar3 = new File(projectDir, "testDependency3.jar")
        dependencyJar3.createNewFile()

        downloadLicenses.aliases = [
                "The Apache Software License, Version 2.0": ["Apache 2", "The Apache 2", "Apache"]]

        downloadLicenses.licenses = ["testDependency1.jar": license("Apache 2"),
                "testDependency2.jar": license("The Apache 2"),
                "testDependency3.jar": "Apache"]

        project.dependencies {
            runtime project.files("testDependency1.jar")
            runtime project.files("testDependency2.jar")
            runtime project.files("testDependency3.jar")
        }

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 3
        licensesInReport(xmlByLicense) == 1

        xmlByLicense.license.@name.text() == "The Apache Software License, Version 2.0"
        xmlByLicense.license.dependency.size() == 3

        dependencyWithLicensePresent(xmlByDependency, "testDependency1.jar", "testDependency1.jar", "The Apache Software License, Version 2.0")
        dependencyWithLicensePresent(xmlByDependency, "testDependency2.jar", "testDependency2.jar", "The Apache Software License, Version 2.0")
        dependencyWithLicensePresent(xmlByDependency, "testDependency3.jar", "testDependency3.jar", "The Apache Software License, Version 2.0")
    }

    def "Test that aliases works well for different dependencies with the same license for licenseMetadata->list mapping"() {
        setup:
        File dependencyJar1 = new File(projectDir, "testDependency1.jar")
        dependencyJar1.createNewFile()

        File dependencyJar2 = new File(projectDir, "testDependency2.jar")
        dependencyJar2.createNewFile()

        File dependencyJar3 = new File(projectDir, "testDependency3.jar")
        dependencyJar3.createNewFile()

        HashMap<Object, List> aliases = new HashMap()
        aliases.put(license("The Apache Software License, Version 2.0", "MY_URL"), ["Apache 2", "The Apache 2", "Apache"])
        downloadLicenses.aliases = aliases

        downloadLicenses.licenses = ["testDependency1.jar": license("Apache 2"),
                "testDependency2.jar": license("The Apache 2"),
                "testDependency3.jar": license("Apache")]

        project.dependencies {
            runtime project.files("testDependency1.jar")
            runtime project.files("testDependency2.jar")
            runtime project.files("testDependency3.jar")
        }

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 3
        licensesInReport(xmlByLicense) == 1

        xmlByLicense.license.@name.text() == "The Apache Software License, Version 2.0"
        xmlByLicense.license.dependency.size() == 3

        dependencyWithLicensePresent(xmlByDependency, "testDependency1.jar", "testDependency1.jar", "The Apache Software License, Version 2.0")
        dependencyWithLicensePresent(xmlByDependency, "testDependency2.jar", "testDependency2.jar", "The Apache Software License, Version 2.0")
        dependencyWithLicensePresent(xmlByDependency, "testDependency3.jar", "testDependency3.jar", "The Apache Software License, Version 2.0")
        dependencyWithLicenseUrlPresent(xmlByDependency, "testDependency1.jar", "MY_URL")
        dependencyWithLicenseUrlPresent(xmlByDependency, "testDependency2.jar", "MY_URL")
        dependencyWithLicenseUrlPresent(xmlByDependency, "testDependency3.jar", "MY_URL")
    }

    def "Test that aliases can me mixed in mapping licenseMetadata/String->list<String/LicenseMetadata> mapping for file dependencies"() {
        setup:
        File dependencyJar1 = new File(projectDir, "testDependency1.jar")
        dependencyJar1.createNewFile()

        File dependencyJar2 = new File(projectDir, "testDependency2.jar")
        dependencyJar2.createNewFile()

        File dependencyJar3 = new File(projectDir, "testDependency3.jar")
        dependencyJar3.createNewFile()

        HashMap<Object, List> aliases = new HashMap()
        aliases.put(license("The Apache Software License, Version 2.0", "MY_URL"), ["Apache 2", license("The Apache 2", "url"), license("Apache", "urrrl")])
        downloadLicenses.aliases = aliases

        downloadLicenses.licenses = ["testDependency1.jar": license("Apache 2"),
                                     "testDependency2.jar": license("The Apache 2", "url"),
                                     "testDependency3.jar": license("Apache", "uur")
        ]

        project.dependencies {
            runtime project.files("testDependency1.jar")
            runtime project.files("testDependency2.jar")
            runtime project.files("testDependency3.jar")
        }

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 3
        licensesInReport(xmlByLicense) == 2

        dependencyWithLicensePresent(xmlByDependency, "testDependency1.jar", "testDependency1.jar", "The Apache Software License, Version 2.0")
        dependencyWithLicensePresent(xmlByDependency, "testDependency2.jar", "testDependency2.jar", "The Apache Software License, Version 2.0")
        dependencyWithLicensePresent(xmlByDependency, "testDependency3.jar", "testDependency3.jar", "Apache")
        dependencyWithLicenseUrlPresent(xmlByDependency, "testDependency1.jar", "MY_URL")
        dependencyWithLicenseUrlPresent(xmlByDependency, "testDependency2.jar", "MY_URL")
        dependencyWithLicenseUrlPresent(xmlByDependency, "testDependency3.jar", "uur")
    }

    def "Test that we can apply aliases for dependencies with certain license url"() {
        setup:
        HashMap<Object, List> aliases = new HashMap()
        aliases.put(license("Apache 2", "MY_URL"), [
                    "ASF 2.0",
                    license("ASL, version 2", "http://www.apache.org/licenses/LICENSE-2.0.txt"),
                    "The Apache Software License, Version 2.0"]
        )
        downloadLicenses.aliases = aliases

        project.dependencies {
            runtime 'org.apache.ivy:ivy:2.3.0'
            runtime 'net.java.dev.jna:jna:4.0.0'
            compile 'cglib:cglib:2.2.2'
        }

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)

        dependencyWithLicensePresent(xmlByDependency, "org.apache.ivy:ivy:2.3.0", "ivy-2.3.0.jar", "Apache 2")
        dependencyWithLicensePresent(xmlByDependency, "net.java.dev.jna:jna:4.0.0", "jna-4.0.0.jar", "ASL, version 2")
        dependencyWithLicensePresent(xmlByDependency, "cglib:cglib:2.2.2", "cglib-2.2.2.jar", "Apache 2")

        dependencyWithLicenseUrlPresent(xmlByDependency, "org.apache.ivy:ivy:2.3.0", "MY_URL")
        dependencyWithLicenseUrlPresent(xmlByDependency, "net.java.dev.jna:jna:4.0.0", "http://www.apache.org/licenses/")
        dependencyWithLicenseUrlPresent(xmlByDependency, "cglib:cglib:2.2.2", "MY_URL")
    }

    def "Test that we can specify license that will override existent license for dependency"() {
        setup:
        project.dependencies {
            compile "org.jboss.logging:jboss-logging:3.1.3.GA"
            compile "com.google.guava:guava:15.0"
        }
        downloadLicenses.licenses = [
                "com.google.guava:guava:15.0": license("MY_LICENSE", "MY_URL"),
                "org.jboss.logging:jboss-logging:3.1.3.GA": license("MY_LICENSE", "MY_URL")
        ]

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 2
        licensesInReport(xmlByLicense) == 1

        dependencyWithLicensePresent(xmlByDependency, "org.jboss.logging:jboss-logging:3.1.3.GA", "jboss-logging-3.1.3.GA.jar", "MY_LICENSE")
        dependencyWithLicensePresent(xmlByDependency, "com.google.guava:guava:15.0", "guava-15.0.jar", "MY_LICENSE")
        dependencyWithLicenseUrlPresent(xmlByDependency, "org.jboss.logging:jboss-logging:3.1.3.GA", "MY_URL")
        dependencyWithLicenseUrlPresent(xmlByDependency, "com.google.guava:guava:15.0", "MY_URL")

        xmlByLicense.license.@name.text() == "MY_LICENSE"
        xmlByLicense.license.@url.text() == "MY_URL"
    }

    def "Test that we can specify groupId for which we will use license in the report"() {
        setup:
        project.dependencies {
            compile "org.jboss.logging:jboss-logging:3.1.3.GA"
            compile 'org.jboss.logging:jboss-logging-log4j:2.1.2.GA'
        }

        project.downloadLicenses {
            licenses = [(group("org.jboss.logging")): "MY_LICENSE"]
        }

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)

        dependencyWithLicensePresent(xmlByDependency, "org.jboss.logging:jboss-logging:3.1.3.GA", "jboss-logging-3.1.3.GA.jar", "MY_LICENSE")
        dependencyWithLicensePresent(xmlByDependency, "org.jboss.logging:jboss-logging-log4j:2.1.2.GA", "jboss-logging-log4j-2.1.2.GA.jar", "MY_LICENSE")
    }

    def "Test that file dependencies has no license by default"() {
        setup:
        File dependencyJar1 = new File(projectDir, "nolicense.jar")
        dependencyJar1.createNewFile()
        project.dependencies {
            runtime project.files("nolicense.jar")
        }

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 1
        licensesInReport(xmlByLicense) == 1

        dependencyWithLicensePresent(xmlByDependency, "nolicense.jar", "nolicense.jar", "No license found")
    }

    def "Test that we can exclude particular file dependencies from report"() {
        setup:
        File dependencyJar1 = new File(projectDir, "dep1.jar")
        dependencyJar1.createNewFile()
        File dependencyJar2 = new File(projectDir, "dep2.jar")
        dependencyJar2.createNewFile()
        File dependencyJar3 = new File(projectDir, "dep3.jar")
        dependencyJar3.createNewFile()
        project.dependencies {
            runtime project.files("dep1.jar")
            runtime project.files("dep2.jar")
            runtime project.files("dep3.jar")
        }
        project.downloadLicenses {
            excludeDependencies = ["dep1.jar", "dep2.jar"]
        }
        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 1
        licensesInReport(xmlByLicense) == 1

        dependencyWithLicensePresent(xmlByDependency, "dep3.jar", "dep3.jar", "No license found")
    }

    def "Test that we can exclude particular external dependencies from report"() {
        setup:
        project.dependencies {
            compile 'com.google.guava:guava:15.0'
        }
        project.downloadLicenses {
            excludeDependencies = ['com.google.guava:guava:15.0']
        }
        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 0
        licensesInReport(xmlByLicense) == 0
    }

    def "Test that excluding unexisting dependencies from report does nothing"() {
        setup:
        File dependencyJar1 = new File(projectDir, "dep1.jar")
        dependencyJar1.createNewFile()
        File dependencyJar2 = new File(projectDir, "dep2.jar")
        dependencyJar2.createNewFile()
        File dependencyJar3 = new File(projectDir, "dep3.jar")
        dependencyJar3.createNewFile()
        project.dependencies {
            runtime project.files("dep1.jar")
            runtime project.files("dep2.jar")
            runtime project.files("dep3.jar")
        }
        project.downloadLicenses {
            excludeDependencies = ["unexistingDependency1", "unexistingDependency2"]
        }
        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 3
        licensesInReport(xmlByLicense) == 1
        dependencyWithLicensePresent(xmlByDependency, "dep1.jar", "dep1.jar", "No license found")
        dependencyWithLicensePresent(xmlByDependency, "dep2.jar", "dep2.jar", "No license found")
        dependencyWithLicensePresent(xmlByDependency, "dep3.jar", "dep3.jar", "No license found")
    }

    def "Test that dependency can have several licenses"() {
        setup:
        project.dependencies {
            compile 'org.codehaus.jackson:jackson-jaxrs:1.9.13'
        }

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        xmlByDependency.dependency.find {
            it.file.text() == 'jackson-jaxrs-1.9.13.jar'
        }.license.size() == 2

        xmlByLicense.license.dependency.findAll {
              it.text() == 'jackson-jaxrs-1.9.13.jar'
        }.size() == 2

    }

    def "Test that if dependency has no license url it will be omitted in the report"() {
        setup:
        project.dependencies {
            compile project.files("testDependency.jar")
        }

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 1
        licensesInReport(xmlByLicense) == 1

        !xmlByDependency.dependency.license.find { it['@url'] == "testDependency.jar" }.asBoolean()
    }

    def "Test if a dependency from a local repository without pom should be excluded"() {
      setup:
      File flatRepoDir = new File(projectDir, "libs")
      flatRepoDir.mkdir()
      new File(flatRepoDir, "mydep-1.0.0.jar").createNewFile()

      project.repositories {
        flatDir name: 'local_repo', dir: 'libs'
      }

      project.dependencies {
        compile ":mydep:1.0.0"
      }

      project.downloadLicenses {
        excludeDependencies = [":mydep:1.0.0"]
      }

      when:
      downloadLicenses.execute()

      then:
      File f = getLicenseReportFolder()
      assertLicenseReportsExist(f)
    }

    def "Test that plugin works if no dependencies defined in the project"() {
        setup:
        project.dependencies {
        }

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 0
        licensesInReport(xmlByLicense) == 0
    }

    def "Test that plugin generates 4 reports"() {
        when:
        project.dependencies {
            compile 'com.google.guava:guava:14.0'
        }
        project.downloadLicenses {
            reportByDependency = true
            reportByLicenseType = true
            project.downloadLicenses.report {
                xml.enabled = true
                html.enabled = true
            }
        }
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        f.exists()
        f.listFiles().length == 4
    }

    def "Test that plugin generate no reports when all report types are disabled"() {
        setup:
        downloadLicenses.reportByDependency = false
        downloadLicenses.reportByLicenseType = false
        project.downloadLicenses.report {
            xml.enabled = true
            html.enabled = true
        }
        project.dependencies {
            compile 'com.google.guava:guava:15.0'
        }

        when:
        downloadLicenses.execute()

        then:
        File f = new File(LICENSE_REPORT)
        f.exists()
        f.listFiles().length == 0
    }

    def "Test that plugin generate no reports when all report formats are disabled"() {
        setup:
        downloadLicenses.reportByDependency = true
        downloadLicenses.reportByLicenseType = true
        project.downloadLicenses.report {
            xml.enabled = false
            html.enabled = false
        }
        project.dependencies {
            compile 'com.google.guava:guava:15.0'
        }

        when:
        downloadLicenses.execute()

        then:
        File f = new File(LICENSE_REPORT)
        f.exists()
        f.listFiles().length == 0
    }

    def "Test that plugin generate no reports when it is fully disabled"() {
        setup:
        downloadLicenses.enabled = false
        project.dependencies {
            compile 'com.google.guava:guava:15.0'
        }

        when:
        downloadLicenses.execute()

        then:
        File f = new File(LICENSE_REPORT)
        !f.exists()
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
            project.downloadLicenses.report {
                xml.enabled = true
                html.enabled = false
                xml.destination = this.outputDir
                html.destination = this.outputDir
            }
            licenses = ["testDependency.jar": license("Apache 2")]
        }
    }

    def xml4DependencyByLicenseReport(File reportDir) {
        File reportByLicense = new File(reportDir, LicensePlugin.DEFAULT_FILE_NAME_FOR_REPORTS_BY_LICENSE + ".xml")
        new XmlSlurper().parse(reportByLicense)
    }

    def xml4LicenseByDependencyReport(File reportDir) {
        File reportByDependency = new File(reportDir, LicensePlugin.DEFAULT_FILE_NAME_FOR_REPORTS_BY_DEPENDENCY + ".xml")
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
        f.listFiles().length == 2
    }

    def dependenciesInReport(GPathResult xmlByDependency) {
        xmlByDependency.dependency.size()
    }

    def licensesInReport(GPathResult xmlByLicense) {
        xmlByLicense.license.size()
    }
}
