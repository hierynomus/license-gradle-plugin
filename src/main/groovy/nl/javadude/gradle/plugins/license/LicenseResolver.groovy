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

import groovy.util.slurpersupport.GPathResult
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.util.regex.Pattern

import static DependencyMetadata.noLicenseMetaData

/**
 * License resolver for dependencies.
 */
class LicenseResolver {

    private static final Logger logger = Logging.getLogger(LicenseResolver)

    /**
     * Reference to gradle project.
     */
    private Project project
    private Map<Object, Object> licenses
    private Map<LicenseMetadata, List<Object>> aliases
    private boolean includeProjectDependencies
    private String dependencyConfiguration
    private boolean ignoreFatalParseErrors
    private List<Pattern> patternsToIgnore

    protected static final String LOCAL_LIBRARY_VERSION = "unspecified"
    private static final String TEST_PREFIX = "test"
    private static final String ANDROID_TEST_PREFIX = "androidTest"
    private static final Set<String> TEST_COMPILE = ["testCompile", "androidTestCompile"]
    private static final Set<String> PACKAGED_DEPENDENCIES_PREFIXES = ["compile", "implementation", "api"]

    /**
     * Provide set with dependencies metadata.
     *
     * For cases when we have no license information we try to use licenses file that can contains licenses.
     * Otherwise we put 'No license was found' into report and group dependencies without licenses.
     *
     * @return set with licenses
     */
    public Set<DependencyMetadata> provideLicenseMap4Dependencies() {
        Set<DependencyMetadata> licenseSet = new HashSet<DependencyMetadata>()
        def subprojects = project.rootProject.subprojects.groupBy { Project p -> "$p.group:$p.name:$p.version".toString() }

        Set<Project> projects = new HashSet<Project>()
        projects.add(project)
        projects.addAll(project.subprojects)

        projects.each {
            p ->

                // Resolve each dependency
                resolveProjectDependencies(p).each {
                    rd ->
                        String dependencyDesc = "$rd.moduleVersion.id.group:$rd.moduleVersion.id.name:$rd.moduleVersion.id.version".toString()
                        Map.Entry licenseEntry = licenses.find {
                            dep ->
                                if (dep.key instanceof String) {
                                    dep.key == dependencyDesc
                                } else if (dep.key instanceof DependencyGroup) {
                                    rd.moduleVersion.id.group == dep.key.group
                                }
                        }
                        if (licenseEntry != null) {
                            def license = licenseEntry.value
                            def licenseMetadata = license instanceof String ? DownloadLicensesExtension.license(license) : license
                            licenseSet << new DependencyMetadata(
                                    dependency: dependencyDesc, dependencyFileName: rd.file.name, licenseMetadataList: [licenseMetadata]
                            )
                        } else {
                            Closure<DependencyMetadata> dependencyMetadata = {
                                if (!subprojects[dependencyDesc]) {
                                    def depMetadata = retrieveLicensesForDependency(p, dependencyDesc)
                                    depMetadata.dependencyFileName = rd.file.name
                                    depMetadata
                                } else {
                                    noLicenseMetaData(dependencyDesc, rd.file.name)
                                }
                            }

                            licenseSet << dependencyMetadata()
                        }
                }

                provideFileDependencies(p).each {
                    fileDependency ->
                        Closure<DependencyMetadata> licenseMetadata = {
                            if (licenses.containsKey(fileDependency)) {
                                def license = licenses[fileDependency]
                                LicenseMetadata licenseMetadata = license instanceof String ? DownloadLicensesExtension.license(license) : license
                                def alias = aliases.find {
                                    aliasEntry ->
                                        aliasEntry.value.any {
                                            aliasElem ->
                                                if (aliasElem instanceof String) {
                                                    return aliasElem == licenseMetadata.licenseName
                                                } else if (aliasElem instanceof LicenseMetadata) {
                                                    return aliasElem == licenseMetadata
                                                }

                                        }
                                }
                                if (alias) {
                                    licenseMetadata = alias.key
                                }
                                new DependencyMetadata(dependency: fileDependency, dependencyFileName: fileDependency, licenseMetadataList: [licenseMetadata])
                            } else {
                                noLicenseMetaData(fileDependency, fileDependency)
                            }
                        }

                        licenseSet << licenseMetadata()
                }
        }

        licenseSet
    }

