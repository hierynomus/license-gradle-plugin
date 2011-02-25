package nl.javadude.gradle.plugins.license.tasks

import org.gradle.api.tasks.TaskAction
import org.gradle.api.internal.ConventionTask
import nl.javadude.gradle.plugins.license.types.HashFormat
import nl.javadude.gradle.plugins.license.types.SlashStarFormat

abstract class AbstractLicenseTask extends ConventionTask {

    def scanForFiles() {
        List<File> toBeLicensed = []
        project.sourceSets.each { set ->
            set.allSource.each { file ->
                def ext = file.name.substring(file.name.indexOf('.') + 1)
                if (project.licenseTypes.containsKey(ext)) {
                    toBeLicensed.add file
                }
            }
        }
        toBeLicensed
    }
}
