= License Gradle Plugin
Jeroen van Erp
:license_plugin_version: 0.15.0

image:https://travis-ci.org/hierynomus/license-gradle-plugin.svg?branch=master[Build Status,link=https://travis-ci.org/hierynomus/license-gradle-plugin]

This plugin will scan and adapt your source files to include a provided header, e.g. a LICENSE file.  By default it will scan every source set and report warnings. It will also create format tasks, which will properly format and apply the specified header. A bulk of the logic comes from the maven-license-plugin.

This plugin will also report on the licenses of your dependencies.

== Applying the plugin
From v0.11.0 onwards the `license-gradle-plugin` will be published to http://bintray.com[] and will be available through the http://plugins.gradle.org/[Gradle plugin exchange]. This means that there are a few different usage scenarios listed below.


=== Gradle 2.1 and above
In your `build.gradle` file add:

[source,groovy,subs="verbatim,attributes"]
----
plugins {
    id "com.github.hierynomus.license" version "{license_plugin_version}"
}
----

=== Gradle 1.x/2.0, latest license-gradle-plugin
In your `build.gradle` file add:

[source,groovy,subs="verbatim,attributes"]
----
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.com.hierynomus.gradle.plugins:license-gradle-plugin:{license_plugin_version}"
  }
}

apply plugin: "com.github.hierynomus.license"
----

=== Gradle 1.x/2.0, gradle-license-plugin 0.10.0 (and earlier)
In your `build.gradle` file add:

[source,groovy,subs="verbatim,attributes"]
----
    buildscript {
        repositories {
            mavenCentral()
        }

        dependencies {
            classpath 'nl.javadude.gradle.plugins:license-gradle-plugin:0.10.0'
        }
    }

    apply plugin: 'license'
----

== Source file license application

=== com.github.hierynomus.license-base Plugin

[source,groovy,subs="verbatim,attributes"]
----
plugins {
  id "com.github.hierynomus.license-base" version"{license_plugin_version}"
}
----

This plugin contains two types of tasks to each source set available in your project: one for checking for consistency and one to apply the header, e.g.

- licenseMain (`LicenseCheck`)        : checks for header consistency in the main source set
- licenseFormatMain (`LicenseFormat`) : applies the license found in the header file in files missing the header
- licenseTest (`LicenseCheck`)        : checks for header consistency in the test source set
- licenseFormatTest (`LicenseFormat`) : applies the license found in the header file in files missing the header in the test source set
- etc.

Furthermore this task exposes the `license` extension to the project, allowing you to finetune the configuration of the plugin.

=== com.github.hierynomus.license Plugin

[source,groovy,subs="verbatim,attributes"]
----
plugins {
  id "com.github.hierynomus.license" version"{license_plugin_version}"
}
----

This plugin will apply the following plugins:

- `com.github.hierynomus.license-base`
- `com.github.hierynomus.license-report`

Furthermore it will create the following tasks:

- `licenseCheck`: This task depends on all the available `LicenseCheck` tasks in the project
- `licenseFormat`: This task depends on all the available `LicenseFormat` tasks in the project

Also it will add the `licenseCheck` task to the `check` lifecycle task dependencies, so that during a regular build any missing headers are flagged.

=== Tasks

==== LicenseCheck
This task checks all the configured source files to validate whether the correct header has been applied. It can be configured using the following properties,
most of which can also be set on the extension to configure them for all tasks.

[cols="h,d"]
|====
|header |Specify location of header to use in comparisons, default to project.file('LICENSE')
|headerURI |Specify location of header as a URI, see section below on Header Locations for examples
|ignoreFailures |Prevent tasks from stopping the build, defaults to false
|dryRun |Show what would happen if the task was run, defaults to false but also inherits from `--dryRun`
|skipExistingHeaders |Skip over files that have some header already, which might not be the one specified in the header parameter, defaults to false
|useDefaultMappings |Use a long list of standard mapping, defaults to true. See http://code.mycila.com/license-maven-plugin/#supported-comment-types[] for the complete list
|strictCheck |Be extra strict in the formatting of existing headers, defaults to false
|mapping(String ext, String style) |Adds a mapping between a file extension and a style type
|mapping(Map<String,String> mappings) |Adds mappings between file extensions and style types
|mapping(Closure) |Adds mappings between file extensions and a style types, see example below
|exclude(String pattern) |Add an ANT style pattern to exclude files from license absence reporting and license application
|excludes(Collection<String> patterns) |Add ANT style patterns to exclude files from license absence reporting and license application
|include(String pattern) |Add an ANT style pattern to include files into license absence reporting and license application
|includes(Collection<String> patterns) |Add ANT style patterns to include files into license absence reporting and license application
|====