    /**
     * Provide full list of resolved artifacts to handle for a given project.
     *
     * @param project the project
     * @return Set with resolved artifacts
     */
    Set<ResolvedArtifact> resolveProjectDependencies(Project project) {
        Set<ResolvedArtifact> dependenciesToHandle = new HashSet<ResolvedArtifact>()

        if (project.configurations.any { it.name == dependencyConfiguration }) {
            def configuration = project.configurations.getByName(dependencyConfiguration)

            if (!isResolvable(configuration) || isTest(configuration) || !isPackagedDependency(configuration)) {
                println(project.name + " -> " + configuration.name + " -> no no no")
            } else {
                try {
                    Set<ResolvedArtifact> deps = getResolvedArtifactsFromResolvedDependencies(
                            configuration.getResolvedConfiguration()
                                    .getLenientConfiguration()
                                    .getFirstLevelModuleDependencies())

                    dependenciesToHandle.addAll(deps)

                    println(project.name + " -> " + configuration.name + " -> " + deps.size())

                } catch (ResolveException exception) {
                    logger.warn("Failed to resolve OSS licenses for $configuration.name.", exception)
                }
            }
        }

//        project.configurations.each { Configuration configuration ->
//            if (!isResolvable(configuration) || isTest(configuration) || !isPackagedDependency(configuration)) {
//                println(configuration.name + " ->  no no no")
//            } else {
//                try {
//                    Set<ResolvedArtifact> deps = getResolvedArtifactsFromResolvedDependencies(
//                            configuration.getResolvedConfiguration()
//                                    .getLenientConfiguration()
//                                    .getFirstLevelModuleDependencies())
//
//                    println(configuration.name + " -> " + deps.size())
//
//                    dependenciesToHandle.addAll(deps)
//
//                } catch (ResolveException exception) {
//                    logger.warn("Failed to resolve OSS licenses for $configuration.name.", exception)
//                }
//            }
//        }

        /*
        def subprojects = project.rootProject.subprojects.groupBy { Project p -> "$p.group:$p.name:$p.version".toString() }

        if (project.configurations.any {
            it.name == dependencyConfiguration && (isResolvable(it) || isPackagedDependency(it))
        }) {
            def configuration = project.configurations.getByName(dependencyConfiguration)

            configuration.resolvedConfiguration.resolvedArtifacts.each { ResolvedArtifact d ->
                String dependencyDesc = "$d.moduleVersion.id.group:$d.moduleVersion.id.name:$d.moduleVersion.id.version".toString()
                if (isDependencyIncluded(dependencyDesc)) {
                    Project subproject = subprojects[dependencyDesc]?.first()
                    if (subproject) {
                        if (includeProjectDependencies) {
                            dependenciesToHandle.add(d)
                        }
                        dependenciesToHandle.addAll(resolveProjectDependencies(subproject))
                    } else if (!subproject) {
                        dependenciesToHandle.add(d) `
                    }
                }
            }
        }
        */

        logger.debug("Project $project.name found ${dependenciesToHandle.size()} dependencies to handle.")
        dependenciesToHandle
    }

    protected Set<ResolvedArtifact> getResolvedArtifactsFromResolvedDependencies(Set<ResolvedDependency> resolvedDependencies) {
        HashSet<ResolvedArtifact> resolvedArtifacts = new HashSet<>()

        for (resolvedDependency in resolvedDependencies) {
            try {
                if (resolvedDependency.getModuleVersion() == LOCAL_LIBRARY_VERSION) {
                    /**
                     * Attempting to getAllModuleArtifacts on a local library project will result
                     * in AmbiguousVariantSelectionException as there are not enough criteria
                     * to match a specific variant of the library project. Instead we skip the
                     * the library project itself and enumerate its dependencies.
                     */
                    resolvedArtifacts.addAll(getResolvedArtifactsFromResolvedDependencies(resolvedDependency.getChildren()))
                } else {
                    resolvedArtifacts.addAll(resolvedDependency.getAllModuleArtifacts())
                }
            } catch (Exception exception) {
                logger.warn("Failed to process $resolvedDependency.name", exception)
            }
        }
        return resolvedArtifacts
    }


    Set<String> provideFileDependencies(Project project) {
        Set<String> fileDependencies = new HashSet<String>()

        if (project.configurations.any { it.name == dependencyConfiguration }) {
            Configuration configuration = project.configurations.getByName(dependencyConfiguration)

            Set<Dependency> d = configuration.allDependencies.findAll {
                it instanceof FileCollectionDependency
            }

            d.each {
                FileCollectionDependency fileDependency ->
                    fileDependency.resolve().each {
                        if (isDependencyIncluded(it.name)) {
                            fileDependencies.add(it.name)
                        }
                    }
            }

        }

        logger.debug("Project $project.name found ${fileDependencies.size()} file dependencies to handle.")
        fileDependencies
    }

    /**
     * Since Gradle 3.4, configurations can be marked as not resolvable by default.
     * Configuration#isCanBeResolved() from Gradle 3.3 can be used to check that.
     * @param conf Configuration
     * @return whether conf is resolvable
     *
     * @see <ahref="https://docs.gradle.org/3.4/release-notes.html#configurations-can-be-unresolvable"           >           Gradle 3.4 release notes</a>
     */
    boolean isResolvable(Configuration conf) {
        return conf.metaClass.respondsTo(conf, "isCanBeResolved") ? conf.isCanBeResolved() : true
    }

