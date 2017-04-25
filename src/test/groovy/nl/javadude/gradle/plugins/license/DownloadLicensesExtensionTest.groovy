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

import org.junit.Before
import org.junit.Test

/**
 * Unit test for {@link DownloadLicensesExtension}.
 */
class DownloadLicensesExtensionTest {

    DownloadLicensesExtension extension;

    @Before
    public void setupProject() {
        extension = new DownloadLicensesExtension();
    }

    @Test
    public void ableToConstruct() {
        assert extension != null;
    }

    @Test
    public void licenseMetaDataBuilderWorksWell() {
        // WHEN
        extension.licenses = [
                "org.gson:gson:1.4" : extension.license("Apache 2","http://google.com")
        ]

        // THEN
        assert extension.licenses["org.gson:gson:1.4"] == new LicenseMetadata(
                licenseName: "Apache 2",
                licenseTextUrl: "http://google.com"
        )
    }

}
