/* License added by: GRADLE-LICENSE-PLUGIN
 *
 * Copyright (C)2011 - Jeroen van Erp <jeroen@javadude.nl>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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





