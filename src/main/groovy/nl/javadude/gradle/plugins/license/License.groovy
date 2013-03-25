/* License added by: GRADLE-LICENSE-PLUGIN
 *
 * Copyright 2012 Justin Ryan <jryan@netflix.com>
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

import groovy.lang.Closure;

import java.util.HashMap;
import java.util.Map;

import nl.javadude.gradle.plugins.license.maven.CallbackWithFailure
import nl.javadude.gradle.plugins.license.maven.LicenseCheckMojo
import nl.javadude.gradle.plugins.license.maven.LicenseFormatMojo
import nl.javadude.gradle.plugins.license.maven.AbstractLicenseMojo
import com.google.common.collect.Lists

import org.gradle.api.GradleException
import org.gradle.api.tasks.StopActionException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationTask

/**
 * Task to back License. Using convention of naming Task types with just their name, which makes calls
 * like tasks.withType(License) nicer, consistent with most internal Gradle tasks.
 * 
 * TODO: See if removing headers is valuable to add in
 * 
 * @author jryan
 */
public class License extends SourceTask implements VerificationTask {
    /**
     * Whether or not to allow the build to continue if there are warnings.
     */
    boolean ignoreFailures

    /**
     * Check determines if we doing mutation on the files or just looking
     *
     */
    boolean check;

    /**
     * Whether to create new files which have changes or to make them inline
     * 
     */
    boolean dryRun = false;

    /**
     * Whether to skip file where a header has been detected
     */
    boolean skipExistingHeaders = false;

    // TODO useDefaultExcludes, not necessary because we're using source sets

    /**
     * @link {AbstractLicenseMojo.useDefaultMappings}
     */
    boolean useDefaultMappings
    
    boolean strictCheck
    
    @InputFile
    File header

    @OutputFiles
    Iterable<File> altered = Lists.newArrayList()

    // Backing AbstraceLicenseMojo
    FileCollection validHeaders;

    Map<String, String> inheritedProperties;
    Map<String, String> inheritedMappings;

    @TaskAction
    protected void process() {
        if (!enabled) {
            didWork = false;
            return;
        }

        CallbackWithFailure callback;
        if (check) {
            callback = new LicenseCheckMojo(getProject().rootDir)
        } else {
            callback = new LicenseFormatMojo(getProject().rootDir, dryRun, skipExistingHeaders)
        }

        Map<String,String> initial = combineVariables();
        Map<String, String> combinedMappings = combinedMappings();

        new AbstractLicenseMojo(validHeaders, getProject().rootDir, initial, isDryRun(), isSkipExistingHeaders(), isUseDefaultMappings(), isStrictCheck(), getHeader(), source, combinedMappings)
            .execute(callback);

        altered = callback.getAffected()
        didWork = !altered.isEmpty()

        if (!ignoreFailures && callback.hadFailure()) {
            throw new GradleException("License violations were found")
        }

    }

    // Setup up variables
    // Use properties on this task over the ones from the extension
    private Map combineVariables() {
        Map<String, String> initial = new HashMap<String, String>();
        if (getInheritedProperties() != null ) { // Convention will pull these from the extension
            initial.putAll(getInheritedProperties());
        }
        initial.putAll(ext.properties)
        return initial
    }

    // Setup mappings
    Map<String,String> combinedMappings() {
        Map<String, String> combinedMappings = new HashMap<String, String>();
        if (isUseDefaultMappings()) {
            // Sprinkle in some other well known types, which maven-license-plugin doesn't have
            combinedMappings.put('json', 'JAVADOC_STYLE')
            combinedMappings.put('scala', 'JAVADOC_STYLE')
            combinedMappings.put('gsp', 'DYNASCRIPT_STYLE')
            combinedMappings.put('groovy', 'SLASHSTAR_STYLE')
        }
        if (getInheritedMappings() != null) {
            combinedMappings.putAll(getInheritedMappings());
        }
        combinedMappings.putAll(internalMappings)
        return combinedMappings
    }

    Map<String, String> internalMappings = new HashMap<String, String>();
    public void mapping(String fileType, String headerType) {
        internalMappings.put(fileType, headerType);
    }

    public void mapping(Map<String, String> provided) {
        internalMappings.putAll(provided);
    }

    public void mapping(Closure closure) {
        Map<String,String> tmpMap = new HashMap<String,String>();
        closure.delegate = tmpMap;
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure();
        internalMappings.putAll(tmpMap);
    }
}

