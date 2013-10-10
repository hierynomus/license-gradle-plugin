package nl.javadude.gradle.plugins.license

import groovy.transform.Canonical

/**
 * Dependency group representation.
 */
@Canonical
class DependencyGroup implements Serializable {
    String group
}