==== LicenseFormat
This task formats all the configured source files to add a header to them if no header has been applied yet. It can be configured using the following properties,
most of which can also be set on the extension to configure them for all tasks.

[cols="h,d"]
|====
|header |Specify location of header to use in comparisons, default to project.file('LICENSE')
|headerURI |Specify location of header as a URI, see section below on Header Locations for examples
|ignoreFailures |Prevent tasks from stopping the build, defaults to false
|dryRun |Show what would happen if the task was run, defaults to false but also inherits from `--dryRun`
|skipExistingHeaders |Skip over files that have some header already, which might not be the one specified in the header parameter, defaults to false
|useDefaultMappings |Use a long list of standard mapping, defaults to true. See http://code.mycila.com/license-maven-plugin/#supported-comment-types[] for the complete list
|strictCheck |Be extra strict in the formatting of existing headers, defaults to false
|mapping(String ext, String style) |Adds a mapping between a file extension and a style type
|mapping(Map<String,String> mappings) |Adds mappings between file extensions and style types
|mapping(Closure) |Adds mappings between file extensions and a style types, see example below
|exclude(String pattern) |Add an ANT style pattern to exclude files from license absence reporting and license application
|excludes(Collection<String> patterns) |Add ANT style patterns to exclude files from license absence reporting and license application
|include(String pattern) |Add an ANT style pattern to include files into license absence reporting and license application
|includes(Collection<String> patterns) |Add ANT style patterns to include files into license absence reporting and license application
|====

=== License Extension
A license extension is added to the project, which can be used to configure all `LicenseCheck` and `LicenseFormat` tasks. E.g.

[source,groovy,subs="verbatim,attributes"]
----
license {
    header rootProject.file('codequality/HEADER')
    strictCheck true
}
----

Here is a general overview of the options:

[cols="h,d"]
|====
|header |Specify location of header to use in comparisons, default to `project.file('LICENSE')`
|headerURI |Specify location of header as a URI.
|ignoreFailures |Prevent tasks from stopping the build, defaults to false
|dryRun |Show what would happen if the task was run, defaults to false but also inherits from `--dryRun`
|skipExistingHeaders |Skip over files that have some header already, which might not be the one specified in the header parameter, defaults to false
|useDefaultMappings |Use a long list of standard mapping, defaults to true. See http://code.mycila.com/license-maven-plugin/#supported-comment-types[] for the complete list
|strictCheck |Be extra strict in the formatting of existing headers, defaults to false
|mapping(String ext, String style) |Adds a mapping between a file extension and a style type
|mapping(Map<String,String> mappings) |Adds mappings between file extensions and style types
|mapping(Closure) |Adds mappings between file extensions and a style types, see example below
|exclude(String pattern) |Add an ANT style pattern to exclude files from license absence reporting and license application
|excludes(Collection<String> patterns) |Add ANT style patterns to exclude files from license absence reporting and license application
|include(String pattern) |Add an ANT style pattern to include files into license absence reporting and license application
|includes(Collection<String> patterns) |Add ANT style patterns to include files into license absence reporting and license application
|headerDefinition(HeaderDefinitionBuilder headerDefinition) |Add a custom header definition that will be added to the defaults.
|headerDefinitions(Closure) | Add a custom header definition that will be added to the defaults.
|====

[[supported-file-types]]
=== File Types
Supported by default: `java`, `groovy`, `js`, `css`, `xml`, `dtd`, `xsd`, `html`, `htm`, `xsl`, `fml`, `apt`, `properties`, `sh`, `txt`, `bat`, `cmd`, `sql`, `jsp`, `ftl`, `xhtml`, `vm`, `jspx`, `gsp`, `json`. Complete list can be found in the parent project at http://code.mycila.com/license-maven-plugin/#supported-comment-types.

=== Usage and Configuration
==== Header Locations
The plugin can load a reference license file from the local file system with the _header_ property.

[source,groovy,subs="verbatim,attributes"]
----
    license { header = file('LGPL.txt') }
----

To load a license from a URI directly it can be _headerURI_ property.

[source,groovy,subs="verbatim,attributes"]
----
    license { headerURI = new URI("https://www.gnu.org/licenses/lgpl.txt") }
----

The problem with that approach is that we're requiring a network call to run the task. Another option is
to load the license from the classpath. This is most commonly seen from a plugin which is configuring this
plugin. First you'd bundle a _LICENSE.TXT_ file into the _src/main/resources/META-INF_ directory. Then you'd
configure this plugin like the below code.

[source,groovy,subs="verbatim,attributes"]
----
    license { headerURI = myPlugin.class.getResource("/META-INF/LICENSE.TXT").toURI() }
----

In regards to the header, tasks can be configured individually or in bulk also,

