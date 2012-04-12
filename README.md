# License Gradle Plugin
This plugin will scan and adapt your source files to include a provided header, e.g. a LICENSE file.  By default it will scan every source set and report warnings. It will also create format tasks, which will properly format and apply the specified header. A bulk of the logic comes from the maven-license-plugin.

## Usage
In your _build.gradle_ file add:

```
	buildscript {
	  repositories {
	  	mavenCentral()
	  }
	
	  dependencies {
	    classpath 'nl.javadude.gradle.plugins:license-gradle-plugin:0.5'
	  }
	}

	apply plugin: 'license'
```

This will add two types of tasks for each source set in your project, one for checking for consistency and one to apply the header, e.g.

- licenseMain        : checks for header consistency in the main source set
- licenseFormatMain  : applies the license found in the header file in files missing the header
- licenseTest        : checks for header consistency in the test source set

The check tasks are added to the standard Gradle _check_ task.

## License Extension
A license extension is added to the project, which can be used to configure all license tasks. E.g.
 
```
license {
    header rootProject.file('codequality/HEADER')
    strictCheck true
}
```

Here is a general overview of the options:

- header -- Specify location of header to use in comparisons, default to profile.file('LICENSE')
- ignoreFailures -- Prevent tasks from stopping the build, defaults to false
- dryRun -- Show what would happen if the task was run, defaults to false but also inherits from --dryRun
- skipExistingHeaders -- Skip over files that have some header already, which might not be the one specified in the header parameter, defaults to false
- useDefaultMappings -- Use a long list of standard mapping, defaults to true. See http://code.google.com/p/maven-license-plugin/wiki/Configuration#Default_mappings for the complete list
- strictCheck -- Be extra strict in the formatting of existing headers, defaults to false
- sourceSets -- List of sourceSets to create tasks for, will default to all sourceSets created by Java Plugin

## File Types
Supported by default: java, groovy, js, css, xml, dtd, xsd, html, htm, xsl, fml, apt, properties, sh, txt, bat, cmd, sql, jsp, ftl, xhtml, vm, jspx. Complete list can be found in <a href="http://code.google.com/p/maven-license-plugin/wiki/SupportedFormats">SupportedFormats</a> page of the parent project.

## Recognizing other file types.
An extensive list of formats and mappings are available by default, see the SupportedFormats link above. Occassionaly a project might need to add a mapping to a unknown file type to an existing comment style.

```
license {
    mapping {
        javascript='JAVA_STYLE'
    }
}
// or
license.mapping 'javascript' 'JAVA_STYLE'
// or directly on the task
licenseMain.mapping 'javascript' 'JAVA_STYLE'
```

Defining new comment types is not currently supported, but file a bug and it can be added.

## Variable substitution
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
