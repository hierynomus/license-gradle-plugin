package nl.javadude.gradle.plugins.license

/**
 * Report container.
 */
class DownloadLicensesReportExtension {
    LicensesReport xml = new LicensesReport()
    LicensesReport html = new LicensesReport()
    LicensesReport json = new LicensesReport()
}
