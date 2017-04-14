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

import com.mycila.maven.plugin.license.document.Document;
import com.mycila.maven.plugin.license.document.DocumentPropertiesLoader;
import com.mycila.maven.plugin.license.header.HeaderDefinition;

import java.io.File;
import java.util.Map;

import static org.codehaus.plexus.util.FileUtils.*;

/**
 * <b>Date:</b> 14-Feb-2008<br>
 * <b>Author:</b> Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class DocumentFactory {
    private final Map<String, String> mapping;
    private final Map<String, HeaderDefinition> definitions;
    private final File basedir;
    private final String encoding;
    private final String[] keywords;
    private final DocumentPropertiesLoader documentPropertiesLoader;

    public DocumentFactory(File basedir, Map<String, String> mapping, Map<String, HeaderDefinition> definitions, String encoding, String[] keywords, DocumentPropertiesLoader documentPropertiesLoader) {
        this.mapping = mapping;
        this.definitions = definitions;
        this.basedir = basedir;
        this.encoding = encoding;
        this.keywords = keywords.clone();
        this.documentPropertiesLoader = documentPropertiesLoader;
    }

    public Document createDocuments(String file) {
        return getWrapper(file, encoding);
    }

    public Document createDocuments(File file) {
        return getWrapper(file, encoding);
    }

    private Document getWrapper(String file, String encoding) {
        return getWrapper( new File(basedir, file), encoding);
    }

    private Document getWrapper(File file, String encoding) {
        String headerType = mapping.get(extension(file.getName()).toLowerCase());
        if (headerType == null) {
            headerType = mapping.get("");
        } else {
            headerType = headerType.toLowerCase();
        }
        return new Document(file, definitions.get(headerType), encoding, keywords, documentPropertiesLoader);
    }

    public static String getRelativeFile(File basedir, Document document) {
        String prefix = basedir.getAbsolutePath();
        String whole = document.getFile().getAbsolutePath();
        if (whole.startsWith(prefix)) {
            return whole.substring(prefix.length()+1);
        } else {
            return document.getFile().getPath();
        }
    }
}
