package nl.javadude.gradle.plugins.license

/**
 * Report container.
 */
class DownloadLicensesReportExtension {
    LicensesReportXml xml = new LicensesReportXml()
    LicensesReportHtml html = new LicensesReportHtml()
}
