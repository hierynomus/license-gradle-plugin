/* License added by: GRADLE-LICENSE-PLUGIN
 *
 * Copyright 2011 Justin Ryan <jryan@netflix.com>
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

import java.io.File;


import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.SourceSet

/**
 * Extension in the license namespace, which drives the License tasks.
 * 
 * @author jryan
 */
class LicenseExtension {
    /**
     * Header to apply to files
     */
    File header

    /**
     * Source sets to perform search on, will default to all sourceSets in the project
     */
    Collection<SourceSet> sourceSets // Probably should be final SourceSetContainer, so that it doesn't turn out null at anytime

    /**
     * Whether or not to allow the build to continue if there are warnings.
     */
    boolean ignoreFailures

    /**
     * Whether to create new files which have changes or to make them inline
     * 
     */
    boolean dryRun;

    /**
     * Whether to skip file where a header has been detected
     */
    boolean skipExistingHeaders;

    /**
     * @link {AbstractLicenseMojo.useDefaultMappings}
     */
    boolean useDefaultMappings
    
    boolean strictCheck

    Map<String, String> internalMappings = new HashMap<String, String>();
    public void mapping(String fileType, String headerType) {
        internalMappings.put(fileType, headerType);
    }

    public void mapping(Map<String, String> provided) {
        internalMappings.putAll(provided);
    }

    public void mapping(Closure closure) {
        Map<String,String> tmpMap = new HashMap<String,String>()
        closure.delegate = tmpMap;
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure();
        internalMappings.putAll(tmpMap);
    }

}
