package nl.javadude.gradle.plugins.license.tasks

import org.gradle.api.tasks.TaskAction

class UnlicenseTask extends AbstractLicenseTask {
    boolean force = false

    @TaskAction
    protected void process() {
        def files = scanForFiles()
        if (!force) {
            files = files.findAll({ project.licenseTypes[getExtension(it)]?.isLicensedByPlugin(it) })
        }
        files.each { removeLicense(it) }
    }

    def removeLicense(File file) {
        println "Removing license from file ${file}"
        def lines = file.readLines()
        def format = project.licenseTypes[getExtension(file)]
        def unlicensedFileLines = format.removeLicenseBlock(lines)
        file.withWriter { writer ->
            unlicensedFileLines.each {
                writer.writeLine(it)
            }
        }
    }
}
