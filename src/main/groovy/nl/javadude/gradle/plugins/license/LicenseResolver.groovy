package nl.javadude.gradle.plugins.license

import groovy.util.slurpersupport.GPathResult
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import static com.google.common.base.Strings.isNullOrEmpty
import static com.google.common.collect.Sets.newHashSet
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

    /**
     * Provide set with dependencies metadata.
     *
     * For cases when we have no license information we try to use licenses file that can contains licenses.
     * Otherwise we put 'No license was found' into report and group dependencies without licenses.
     *
     * @return set with licenses
     */
    public Set<DependencyMetadata> provideLicenseMap4Dependencies() {
        Set<DependencyMetadata> licenseSet = newHashSet()
        def subprojects = project.rootProject.subprojects.groupBy { Project p -> "$p.group:$p.name:$p.version".toString()}

        // Resolve each dependency
        resolveProjectDependencies(project).each {
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
                        def depMetadata = retrieveLicensesForDependency(dependencyDesc)
                        depMetadata.dependencyFileName = rd.file.name
                        depMetadata
                    } else {
                        noLicenseMetaData(dependencyDesc, rd.file.name)
                    }
                }

                licenseSet << dependencyMetadata()
            }
        }

        provideFileDependencies(dependenciesToIgnore).each {
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

        Set<ResolvedArtifact> dependenciesToHandle = newHashSet()
        def subprojects = project.rootProject.subprojects.groupBy { Project p -> "$p.group:$p.name:$p.version".toString()}

        if (project.configurations.any { it.name == dependencyConfiguration }) {
            def runtimeConfiguration = project.configurations.getByName(dependencyConfiguration)
            runtimeConfiguration.resolvedConfiguration.resolvedArtifacts.each { ResolvedArtifact d ->
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

    Set<String> provideFileDependencies(List<String> dependenciesToIgnore) {
        Set<String> fileDependencies = newHashSet()

        if (project.configurations.any { it.name == dependencyConfiguration }) {
            Configuration configuration = project.configurations.getByName(dependencyConfiguration)

            Set<Dependency> d = configuration.allDependencies.findAll {
                it instanceof FileCollectionDependency
            }

            d.each {
                FileCollectionDependency fileDependency ->
                    fileDependency.source.files.each {
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
     * @param dependencyDesc dependency description
     * @param aliases alias mapping for similar license names
     * @param initialDependency base dependency (not parent)
     * @return dependency metadata, includes license info
     */
    private DependencyMetadata retrieveLicensesForDependency(String dependencyDesc,
                                                             String initialDependency = dependencyDesc) {
        Dependency d = project.dependencies.create("$dependencyDesc@pom")
        Configuration pomConfiguration = project.configurations.detachedConfiguration(d)

        File pStream = pomConfiguration.resolve().asList().first()
        GPathResult xml = new XmlSlurper(true, false).parse(pStream)
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
        } else if (!isNullOrEmpty(xml.parent.text())) {
            String parentGroup = xml.parent.groupId.text().trim()
            String parentName = xml.parent.artifactId.text().trim()
            String parentVersion = xml.parent.version.text().trim()

            retrieveLicensesForDependency("$parentGroup:$parentName:$parentVersion", initialDependency)
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
