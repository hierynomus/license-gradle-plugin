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

import java.io.File;
import java.util.List;

import org.gradle.api.tasks.TaskAction
import org.gradle.api.internal.ConventionTask
import org.gradle.api.GradleException

abstract class AbstractLicenseTask extends ConventionTask {
	def licenseCache = [:]
	List<String> licenseLines
	
	List<java.lang.String> loadLicense() {
		def license = project.convention.plugins.license.license
		if (!license.exists()) {
			throw new GradleException("The license file [" + license + "] does not exist.")
		}
		license.readLines()
	}

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
	
	def getLicenseForFile(File file) {
		def ext = getExtension(file)
		println this
		assert licenseLines != null
		if (!licenseCache[ext]) {
			format = project.licenseTypes[ext]
			licenseCache[ext] = format.transform(licenseLines)
		}
		licenseCache[ext]
	}

	boolean shouldBeLicensed(File file) {
		def license = getLicenseForFile(file)
		!license.isLicensed(file)
	}


    String getExtension(File file) {
        return file.name.substring(file.name.indexOf('.') + 1)
    }

}





