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
import com.google.code.mojo.license.header.HeaderParser;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

/**
 * Reformat files with a missing header to add it
 *
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class LicenseFormatMojo implements CallbackWithFailure {

    /**
     * For accessing the {@link com.google.code.mojo.license.document.Document#parser} object
     */
    private static final String DOCUMENT_PARSER_FIELD_NAME = "parser";

    Logger logger = Logging.getLogger(LicenseCheckMojo.class);
    File basedir;

    public LicenseFormatMojo(File basedir, boolean dryRun, boolean skipExistingHeaders, boolean insertNewline) {
        this.basedir = basedir;
        this.dryRun = dryRun;
        this.skipExistingHeaders = skipExistingHeaders;
        this.insertNewline = insertNewline;
    }

    /**
     * Whether to create new files which have changes or to make them inline
     */
    protected boolean dryRun = false;

    /**
     * Whether to skip file where a header has been detected
     */
    protected boolean skipExistingHeaders = false;

    /**
     * Whether to insert a new line after the header content
     */
    protected boolean insertNewline = false;

    public final Collection<File> missingHeaders = new ConcurrentLinkedQueue<File>();

    public void onHeaderNotFound(Document document, Header header) {
        document.parseHeader();
        if (document.headerDetected()) {
            if (skipExistingHeaders) {
                logger.info("Keeping license header in: {}", DocumentFactory.getRelativeFile(basedir, document));
                return;
            } else
                document.removeHeader();
        }
        logger.lifecycle("Updating license header in: {}", DocumentFactory.getRelativeFile(basedir, document));
        document.updateHeader(header);
        if(insertNewline) {
            try {
                // Bypass the 'private' modifier
                Field documentParserField = document.getClass().getDeclaredField(DOCUMENT_PARSER_FIELD_NAME);
                documentParserField.setAccessible(true);
                // Fetch the 'parser' object
                HeaderParser documentHeaderParser = (HeaderParser) documentParserField.get(document);
                // Reset the 'private' modifier
                documentParserField.setAccessible(false);
                // Calculate the position for the new line
                String headerContent = header.applyDefinitionAndSections(documentHeaderParser.getHeaderDefinition(), documentHeaderParser.getFileContent().isUnix());
                int position =  documentHeaderParser.getBeginPosition() + headerContent.length();
                // Insert the new line
                String newline = System.getProperty("line.separator");
                documentHeaderParser.getFileContent().insert(position, newline);
            } catch (Exception e) { // NoSuchFieldException | IllegalAccessException
                throw new GradleException(String.format(
                        "An error occurred while attempting to insert a new line after header -- couldn't properly access / manipulate the %s field!",
                        document.getClass().getCanonicalName() + "#" + DOCUMENT_PARSER_FIELD_NAME
                ), e);
            }
        }
        missingHeaders.add(document.getFile());
        if (!dryRun) {
            document.save();
        } else {
            String name = document.getFile().getName() + ".licensed";
            File copy = new File(document.getFile().getParentFile(), name);
            logger.debug("Result saved to: {}", copy);
            document.saveTo(copy);
        }
    }

    public void onExistingHeader(Document document, Header header) {
        logger.info("Header OK in: {}", DocumentFactory.getRelativeFile(basedir, document));
    }

    @Override
    public boolean hadFailure() {
        // Can't really fail, since we're actually modifying files
        return false;
    }

    @Override
    public Collection<File> getAffected() {
        return missingHeaders;
    }

}