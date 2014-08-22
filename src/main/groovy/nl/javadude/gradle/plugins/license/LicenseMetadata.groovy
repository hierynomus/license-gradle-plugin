package nl.javadude.gradle.plugins.license

import groovy.transform.Canonical

/**
 * License metadata. Includes name and text url.
 */
@Canonical
class LicenseMetadata implements Serializable {

    /**
     * License name.
     */
    String licenseName

    /**
     * URL with license text.
     */
    String licenseTextUrl
}
