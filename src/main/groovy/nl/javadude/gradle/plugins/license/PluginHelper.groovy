package nl.javadude.gradle.plugins.license

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project

class PluginHelper {

    static void withOptionalPlugin(String pluginClassName, Project project, Action<? extends Plugin> configureAction) {
        try {
            def pluginClass = Class.forName(pluginClassName)
            // Will most likely throw a ClassNotFoundException
            project.plugins.withType(pluginClass, configureAction)
        } catch(ClassNotFoundException nfe) {
            // do nothing
        }
    }

    static void withAndroidPlugin(Project project, Action<? extends Plugin> configureAction) {
        ['com.android.build.gradle.AppPlugin', 'com.android.build.gradle.LibraryPlugin'].each { c ->
            withOptionalPlugin(c, project, configureAction)
        }

    }
}
