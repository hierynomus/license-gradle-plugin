/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package nl.javadude.gradle.plugins.license.tasks

import org.gradle.api.GradleException
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.TaskAction
import nl.javadude.gradle.plugins.license.types.HashFormat
import nl.javadude.gradle.plugins.license.types.SlashStarFormat

class LicenseTask extends AbstractLicenseTask {
	Map licenses = [:]

	def LicenseTask() {
	}

	def convertAllLicenseTypes(List<java.lang.String> license) {
		project.licenseTypes.each { k, v ->
			def format = project.formatters[v]
			licenses[v] = format.transform(license)
		}
	}

	List<java.lang.String> loadLicense() {
		def license = project.convention.plugins.license.license
		if (!license.exists()) {
			throw new GradleException("The license file [" + license + "] does not exist.")
		}
		license.readLines()
	}

	@TaskAction
	protected void process() {
		init()

		toBeLicensed = scanForFiles()
		toBeLicensed.findAll({ shouldBeLicensed it }).each { file ->
			handleFile(file)
		}
	}

    def getLicenseForFile(File file) {
        def ext = file.name.substring(file.name.indexOf('.') + 1)
        licenses.get(project.licenseTypes.get(ext))
    }


	def init() {
		List<String> license = loadLicense()
		convertAllLicenseTypes(license)
	}

	boolean shouldBeLicensed(File file) {
		def license = getLicenseForFile(file)
		!license.isLicensed(file)
	}

	def handleFile(File file) {
		println "Adding license on " + file
		def license = getLicenseForFile(file)
		def lines = file.readLines()
		file.delete()
		file.createNewFile()
		file.withWriter { w ->
			license.lines.each { line ->
				w.writeLine(line)
			}
			lines.each { line ->
				w.writeLine(line)
			}
			w.newLine()
		}
	}
}

