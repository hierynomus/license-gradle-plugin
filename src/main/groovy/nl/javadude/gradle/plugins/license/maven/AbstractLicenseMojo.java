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
package nl.javadude.gradle.plugins.license.maven;

import static com.mycila.maven.plugin.license.document.DocumentType.defaultMapping;
import static com.mycila.maven.plugin.license.git.CopyrightRangeProvider.INCEPTION_YEAR_KEY;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import com.mycila.maven.plugin.license.Callback;
import com.mycila.maven.plugin.license.HeaderSection;
import com.mycila.maven.plugin.license.LicenseCheckMojo;
import com.mycila.maven.plugin.license.PropertiesProvider;
import com.mycila.maven.plugin.license.document.Document;
import com.mycila.maven.plugin.license.document.DocumentPropertiesLoader;
import com.mycila.maven.plugin.license.document.DocumentType;
import com.mycila.maven.plugin.license.header.Header;
import com.mycila.maven.plugin.license.header.HeaderDefinition;
import com.mycila.maven.plugin.license.header.HeaderSource.UrlHeaderSource;
import com.mycila.maven.plugin.license.header.HeaderType;

public class AbstractLicenseMojo {
    static Logger logger = Logging.getLogger(AbstractLicenseMojo.class);

    // Backing AbstraceLicenseMojo
    Collection<File> validHeaders; // Convert to FileCollection
    File rootDir;
    Map<String, String> initial;

    protected String[] keywords = new String[] { "copyright" };
    protected List<HeaderDefinition> headerDefinitions;
    protected HeaderSection[] headerSections = new HeaderSection[0];
    protected String encoding;
    protected float concurrencyFactor = 1.5f;
    protected Map<String, String> mapping;

    boolean dryRun;
    boolean skipExistingHeaders;
    boolean useDefaultMappings;
    boolean strictCheck;
    URI header;
    FileCollection source;
    int inceptionYear;
    File baseDir;

    public AbstractLicenseMojo(Collection<File> validHeaders, File rootDir, Map<String, String> initial,
                               boolean dryRun, boolean skipExistingHeaders, boolean useDefaultMappings,
                               boolean strictCheck, URI header, FileCollection source,
                               Map<String, String> mapping, String encoding,
                               List<HeaderDefinition> headerDefinitions, int inceptionYear, File baseDir) {
        this.validHeaders = validHeaders;
        this.rootDir = rootDir;
        this.initial = initial;
        this.dryRun = dryRun;
        this.skipExistingHeaders = skipExistingHeaders;
        this.useDefaultMappings = useDefaultMappings;
        this.strictCheck = strictCheck;
        this.header = header;
        this.source = source;
        this.mapping = mapping;
        this.encoding = encoding;
        this.headerDefinitions = headerDefinitions;
        this.inceptionYear = inceptionYear;
        this.baseDir = baseDir;
    }