[source,groovy,subs="verbatim,attributes"]
----
    licenseFormatMain.header = file('APL.txt')
    // or
    tasks.withType(License) { header = file('LGPL.txt') }
----

==== Recognizing other file types.
An extensive list of formats and mappings are available by default, see the <<supported-file-types,SupportedFormats>> link above. Occasionally a project might need to add a mapping to a unknown file type to an existing comment style.

[source,groovy,subs="verbatim,attributes"]
----
license {
    mapping {
        javascript='JAVADOC_STYLE'
    }
}
// or
license.mapping 'javascript' 'JAVADOC_STYLE'
// or
license.mapping('javascript', 'JAVADOC_STYLE')
// or directly on the task
licenseMain.mapping 'javascript' 'JAVADOC_STYLE'
----

Defining new comment types is not currently supported, but file a bug and it can be added.

==== Variable substitution
Variables in the format `${}` format will be substituted, as long as their values are provided in the extension or the task.

----
    Copyright (C) ${year} ${name} <${email}>
----

Will be completed with this extension block, the key is adding them via extra properties:

[source,groovy]
----
license {
    ext.year = Calendar.getInstance().get(Calendar.YEAR)
    ext.name = 'Company'
    ext.email = 'support@company.com'
}
// or
licenseMain.ext.year = 2012
----

==== Creating custom header definitions
When the default header definitions can not be used for your specific project, we support the ability to define custom header definitions.

Adding a new header definition is done through the license extension. These header definitions can then be assigned to the necessary file types by mapping them to their extensions.

[source,groovy]
----
license {
    headerDefinitions {
        custom_definition {
          firstLine = "//"
          endLine   = "//"
          firstLineDetectionPattern = "//"
          lastLineDetectionPattern  = "//"
          allowBlankLines = false
          skipLinePattern = "//"
          isMultiline = false
        }
    }
}
----

==== Include/Exclude files from license absence reporting and license application
By default all files in the sourceSets configured are required to carry a license. Just like with Gradle `SourceSet` you can use include/exclude patterns to control this behaviour.

The semantics are:

- no `includes` or `excludes`: All files in the sourceSets will be included
- `excludes` provided: All files except those matching the exclude patterns are included
- `includes` provided: Only the files matching the include patterns are included
- both `includes` and `excludes` provided: All files matching the include patterns, except those matching the exclude patterns are included.

For instance:

[source,groovy]
----
license {
    exclude "**/*.properties"
    excludes(["**/*.txt", "**/*.conf"])
}
----

This will exclude all `*.properties`, `*.txt` and `*.conf` files.

[source,groovy]
----
license {
    include "**/*.groovy"
    includes(["**/*.java", "**/*.properties"])
}
----

This will include only all `*.groovy`, `*.java` and `*.properties` files.

[source,groovy]
----
license {
    include "**/*.java"
    exclude "**/*Test.java"
}
----

This will include all `*.java` files, except the `*Test.java` files.

==== Running on a non-java project
By default, applying the plugin will generate license tasks for all source sets defined by the java plugin. You can also run the license task on an arbitrary file tree, if you don't have the java plugin, or your files are outside a java source tree.

[source,groovy]
----
task licenseFormatSql(type: com.hierynomus.gradle.license.tasks.LicenseFormat) {
    source = fileTree(dir: "source").include("**/*.sql")
}
licenseFormat.dependsOn licenseFormatSql
----


== Dependency License Reporting
Next to checking for and applying license headers to your source files, this plugin also supports reporting on the licenses that your dependencies are licensed under.

== com.github.hierynomus.license-report

[source,groovy,subs="verbatim,attributes"]
----
plugins {
  id "com.github.hierynomus.license-report" version"{license_plugin_version}"
}
----

This plugin will add a task to manage the downloading and reporting of licenses of your dependencies.

- `downloadLicenses`   : generates reports on your runtime dependencies

== License Reporting
The `downloadLicenses` task has a set of properties, most can be set in the extension:

[cols="h,d"]
|====
|includeProjectDependencies |true if you want to include the transitive dependencies of your project dependencies
|ignoreFatalParseErrors |true if you want to ignore fatal errors when parsing POMs of transitive dependencies
|licenses |a pre-defined mapping of a dependency to a license; useful if the external repositories do not have license information available
|aliases |a mapping between licenses; useful to consolidate the various POM definitions of different spelled/named licenses
|excludeDependencies |a List of dependencies that are to be excluded from reporting
|dependencyConfiguration |Gradle dependency configuration to report on (defaults to "runtime").
|====

A 'license()' method is made available by the License Extension that takes two Strings, the first is the license name, the second is the URL to the license.

