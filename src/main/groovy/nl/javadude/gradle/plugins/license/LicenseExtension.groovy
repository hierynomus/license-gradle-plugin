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

import nl.javadude.gradle.plugins.license.header.HeaderDefinitionBuilder
import org.gradle.api.DomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
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
     * URI to header to apply to files
     */
    URI headerURI

    /**
     * Source sets to perform search on, will default to all sourceSets in the project
     * @deprecated Is no longer used.
     */
    DomainObjectCollection<SourceSet> sourceSets

    /**
     * Path patterns to exclude while applying licenses or reporting missing licenses
     */
    Collection<String> excludePatterns = new HashSet<String>()

    /**
     * Path patterns to include while applying licenses or reporting missing licenses
     */
    Collection<String> includePatterns = new HashSet<String>()

    /**
     * Whether or not to allow the build to continue if there are warnings.
     */
    boolean ignoreFailures

    /**
     * Whether to create new files which have changes or to make them inline
     *
     */
    boolean dryRun

    /**
     * Whether to skip file where a header has been detected
     */
    boolean skipExistingHeaders

    /**
     * @link {AbstractLicenseMojo.useDefaultMappings}
     */
    boolean useDefaultMappings

    /**
     * Specify the list of keywords to use to detect a header. A header must
     * include all keywords to be valid. Detection is case insensitive.
     */
    Collection<String> keywords

    boolean strictCheck

    /**
     * Additional header definitions
     */
    NamedDomainObjectContainer<HeaderDefinitionBuilder> headerDefinitions

    /**
     * The encoding used for opening files. It is the system encoding by default
     */
    String encoding

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

    void exclude(String pattern) {
      excludePatterns.add(pattern)
    }

    void excludes(Collection<String> patterns) {
      excludePatterns.addAll(patterns)
    }

    void include(String pattern) {
      includePatterns.add(pattern)
    }

    void includes(Collection<String> patterns) {
      includePatterns.addAll(patterns)
    }

    def headerDefinitions(final Closure configureHeaderDefinition) {
      headerDefinitions.configure(configureHeaderDefinition)
    }

    void headerDefinition(HeaderDefinitionBuilder definition) {
      headerDefinitions.add(definition)
    }
}
