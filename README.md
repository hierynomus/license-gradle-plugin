# License Gradle Plugin
This plugin will scan and adapt your source files to include a provided header, e.g. a LICENSE file.  By default it will scan every source set and report warnings. It will also create format tasks, which will properly format and apply the specified header. A bulk of the logic comes from the maven-license-plugin.

This plugin will also report on the licenses of your dependencies.

## Usage
In your _build.gradle_ file add:

```
    buildscript {
        repositories {
            mavenCentral()
        }

        dependencies {
            classpath 'nl.javadude.gradle.plugins:license-gradle-plugin:0.9.0'
        }
    }

    apply plugin: 'license'
```

This will add two types of tasks for each source set (created by BasePlugin) in your project: one for checking for consistency and one to apply the header, e.g.

- licenseMain        : checks for header consistency in the main source set
- licenseFormatMain  : applies the license found in the header file in files missing the header
- licenseTest        : checks for header consistency in the test source set

The check tasks are added to the standard Gradle _check_ task.

This will also add a task to manage the downloading and reporting of licenses of your dependencies.

- downloadLicenses   : generates reports on your runtime dependencies

## License Task
The license task has a properties, most can be set in the extension:

- header -- Specify location of header to use in comparisons, default to project.file('LICENSE')
- ignoreFailures -- Prevent tasks from stopping the build, defaults to false
- dryRun -- Show what would happen if the task was run, defaults to false but also inherits from --dryRun
- skipExistingHeaders -- Skip over files that have some header already, which might not be the one specified in the header parameter, defaults to false
- useDefaultMappings -- Use a long list of standard mapping, defaults to true. See http://code.google.com/p/maven-license-plugin/wiki/Configuration#Default_mappings for the complete list
- strictCheck -- Be extra strict in the formatting of existing headers, defaults to false
- sourceSets -- List of sourceSets to create tasks for, will default to all sourceSets created by Java Plugin
- mapping(String ext, String style) -- Adds a mapping between a file extension and a style type
- mapping(Map<String,String> mappings) -- Adds mappings between file extensions and style types
- mapping(Closure) -- Adds mappings between file extensions and a style types, see example below

### License Extension
A license extension is added to the project, which can be used to configure all license tasks. E.g.
 
```
license {
    header rootProject.file('codequality/HEADER')
    strictCheck true
}
```

Here is a general overview of the options:

- header -- Specify location of header to use in comparisons, default to project.file('LICENSE')
- ignoreFailures -- Prevent tasks from stopping the build, defaults to false
- dryRun -- Show what would happen if the task was run, defaults to false but also inherits from --dryRun
- skipExistingHeaders -- Skip over files that have some header already, which might not be the one specified in the header parameter, defaults to false
- useDefaultMappings -- Use a long list of standard mapping, defaults to true. See http://code.google.com/p/maven-license-plugin/wiki/Configuration#Default_mappings for the complete list
- strictCheck -- Be extra strict in the formatting of existing headers, defaults to false
- sourceSets -- List of sourceSets to create tasks for, will default to all sourceSets created by Java Plugin
- mapping(String ext, String style) -- Adds a mapping between a file extension and a style type
- mapping(Map<String,String> mappings) -- Adds mappings between file extensions and style types
- mapping(Closure) -- Adds mappings between file extensions and a style types, see example below
- exclude(String pattern) -- Add an ANT style pattern to exclude files from license absence reporting and license application
- exclude(Collection<String> patterns) -- Add ANT style patterns to exclude files from license absence reporting and license application

### File Types
Supported by default: java, groovy, js, css, xml, dtd, xsd, html, htm, xsl, fml, apt, properties, sh, txt, bat, cmd, sql, jsp, ftl, xhtml, vm, jspx, gsp, json. Complete list can be found in <a href="http://code.google.com/p/maven-license-plugin/wiki/SupportedFormats">SupportedFormats</a> page of the parent project.

### Recognizing other file types.
An extensive list of formats and mappings are available by default, see the SupportedFormats link above. Occasionally a project might need to add a mapping to a unknown file type to an existing comment style.

```
license {
    mapping {
        javascript='JAVADOC_STYLE'
    }
}
// or
license.mapping 'javascript' 'JAVADOC_STYLE'
// or directly on the task
licenseMain.mapping 'javascript' 'JAVADOC_STYLE'
```

Defining new comment types is not currently supported, but file a bug and it can be added.

### Variable substitution
Variables in the format ${} format will be substituted, as long as they're values are provided in the extension or the task.

    Copyright (C) ${year} ${name} <${email}>

Will be completed with this extension block, the key is adding them via extra properties:

```
license {
    ext.year = Calendar.getInstance().get(Calendar.YEAR)
    ext.name = 'Company'
    ext.email = 'support@company.com'
}
// or
licenseMain.ext.year = 2012
```

### Exclude files from license absence reporting and license application
By default all files in the sourceSets configured are required to carry a license. In order to exclude certain file(-types), you can add exclusion patterns.

The following sample will exclude all properties, txt and conf files.
```
license {
    exclude "**/*.properties"
    excludes(["**/*.txt", "**/*.conf"])
}
```

## License Reporting
The downloadLicense task has a set of properties, most can be set in the extension:

- includeProjectDependencies -- true if you want to include the transitive dependencies of your project dependencies
- licenses -- a pre-defined mapping of a dependency to a license; useful if the external repositories do not have license information available
- aliases -- a mapping between licenses; useful to consolidate the various POM definitions of different spelled/named licenses
- excludeDependencies -- a List of dependencies that are to be excluded from reporting
- dependencyConfiguration -- Gradle dependency configuration to report on (defaults to "runtime").

A 'license()' method is made available by the License Extension that takes two Strings, the first is the license name, the second is the URL to the license.
```
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
```

# Changelog

## v0.9.0
- Fixed build to force Java6 (Fixes [#35](https://github.com/hierynomus/license-gradle-plugin/issues/35))
- Added example test for [#38](https://github.com/hierynomus/license-gradle-plugin/issues/38)

## v0.8.0
- Merged pull-requests [#31](https://github.com/hierynomus/license-gradle-plugin/pull/31), [#33](https://github.com/hierynomus/license-gradle-plugin/pull/33), [#42](https://github.com/hierynomus/license-gradle-plugin/pull/42)
- 
