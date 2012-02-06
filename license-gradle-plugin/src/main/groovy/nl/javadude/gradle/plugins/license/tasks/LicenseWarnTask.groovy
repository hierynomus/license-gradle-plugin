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

import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class LicenseWarnTask extends AbstractLicenseTask {
	
    @TaskAction
    protected void process() {
        licenseLines = loadLicense()
		licenseWarn()
    } // end method

	def licenseWarn() {
		def toBeLicensed = scanForFiles()
		def needsLicense = false;
		toBeLicensed.findAll({ shouldBeLicensed it }).each { file ->
			println file;
			needsLicense = true;
		} // end each
		if( needsLicense ) {
			throw new GradleException("Break the build")
		} // end if
	} // end method
} // end class






