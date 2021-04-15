/*
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

import com.mycila.maven.plugin.license.header.HeaderDefinition
import nl.javadude.gradle.plugins.license.header.HeaderDefinitionBuilder
import nl.javadude.gradle.plugins.license.maven.AbstractLicenseMojo
import nl.javadude.gradle.plugins.license.maven.CallbackWithFailure
import nl.javadude.gradle.plugins.license.maven.LicenseCheckMojo
import nl.javadude.gradle.plugins.license.maven.LicenseFormatMojo
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*

import javax.inject.Inject

/**
 * Task to back License. Using convention of naming Task types with just their name, which makes calls
 * like tasks.withType(License) nicer, consistent with most internal Gradle tasks.
 *
 * TODO: See if removing headers is valuable to add in
 *
 * @author jryan
 */
class License extends SourceTask implements VerificationTask {
    /**
     * Whether or not to allow the build to continue if there are warnings.
     */
    @Input boolean ignoreFailures

    /**
     * Check determines if we doing mutation on the files or just looking
     */
    @Input boolean check

    /**
     * Whether to create new files which have changes or to make them inline
     */
    @Input boolean dryRun = false

    /**
     * Whether to skip file where a header has been detected
     */
    @Input boolean skipExistingHeaders = false

    // TODO useDefaultExcludes, not necessary because we're using source sets

    /**
     * @link {AbstractLicenseMojo.useDefaultMappings}
     */
    @Input boolean useDefaultMappings

    @Input boolean strictCheck

    /**
     * The encoding used to open files
     */
    @Input String encoding

    @Optional
    @InputFile
    File header

    /**
     * In lieu of a header file on the local filesystem, this property lets you provide a URL that could be
     * in the classpath or on a remote system. When configured across a few projects, it would mean that a
     * header file doesn't have to be in each project.
     */
    @Optional
    @Input
    URI headerURI

    @OutputFiles
    Iterable<File> altered = new ArrayList<File>()

    // Backing AbstraceLicenseMojo
    @Optional
    @InputFiles
    FileCollection validHeaders

    @Optional
    @Input
    Map<String, String> inheritedProperties

    @Optional
    @Input Map<String, String> inheritedMappings

    // Container for all custom header definitions
    @Optional
    @Nested
    NamedDomainObjectContainer<HeaderDefinitionBuilder> headerDefinitions

    @Inject
    @Deprecated
    License() {
    }

    License(boolean check) {
        this.check = check
    }

    @TaskAction
    protected void process() {
        // Plain weird, but this ensures that the lazy closure from the extension is properly wired into the excludes field of the SourceTask.
        this.excludes = getExcludes()
        this.includes = getIncludes()

        if (!enabled) {
            didWork = false
            return
        }
        CallbackWithFailure callback
        if (isCheck()) {
            callback = new LicenseCheckMojo(getProject().rootDir, isSkipExistingHeaders())
        } else {
            callback = new LicenseFormatMojo(getProject().rootDir, isDryRun(), isSkipExistingHeaders())
        }

        Map<String, String> initial = combineVariables()
        Map<String, String> combinedMappings = combinedMappings()

        URI uri = resolveURI()

        new AbstractLicenseMojo(validHeaders, getProject().rootDir, initial, isDryRun(), isSkipExistingHeaders(), isUseDefaultMappings(), isStrictCheck(), uri, source, combinedMappings, getEncoding(), buildHeaderDefinitions())
            .execute(callback)

        altered = callback.getAffected()
        didWork = !altered.isEmpty()

        if (!isIgnoreFailures() && callback.hadFailure()) {
            throw new GradleException("License violations were found: ${callback.affected.join(',')}}")
        }

    }

    // Gradle thinks all getters should be associated with properties that must be annotated
    // renamed as @Internal is not available in Gradle 2.x
    URI resolveURI() {
        def uri = getHeaderURI() ?: getHeader().toURI()
        if (!uri) {
            throw new GradleException("A headerUri or header has to be provided to the License task")
        }
        uri
    }

    // Setup up variables
    // Use properties on this task over the ones from the extension
    private Map combineVariables() {
        Map<String, String> initial = new HashMap<String, String>()
        if (getInheritedProperties() != null ) { // Convention will pull these from the extension
            initial.putAll(getInheritedProperties())
        }
        initial.putAll(ext.properties)
        return initial
    }

    // Setup mappings
    Map<String,String> combinedMappings() {
        Map<String, String> combinedMappings = new HashMap<String, String>()
        if (isUseDefaultMappings()) {
            // Sprinkle in some other well known types, which maven-license-plugin doesn't have
            combinedMappings.put('gradle', 'JAVADOC_STYLE')
            combinedMappings.put('json', 'JAVADOC_STYLE')
            combinedMappings.put('scala', 'JAVADOC_STYLE')
            combinedMappings.put('gsp', 'DYNASCRIPT_STYLE')
            combinedMappings.put('groovy', 'SLASHSTAR_STYLE')
            combinedMappings.put('clj', 'SEMICOLON_STYLE')
            combinedMappings.put('yaml', 'SCRIPT_STYLE')
            combinedMappings.put('yml', 'SCRIPT_STYLE')
        }
        if (getInheritedMappings() != null) {
            combinedMappings.putAll(getInheritedMappings())
        }
        combinedMappings.putAll(internalMappings)
        return combinedMappings
    }

    List<HeaderDefinition> buildHeaderDefinitions() {
        List<HeaderDefinition> definitions = new ArrayList<>()
        getHeaderDefinitions().all { headerDefinition ->
            logger.debug("Adding extra header definition ${headerDefinition.toString()}")
            definitions.add(headerDefinition.build())
        }

        return definitions
    }

    @Internal
    Map<String, String> internalMappings = new HashMap<String, String>()

    void mapping(String fileType, String headerType) {
        internalMappings.put(fileType, headerType)
    }

    void mapping(Map<String, String> provided) {
        internalMappings.putAll(provided)
    }

    void mapping(Closure closure) {
        Map<String,String> tmpMap = new HashMap<String,String>()
        closure.delegate = tmpMap
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
        internalMappings.putAll(tmpMap)
    }
}
