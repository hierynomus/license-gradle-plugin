/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package nl.javadude.gradle.plugins.license.types

import nl.javadude.gradle.plugins.license.License

class LicenseFormat {
    static final String LICENSE_KEY = "License added by: GRADLE-LICENSE-PLUGIN"
    def prefix = ""
    def line = ""
    def suffix = null

    License transform(List<String> input) {
        def license = new License()
        license.add(prefix + " " + LICENSE_KEY)
        input.each { line ->
            license.add(this.line + " " + line)
        }
        !suffix ?: license.add(suffix)

        license
    }

    static boolean isLicensedByPlugin(File file) {
        def lines = file.readLines()
        lines.size() > 0 ? lines[0].contains(LICENSE_KEY) : false
    }

    def removeLicenseBlock(lines) {
        def newLines = []
        if (!lines[0].startsWith(prefix))
            lines
        else {
            boolean inBlock = true
            boolean skipEmptyLine = false
            lines[1..lines.size() - 1].each { l ->
                if (inBlock && l.startsWith(line) && !(suffix && l.startsWith(suffix))) {

                } else if (inBlock && suffix && l.startsWith(suffix)) {
                    inBlock = false
                    skipEmptyLine = true
                } else if (inBlock && !suffix) {
                    inBlock = false
                } else if (skipEmptyLine) {
                    skipEmptyLine = false
                } else {
                    newLines.add(l)
                }
            }

            newLines
        }
    }
}