    protected void execute(final Callback callback) throws MalformedURLException, IOException {
        final Map<String, String> props = mergeProperties();

        final Header h = new Header(new UrlHeaderSource(header.toURL(), encoding), headerSections);
        logger.debug("Header {}:\n{}", h.getLocation(), h);

        if (this.validHeaders == null) {
            this.validHeaders = new ArrayList<File>();
        }
        final List<Header> validHeaders = new ArrayList<Header>(this.validHeaders.size());
        for (File validHeader : this.validHeaders) {
            validHeaders.add(
                    new Header(new UrlHeaderSource(validHeader.toURI().toURL(), encoding), headerSections));
        }

        final List<PropertiesProvider> propertiesProviders = new LinkedList<>();
        for (final PropertiesProvider provider : ServiceLoader.load(PropertiesProvider.class,
                                                                    Thread.currentThread()
                                                                          .getContextClassLoader())) {
            provider.init(new com.mycila.maven.plugin.license.AbstractLicenseMojo() {
                {
                    defaultBasedir = baseDir;
                }

                @Override
                public void execute() throws MojoExecutionException, MojoFailureException {
                }
            }, props);
            propertiesProviders.add(provider);
        }
        final DocumentPropertiesLoader documentPropertiesLoader = d -> {
            Map<String, String> properties = new HashMap<>();

            for (String key : props.keySet()) {
                properties.put(key, String.valueOf(props.get(key)));
            }

            properties.put("file.name", d.getFile().getName());
            if (inceptionYear == 0) {
                throw new IllegalStateException("inceptionYear should be provided");
            }
            properties.put(INCEPTION_YEAR_KEY, Integer.toString(inceptionYear));

            for (PropertiesProvider provider : propertiesProviders) {
                try {
                    final Map<String, String> providerProperties = provider.adjustProperties(
                            new LicenseCheckMojo(), properties, d);
                    logger.debug("provider: " + provider.getClass() + " brought new properties\n"
                                 + providerProperties);
                    for (Map.Entry<String, String> entry : providerProperties.entrySet()) {
                        if (entry.getValue() != null) {
                            properties.put(entry.getKey(), entry.getValue());
                        } else {
                            properties.remove(entry.getKey());
                        }
                    }
                } catch (Exception e) {
                    logger.warn("failure occured while calling " + provider.getClass(), e);
                }
            }
            return properties;
        };
        Map<String, HeaderDefinition> definitions = buildHeaderDefinitions();
        final DocumentFactory documentFactory = new DocumentFactory(rootDir, buildMapping(definitions),
                                                                    definitions,
                                                                    encoding, keywords,
                                                                    documentPropertiesLoader);

        int nThreads = (int) (Runtime.getRuntime().availableProcessors() * concurrencyFactor);
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        CompletionService<Boolean> completionService = new ExecutorCompletionService<Boolean>(executorService);
        int count = 0;
        logger.debug("Number of execution threads: {}", nThreads);

        try {
            for (final File file : source) {
                completionService.submit(new Runnable() {
                    public void run() {
                        Document document = documentFactory.createDocuments(file);
                        logger.debug("Selected file: {} [header style: {}]",
                                     DocumentFactory.getRelativeFile(rootDir, document),
                                     document.getHeaderDefinition());
                        if (document.isNotSupported()) {
                            logger.warn("Unknown file extension: {}",
                                        DocumentFactory.getRelativeFile(rootDir, document));
                        } else if (document.is(h)) {
                            logger.debug("Skipping header file: {}",
                                         DocumentFactory.getRelativeFile(rootDir, document));
                        } else if (document.hasHeader(h, strictCheck)) {
                            callback.onExistingHeader(document, h);
                        } else {
                            boolean headerFound = false;
                            for (Header validHeader : validHeaders) {
                                headerFound = document.hasHeader(validHeader, strictCheck);
                                if (headerFound) {
                                    callback.onExistingHeader(document, h);
                                    break;
                                }
                            }
                            if (!headerFound) {callback.onHeaderNotFound(document, h);}
                        }
                    }
                }, null);
                count++;
            }

            while (count-- > 0) {
                try {
                    completionService.take().get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof Error) {throw (Error) cause;}
                    if (cause instanceof RuntimeException) {throw (RuntimeException) cause;}
                    throw new GradleException(cause.getMessage(), cause);
                }
            }

        } finally {
            executorService.shutdownNow();
        }

    }

    // //////////////////////////////////////////////////////////////////////////
    // Pulling from maven-license-plugin. Copying here because methods are protected
    // or rely on Maven classes

    /**
     * From com/google/code/mojo/license/AbstractLicenseMojo.java
     */
    protected final Map<String, String> mergeProperties() {
        // first put syste environment
        Map<String, String> props = new HashMap<String, String>(System.getenv());

        // Override with extension
        props.putAll(initial);

        // then we override by java system properties (command-line -D...)
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            props.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return props;
    }

    private Map<String, String> buildMapping(Map<String, HeaderDefinition> headerDefinitions) {
        Map<String, String> extensionMapping = useDefaultMappings ?
                                               new HashMap<>(defaultMapping()) :
                                               new HashMap<>();

        List<HeaderType> headerTypes = Arrays.asList(HeaderType.values());
        Set<String> validHeaderTypes = new HashSet<String>();
        for (HeaderType headerType : headerTypes) {
            validHeaderTypes.add(headerType.name().toLowerCase());
        }

        // Add all custom headers
        validHeaderTypes.addAll(headerDefinitions.keySet());

        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String headerType = entry.getValue().toLowerCase();
            String fileType = entry.getKey().toLowerCase();
            if (!validHeaderTypes.contains(headerType)) {
                throw new InvalidUserDataException(
                        String.format("The provided header type (%s) for %s is invalid", headerType, fileType));
            }
            extensionMapping.put(fileType, headerType);
        }
        // force inclusion of unknown item to manage unknown files
        extensionMapping.put(DocumentType.UNKNOWN.getExtension(),
                             DocumentType.UNKNOWN.getDefaultHeaderTypeName());
        return extensionMapping;
    }

    private Map<String, HeaderDefinition> buildHeaderDefinitions() {
        // like mappings, first get default definitions
        final Map<String, HeaderDefinition> headers = new HashMap<String, HeaderDefinition>(
                HeaderType.defaultDefinitions());

        // Add additional header definitions
        for (HeaderDefinition headerDefinitionBuilder : headerDefinitions) {
            headers.put(headerDefinitionBuilder.getType(), headerDefinitionBuilder);
        }

        // force inclusion of unknown item to manage unknown files
        headers.put(HeaderType.UNKNOWN.getDefinition().getType(), HeaderType.UNKNOWN.getDefinition());
        return headers;
    }

    // TODO reimplement with Gradle classloaders
    private File findResource(String resource) {
        return null;
    }

}
