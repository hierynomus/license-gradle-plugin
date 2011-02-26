package nl.javadude.gradle.plugins.license.tasks

import org.gradle.api.tasks.TaskAction
import org.gradle.api.internal.ConventionTask

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

    String getExtension(File file) {
        return file.name.substring(file.name.indexOf('.') + 1)
    }

}