[source,groovy]
----
downloadLicenses {
    ext.apacheTwo = license('Apache License, Version 2.0', 'http://opensource.org/licenses/Apache-2.0')
    ext.bsd = license('BSD License', 'http://www.opensource.org/licenses/bsd-license.php')

    includeProjectDependencies = true
    licenses = [
        (group('com.myproject.foo')) : license('My Company License'),
        'org.apache.james:apache-mime4j:0.6' : apacheTwo,
        'org.some-bsd:project:1.0' : bsd
    ]

    aliases = [
        (apacheTwo) : ['The Apache Software License, Version 2.0', 'Apache 2', 'Apache License Version 2.0', 'Apache License, Version 2.0', 'Apache License 2.0', license('Apache License', 'http://www.apache.org/licenses/LICENSE-2.0')],
        (bsd) : ['BSD', license('New BSD License', 'http://www.opensource.org/licenses/bsd-license.php')]
    ]

    excludeDependencies = [
        'com.some-other-project.bar:foobar:1.0'
    ]

    dependencyConfiguration = 'compile'
}
----

== Changelog

=== v0.15.0 (2018-11-22)
- Correctly published the split-up plugins (I hope..)

=== v0.14.0 (2017-??-??) --> See Upgrade Notes!
- Upgraded to com.mycila:license-maven-plugin:3.0
- Split up plugin into smaller parts (`license-base`, `license-report`, `license`)
- Merged https://github.com/hierynomus/license-gradle-plugin/pull/134[#134]: Fixed build on Gradle 3.4.+
- Merged https://github.com/hierynomus/license-gradle-plugin/pull/132[#132]: Added custom header definitions
- Fixed https://github.com/hierynomus/license-gradle-plugin/issues/127[#127]: Made reporting target directory lazy
- Merged https://github.com/hierynomus/license-gradle-plugin/pull/112[#112]: Added JSON license reporting

=== v0.13.1 (2016-06-07)
- Merged https://github.com/hierynomus/license-gradle-plugin/pull/109[#109]: Fixed compatibility with older Android plugins
=== v0.13.0 (2016-06-06)
- Upgraded Gradle build version to 2.13
- Upgraded Android Tools version to 2.0+
- Merged https://github.com/hierynomus/license-gradle-plugin/pull/106[#106]: Added boolean parameter to ignore broken poms while searching for licenses

=== v0.12.1 (2015-10-26)
- Merged https://github.com/hierynomus/license-gradle-plugin/pull/87[#87]: Fix downloadLicenses fails with `module notation '::' is invalid`

=== v0.12.0 (2015-10-07)
- Merged https://github.com/hierynomus/license-gradle-plugin/pull/56[#56]: Added Android support
- Merged https://github.com/hierynomus/license-gradle-plugin/pull/72[#72]: Fix SAX parser to ignore namespaces
- Merged https://github.com/hierynomus/license-gradle-plugin/pull/82[#82]: Also now works for Android LibraryPlugin
- Merged https://github.com/hierynomus/license-gradle-plugin/pull/83[#83]: Fix for Android plugin detection
- Merged https://github.com/hierynomus/license-gradle-plugin/pull/84[#84]: Support for unified license reports in multi-module builds (Fixes https://github.com/hierynomus/license-gradle-plugin/issues/40[#40] and https://github.com/hierynomus/license-gradle-plugin/issues/50[#50])
- Fixed https://github.com/hierynomus/license-gradle-plugin/issues/48[#48]: Added '.gradle' as standard extension
- Fixed https://github.com/hierynomus/license-gradle-plugin/issues/70[#70]: Added '.yaml' and '.yml' as standard extension
- Fixed https://github.com/hierynomus/license-gradle-plugin/issues/85[#85]: Removed source dependency on (optional) Android plugin.

=== v0.11.0
- Added support for uploading to bintray (Fixes https://github.com/hierynomus/license-gradle-plugin/issues/46[#46] and https://github.com/hierynomus/license-gradle-plugin/issues/47[#47])
- Upgraded to Gradle 2.0

=== v0.10.0
- Fixed build to enforce Java6 only for local builds, not on BuildHive
- Added `exclude` / `excludes` to extension (Fixes https://github.com/hierynomus/license-gradle-plugin/issues/39[#39])
- Added `include` / `includes` to extension (Fixes https://github.com/hierynomus/license-gradle-plugin/issues/45[#45])

=== v0.9.0
- Fixed build to force Java6 (Fixes https://github.com/hierynomus/license-gradle-plugin/issues/35[#35])
- Added example test for https://github.com/hierynomus/license-gradle-plugin/issues/38[#38]

=== v0.8.0
- Merged pull-requests https://github.com/hierynomus/license-gradle-plugin/pull/31[#31], https://github.com/hierynomus/license-gradle-plugin/pull/33[#33], https://github.com/hierynomus/license-gradle-plugin/pull/42[#42]
