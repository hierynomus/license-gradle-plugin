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

package nl.javadude.gradle.plugins.license

import org.gradle.api.Project
import org.gradle.api.Plugin
import nl.javadude.gradle.plugins.license.tasks.LicenseTask
import nl.javadude.gradle.plugins.license.tasks.UnlicenseTask
import nl.javadude.gradle.plugins.license.tasks.LicenseWarnTask

class LicensePlugin implements Plugin<Project> {
	void apply(Project project) {
		project.convention.plugins.license = new LicensePluginConvention(project)

		addLicenseTask(project)
	}

	private def addLicenseTask(Project project) {
		project.tasks.add(name: 'license', type: LicenseTask.class)
		project.tasks.add(name: 'licenseClean', type: UnlicenseTask.class)
		project.tasks.add(name: 'licenseWarn', type: LicenseWarnTask.class)
	}
}






