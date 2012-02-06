A collection of custom Gradle plugins...

# License Gradle Plugin
This plugin will scan and adapt all your sourcefiles to include your LICENSE in the header of the file.

By default it will scan and license files with the following extensions:

* scala
* java
* groovy
* properties

## Usage
In your _build.gradle_ file add:

	buildscript {
	  repositories {
	  	mavenCentral()
	  }
	
	  dependencies {
	    classpath 'nl.javadude.gradle.plugins:license-gradle-plugin:0.4'
	  }
	}

	apply plugin: 'license'

This will add two tasks to your project:

- license
- cleanLicense

## Recognizing other file types.
Besides the default recognized file types, you can configure the task to recognize other ones.

    registerLicense('txt', licenseFormat('#'))
    registerLicense('js', licenseFormat('/*', ' *', ' */'))

This will register the '*.txt' and '*.js' files to have a license. For '*.txt' files this will look like

    # license line 1
    # license line 2

And for '*.js' files it will be:

    /*
     * license line 1
     * license line 2
     */