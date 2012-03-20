/**
 * Copyright (C) 2008 http://code.google.com/p/maven-license-plugin/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.javadude.gradle.plugins.license.maven;

import com.google.code.mojo.license.document.Document;
import com.google.code.mojo.license.header.Header;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

/**
 * Check if the source files of the project have a valid license header
 *
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class LicenseCheckMojo implements CallbackWithFailure {
    Logger logger = Logging.getLogger(LicenseCheckMojo.class);

    public final Collection<File> missingHeaders = new ConcurrentLinkedQueue<File>();

    @Override
    public void onHeaderNotFound(Document document, Header header) {
        logger.lifecycle("Missing header in: {}", document.getFile());
        missingHeaders.add(document.getFile());
    }

    @Override
    public void onExistingHeader(Document document, Header header) {
        logger.info("Header OK in: {}", document.getFile());
    }

    @Override
    public boolean hadFailure() {
        return !missingHeaders.isEmpty();
    }

    @Override
    public Collection<File> getAffected() {
        return missingHeaders;
    }

}
