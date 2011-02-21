A collection of custom Gradle plugins...

# License Gradle Plugin
This plugin will scan and adapt all your sourcefiles to include your LICENSE in the header of the file.

## Usage
In your _build.gradle_ file add:

	buildscript {
	  repositories {
	    add(new org.apache.ivy.plugins.resolver.URLResolver()) {
	      name = "GitHub"
	      addArtifactPattern 'http://github.com/hierynomus/gradle-plugins/downloads/[organization]-[module]-[revision].[ext]'
	    }
	  }
	
	  dependencies {
	    classpath 'nl.javadude.gradle.plugins:license-gradle-plugin:0.1-SNAPSHOT'
	  }
	}

	apply plugin: 'license'

This will add a `license` task to your project.
