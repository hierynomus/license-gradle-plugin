package nl.javadude.gradle.plugins.license

import org.gradle.util.ConfigureUtil

/**
 * Extension contains attributes for {@link DownloadLicenses}.
 */
class DownloadLicensesExtension {

    /**
     * Custom license mapping.
     */
    Map<Object, Object> licenses

    /**
     * License aliases.
     */
    Map<Object, List<Object>> aliases

    /**
     * List of dependencies that will be omitted in the report.
     */
    List<String> excludeDependencies

    /**
     * Generate report for each dependency.
     */
    boolean reportByDependency

    /**
     * Generate report for each license type.
     */
    boolean reportByLicenseType

    /**
     * Include project dependencies in reports.
     */
    boolean includeProjectDependencies

    /**
     * Ignore fatal errors when parsing POMs of transitive dependencies.
     */
    boolean ignoreFatalParseErrors

    /**
     * File name for reports by dependency.
     */
    String reportByDependencyFileName

    /**
     * File name for reports by license.
     */
    String reportByLicenseFileName

    /**
     * Generate xml report.
     */
    boolean xml

    /**
     * Generate html report.
     */
    boolean html

    /**
     * Generate json report.
     */
    boolean json


    /**
     * The dependency configuration to report on.
     */
    String dependencyConfiguration

    /**
     * Report extension.
     */
    DownloadLicensesReportExtension report = new DownloadLicensesReportExtension()

    /**
     * Create instance of license metadata with specified name and url (optional).
     *
     * @param name license name
     * @param url URL for license text
     * @return license meta data instance
     */
    static LicenseMetadata license(name, url = null) {
        new LicenseMetadata(name, url)
    }

    /**
     * Configure report container.
     *
     * @param closure configuring closure
     */
    def report(Closure closure) {
        ConfigureUtil.configure(closure, report)
    }

    def static group(String group) {
        return new DependencyGroup(group: group)
    }
}