    /**
     * Checks if the configuration is from test.
     * @param configuration
     * @return true if configuration is a test configuration or its parent
     * configurations are either testCompile or androidTestCompile, otherwise
     * false.
     */
    protected boolean isTest(Configuration configuration) {
        boolean isTestConfiguration = (configuration.name.startsWith(TEST_PREFIX) || configuration.name.startsWith(ANDROID_TEST_PREFIX))
        configuration.hierarchy.each { isTestConfiguration |= TEST_COMPILE.contains(it.name) }
        return isTestConfiguration
    }

    /**
     * Checks if the configuration is for a packaged dependency (rather than e.g. a build or test time dependency)
     * @param configuration
     * @return true if the configuration is in the set of @link #BINARY_DEPENDENCIES
     */
    protected boolean isPackagedDependency(Configuration configuration) {
        boolean isPackagedDependency = PACKAGED_DEPENDENCIES_PREFIXES.any {
            configuration.name.startsWith(it)
        }
        configuration.hierarchy.each {
            String configurationHierarchyName = it.name
            isPackagedDependency |= PACKAGED_DEPENDENCIES_PREFIXES.any {
                configurationHierarchyName.startsWith(it)
            }
        }

        return isPackagedDependency
    }


    boolean isDependencyIncluded(String depName) {
        for (Pattern pattern : this.patternsToIgnore) {
            if (pattern.matcher(depName).matches()) {
                return false;
            }
        }
        return true;
    }


    /**
     * Recursive function for retrieving licenses via creating
     * and resolving detached configuration with "pom" extension.
     *
     * If no license was found in pom, we try to find it in parent pom declaration.
     * Parent pom descriptors are resolved in recursive way until we have no parent.
     *
     * Implementation note: We rely that while resolving configuration with one dependency we get one pom.
     * Otherwise we have IllegalStateException
     *
     * @param project the project
     * @param dependencyDesc dependency description
     * @param aliases alias mapping for similar license names
     * @param initialDependency base dependency (not parent)
     * @return dependency metadata, includes license info
     */
    private DependencyMetadata retrieveLicensesForDependency(Project project,
                                                             String dependencyDesc,
                                                             String initialDependency = dependencyDesc) {
        Dependency d = project.dependencies.create("$dependencyDesc@pom")
        Configuration pomConfiguration = project.configurations.detachedConfiguration(d)

        File pStream
        try {
            pStream = pomConfiguration.resolve().asList().first()
        } catch (ResolveException) {
            logger.warn("Unable to retrieve license for $dependencyDesc")
            return noLicenseMetaData(dependencyDesc)
        }

        XmlSlurper slurper = new XmlSlurper(true, false)
        slurper.setErrorHandler(new org.xml.sax.helpers.DefaultHandler())

        GPathResult xml
        try {
            xml = slurper.parse(pStream)
        } catch (org.xml.sax.SAXParseException e) {
            // Fatal errors are still throw by DefaultHandler, so handle them here.
            logger.warn("Unable to parse POM file for $dependencyDesc")
            if (ignoreFatalParseErrors) {
                return noLicenseMetaData(dependencyDesc)
            } else {
                throw e
            }
        }

        DependencyMetadata pomData = new DependencyMetadata(dependency: initialDependency)

        xml.licenses.license.each {
            def license = new LicenseMetadata(licenseName: it.name.text().trim(), licenseTextUrl: it.url.text().trim())
            def alias = aliases.find {
                aliasEntry ->
                    aliasEntry.value.any {
                        aliasElem ->
                            if (aliasElem instanceof String) {
                                return aliasElem == license.licenseName
                            } else if (aliasElem instanceof LicenseMetadata) {
                                return aliasElem == license
                            }

                    }
            }
            if (alias) {
                license = alias.key
            }
            pomData.addLicense(license)
        }

        if (pomData.hasLicense()) {
            pomData
        } else if (xml.parent.text()) {
            String parentGroup = xml.parent.groupId.text().trim()
            String parentName = xml.parent.artifactId.text().trim()
            String parentVersion = xml.parent.version.text().trim()

            retrieveLicensesForDependency(project, "$parentGroup:$parentName:$parentVersion", initialDependency)
        } else {
            noLicenseMetaData(initialDependency)
        }
    }

    void setDependenciesToIgnore(List<String> dependenciesToIgnore) {
        if (dependenciesToIgnore == null) {
            this.patternsToIgnore = Collections.emptyList();
            return;
        }

        this.patternsToIgnore = new ArrayList<>(dependenciesToIgnore.size());
        for (String toIgnore : dependenciesToIgnore) {
            this.patternsToIgnore.add(Pattern.compile(toIgnore))
        }
    }
}
