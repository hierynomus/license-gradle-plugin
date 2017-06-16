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
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import static DependencyMetadata.noLicenseMetaData

/**
 * License resolver for dependencies.
 */
class LicenseResolver {

    private static final Logger logger = Logging.getLogger(LicenseResolver);

    /**
     * Reference to gradle project.
     */
    private Project project
    private Map<Object, Object> licenses
    private Map<LicenseMetadata, List<Object>> aliases
    private List<String> dependenciesToIgnore
    private boolean includeProjectDependencies
    private String dependencyConfiguration
    private boolean ignoreFatalParseErrors

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
        def subprojects = project.rootProject.subprojects.groupBy { Project p -> "$p.group:$p.name:$p.version".toString()}

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
                    if(dep.key instanceof String) {
                        dep.key == dependencyDesc
                    } else if (dep.key instanceof DependencyGroup) {
                        rd.moduleVersion.id.group == dep.key.group
                    }
                }
                if (licenseEntry != null) {
                    def license = licenseEntry.value
                    def licenseMetadata = license instanceof String ? DownloadLicensesExtension.license(license) : license
                    licenseSet << new DependencyMetadata(
                            dependency: dependencyDesc, dependencyFileName: rd.file.name, licenseMetadataList: [ licenseMetadata ]
                    )
                } else {
                    Closure<DependencyMetadata> dependencyMetadata = {
                        if(!subprojects[dependencyDesc]) {
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

            provideFileDependencies(p, dependenciesToIgnore).each {
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
                                            } else if(aliasElem instanceof LicenseMetadata) {
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
     * @param project                       the project
     * @param dependenciesToIgnore list of dependencies that will be excluded from the report
     * @return Set with resolved artifacts
     */
    Set<ResolvedArtifact> resolveProjectDependencies(Project project) {

        Set<ResolvedArtifact> dependenciesToHandle = new HashSet<ResolvedArtifact>()
        def subprojects = project.rootProject.subprojects.groupBy { Project p -> "$p.group:$p.name:$p.version".toString()}

        if (project.configurations.any { it.name == dependencyConfiguration }) {
            def configuration = project.configurations.getByName(dependencyConfiguration)
            configuration.resolvedConfiguration.resolvedArtifacts.each { ResolvedArtifact d ->
                String dependencyDesc = "$d.moduleVersion.id.group:$d.moduleVersion.id.name:$d.moduleVersion.id.version".toString()
                if(!matchExcludedDependency(dependencyDesc)) {
                    Project subproject = subprojects[dependencyDesc]?.first()
                    if (subproject) {
                        if(includeProjectDependencies) {
                            dependenciesToHandle.add(d)
                        }
                        dependenciesToHandle.addAll(resolveProjectDependencies(subproject))
                    } else if (!subproject) {
                        dependenciesToHandle.add(d)
                    }
                }
            }
        }

        logger.debug("Project $project.name found ${dependenciesToHandle.size()} dependencies to handle.")
        dependenciesToHandle
    }

    Set<String> provideFileDependencies(Project project, List<String> dependenciesToIgnore) {
        Set<String> fileDependencies = new HashSet<String>()

        if (project.configurations.any { it.name == dependencyConfiguration }) {
            Configuration configuration = project.configurations.getByName(dependencyConfiguration)

            Set<Dependency> d = configuration.allDependencies.findAll {
                it instanceof FileCollectionDependency
            }

            d.each {
                FileCollectionDependency fileDependency ->
                    fileDependency.resolve().each {
                        if (!dependenciesToIgnore.contains(it.name)) {
                            fileDependencies.add(it.name)
                        }
                    }
            }

        }

        logger.debug("Project $project.name found ${fileDependencies.size()} file dependencies to handle.")
        fileDependencies
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
     * @param project                       the project
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
                            } else if(aliasElem instanceof LicenseMetadata) {
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
            noLicenseMetaData(dependencyDesc)
        }
    }

    private boolean matchExcludedDependency(String dependencyDesc) {
       if (dependenciesToIgnore.contains(dependencyDesc)) {
           return true;
       } else {
           for (String exclude : dependenciesToIgnore) {
              if (exclude.endsWith("+")) {
                 int excludeLength = exclude.length() - 1;
                 if (dependencyDesc.substring(0, excludeLength).equals(exclude.substring(0, excludeLength))) {
                    return true;
                 }
              }
           }
       }

       return false;

    }
}
