package nl.javadude.gradle.plugins.license

import groovy.transform.Canonical

/**
 * Dependency metadata. Contains:
 * Dependency name, license metadata list.
 */
@Canonical
class DependencyMetadata {

    /**
     * Create Dependency metadata for dependencies without licenses.
     */
    public static final DependencyMetadata noLicenseMetaData(String dependencyName, String fileName = null) {
        return new DependencyMetadata(dependency: dependencyName, dependencyFileName: fileName,
                licenseMetadataList: [new LicenseMetadata(licenseName: "No license found")]
        )
    }

    /**
     * List with license metadata.
     */
    List<LicenseMetadata> licenseMetadataList = []

    /**
     * Dependency name.
     */
    String dependency

    /**
     * Dependency jar file name.
     */
    String dependencyFileName

    /**
     * Check whether metadata list is empty.
     *
     * @return license metadata list is empty or not
     */
    boolean hasLicense() {
       !licenseMetadataList.empty
    }

    /**
     * Add license.
     *
     * @param licenseMetadata license metadata to add
     */
    void addLicense(LicenseMetadata licenseMetadata) {
        licenseMetadataList.add(licenseMetadata)
    }

}
