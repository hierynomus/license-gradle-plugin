package nl.javadude.gradle.plugins.license

import groovy.xml.MarkupBuilder

/**
 * License file reporter.
 */
class LicenseReporter {

    /**
     * Output directory for html reports.
     */
    File htmlOutputDir

    /**
     * Output directory for xml reports.
     */
    File xmlOutputDir

    /**
     * Generate xml report grouping by dependencies.
     *
     * @param dependencyMetadataSet set with dependencies
     * @param fileName report file name
     */
    public void generateXMLReport4DependencyToLicense(Set<DependencyMetadata> dependencyMetadataSet, String fileName) {
        PrintWriter writer = new PrintWriter(new File(xmlOutputDir, fileName))
        MarkupBuilder xml = new MarkupBuilder(writer)

        xml.dependencies() {
            dependencyMetadataSet.each {
                entry ->
                    dependency(name: entry.dependency) {
                        file(entry.dependencyFileName)
                        entry.licenseMetadataList.each {
                            l ->
                                def attributes = [name: l.licenseName]

                                // Miss attribute if it's empty
                                if (l.licenseTextUrl) {
                                    attributes << [url: l.licenseTextUrl]
                                }

                                license(attributes)
                        }
                    }
            }
        }

        writer.close()
    }

    /**
     * Generate xml report grouping by licenses.
     *
     * @param dependencyMetadataSet set with dependencies
     * @param fileName report file name
     */
    public void generateXMLReport4LicenseToDependency(Set<DependencyMetadata> dependencyMetadataSet, String fileName) {
        PrintWriter writer = new PrintWriter(new File(xmlOutputDir, fileName))
        MarkupBuilder xml = new MarkupBuilder(writer)
        Map<LicenseMetadata, Set<String>> licensesMap = getLicenseMap(dependencyMetadataSet)

        xml.licenses() {
            licensesMap.each {
                entry ->
                    def attributes = [name: entry.key.licenseName]

                    // Miss attribute if it's empty
                    if(entry.key.licenseTextUrl) {
                        attributes << [url:  entry.key.licenseTextUrl]
                    }
                    license(attributes) {
                        entry.value.each {
                            d -> dependency(d)
                        }
                    }
            }
        }

        writer.close()
    }

    /**
     * Generate report by dependency.
     *
     * @param dependencyMetadataSet set with dependencies
     * @param fileName report file name
     */
    public void generateHTMLReport4DependencyToLicense(Set<DependencyMetadata> dependencyMetadataSet, String fileName) {
        PrintWriter writer = new PrintWriter(new File(htmlOutputDir, fileName))
        MarkupBuilder html = new MarkupBuilder(writer)

        html.html {
            head {
                title("HTML License report")
            }
            style(
             '''table {
                  width: 85%;
                  border-collapse: collapse;
                  text-align: center;
                }
                .dependencies {
                  text-align: left;
                }
                tr {
                  border: 1px solid black;
                }
                td {
                  border: 1px solid black;
                  font-weight: bold;
                  color: #2E2E2E
                }
                th {
                  border: 1px solid black;
                }
                h3 {
                  text-align:center;
                  margin:3px
                }
                .license {
                    width:70%
                }

                .licenseName {
                    width:15%
                }
                ''')
            body {
                table(align: 'center') {
                    tr {
                        th(){ h3("Dependency") }
                        th(){ h3("Jar") }
                        th(){ h3("License name") }
                        th(){ h3("License text URL") }
                    }

                    dependencyMetadataSet.each {
                        entry ->
                            entry.licenseMetadataList.each { license ->
                                tr {
                                    td(entry.dependency, class: 'dependencies')
                                    td(entry.dependencyFileName, class: 'licenseName')
                                    td(license.licenseName, class: 'licenseName')
                                    td(class: 'license') {
                                        if (license.licenseTextUrl) {
                                            a(href: license.licenseTextUrl, "Show license agreement")
                                        }
                                    }
                                }
                            }
                    }
                }
            }
        }

        writer.close()
    }

    /**
     * Generate html report by license type.
     *
     * @param dependencyMetadataSet set with dependencies
     * @param fileName report file name
     */
    public void generateHTMLReport4LicenseToDependency(Set<DependencyMetadata> dependencyMetadataSet, String fileName) {
        PrintWriter writer = new PrintWriter(new File(htmlOutputDir, fileName))
        MarkupBuilder html = new MarkupBuilder(writer)
        Map<LicenseMetadata, Set<String>> licensesMap = getLicenseMap(dependencyMetadataSet)

        html.html {
            head {
                title("HTML License report")
            }
            style(
             '''table {
                  width: 85%;
                  border-collapse: collapse;
                  text-align: center;
                }

                .dependencies {
                  text-align: left;
                  width:15%;
                }

                tr {
                  border: 1px solid black;
                }

                td {
                  border: 1px solid black;
                  font-weight: bold;
                  color: #2E2E2E
                }

                th {
                  border: 1px solid black;
                }

                h3 {
                  text-align:center;
                  margin:3px
                }

                .license {
                    width:70%
                }

                .licenseName {
                    width:15%
                }
                ''')
            body {
                table(align: 'center') {
                    tr {
                        th(){ h3("License") }
                        th(){ h3("License text URL") }
                        th(){ h3("Dependency") }
                    }

                    licensesMap.each {
                        entry ->
                            tr {
                                td(entry.key.licenseName, class: 'licenseName')
                                td(class: 'license') {
                                    if (entry.key.licenseTextUrl) {
                                        a(href: entry.key.licenseTextUrl, "License agreement")
                                    }
                                }
                                td(class: "dependencies") {
                                    ul() {
                                        entry.value.each {
                                            dependency ->
                                                li(dependency)
                                        }
                                    }
                                }
                            }
                    }
                }
            }
        }

        writer.close()
    }

    // Utility
    private Map<LicenseMetadata, Set<String>> getLicenseMap(Set<DependencyMetadata> dependencyMetadataSet) {
        Map<LicenseMetadata, Set<String>> licensesMap = new HashMap<LicenseMetadata, Set<String>>()

        dependencyMetadataSet.each {
            dependencyMetadata ->
                dependencyMetadata.licenseMetadataList.each { license ->
                    if (!licensesMap.containsKey(license)) {
                        licensesMap.put(license, new HashSet<String>())
                    }
                    licensesMap.get(license).add(dependencyMetadata.dependencyFileName)
                }
        }

        licensesMap
    }
}
