package nl.javadude.gradle.plugins.license.maven;

import static com.google.code.mojo.license.document.DocumentType.defaultMapping;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import com.google.code.mojo.license.Callback;
import com.google.code.mojo.license.HeaderSection;
import com.google.code.mojo.license.document.Document;
import com.google.code.mojo.license.document.DocumentType;
import com.google.code.mojo.license.header.AdditionalHeaderDefinition;
import com.google.code.mojo.license.header.Header;
import com.google.code.mojo.license.header.HeaderDefinition;
import com.google.code.mojo.license.header.HeaderType;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.base.Function;

import com.mycila.xmltool.XMLDoc;

public class AbstractLicenseMojo {
    static Logger logger = Logging.getLogger(AbstractLicenseMojo.class);

    // Backing AbstraceLicenseMojo
    Collection<File> validHeaders; // Convert to FileCollection
    File rootDir;
    Map<String, String> initial;

    protected String[] keywords = new String[] { "copyright" };
    protected String[] headerDefinitions = new String[0]; // TODO Not sure how a user would specify
    protected HeaderSection[] headerSections = new HeaderSection[0];
    protected String encoding = System.getProperty("file.encoding");
    protected float concurrencyFactor = 1.5f;
    protected Map<String, String> mapping;
    
    boolean dryRun;
    boolean skipExistingHeaders;
    boolean useDefaultMappings;
    boolean strictCheck;
    File header;
    FileCollection source;

    public AbstractLicenseMojo(Collection<File> validHeaders, File rootDir, Map<String, String> initial,
                    boolean dryRun, boolean skipExistingHeaders, boolean useDefaultMappings, boolean strictCheck,
                    File header, FileCollection source, Map<String, String> mapping) {
        super();
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
    }

    protected void execute(final Callback callback) throws MalformedURLException {
        Map<String, String> props = mergeProperties();

        final Header h = new Header(header.toURI().toURL(), props, headerSections);
        logger.debug("Header {}:\n{}", h.getLocation(), h);

        if (this.validHeaders == null)
            this.validHeaders = new ArrayList<File>();
        final List<Header> validHeaders = new ArrayList<Header>(this.validHeaders.size());
        for (File validHeader : this.validHeaders) {
            validHeaders.add(new Header(validHeader.toURI().toURL(), props, headerSections));
        }

        final DocumentFactory documentFactory = new DocumentFactory(rootDir, buildMapping(), buildHeaderDefinitions(),
                        encoding, keywords);

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
                        logger.debug("Selected file: {} [header style: {}]", DocumentFactory.getRelativeFile(rootDir, document),
                                        document.getHeaderDefinition());
                        if (document.isNotSupported()) {
                            logger.warn("Unknown file extension: {}", DocumentFactory.getRelativeFile(rootDir, document));
                        } else if (document.is(h)) {
                            logger.debug("Skipping header file: {}", DocumentFactory.getRelativeFile(rootDir, document));
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
                            if (!headerFound)
                                callback.onHeaderNotFound(document, h);
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
                    if (cause instanceof Error)
                        throw (Error) cause;
                    if (cause instanceof RuntimeException)
                        throw (RuntimeException) cause;
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

    private Map<String, String> buildMapping() {
        Map<String, String> extensionMapping = useDefaultMappings ? new HashMap<String, String>(defaultMapping())
                        : new HashMap<String, String>();
                        
        List<HeaderType> headerTypes = Arrays.asList(HeaderType.values());
        Set<String> validHeaderTypes = Sets.newHashSet(Lists.transform(headerTypes, new Function<HeaderType, String>() {
            public String apply(HeaderType headerType) {
                return headerType.name();
            }
        }));

        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String headerType = entry.getValue().toUpperCase();
            String fileType = entry.getKey().toLowerCase();
            if (!validHeaderTypes.contains(headerType)) {
                throw new InvalidUserDataException(String.format("The provided header type (%s) for %s is invalid", headerType, fileType));
            }
            extensionMapping.put(fileType, headerType);
        }
        // force inclusion of unknown item to manage unknown files
        extensionMapping.put(DocumentType.UNKNOWN.getExtension(), DocumentType.UNKNOWN.getDefaultHeaderTypeName());
        return extensionMapping;
    }

    private Map<String, HeaderDefinition> buildHeaderDefinitions() {
        // like mappings, first get default definitions
        final Map<String, HeaderDefinition> headers = new HashMap<String, HeaderDefinition>(
                        HeaderType.defaultDefinitions());
        // and then override them with those provided in properties file
        for (String resource : headerDefinitions) {
            final AdditionalHeaderDefinition fileDefinitions = new AdditionalHeaderDefinition(XMLDoc.from(findResource(resource), true));
            final Map<String, HeaderDefinition> map = fileDefinitions.getDefinitions();
            logger.debug("{} header definitions loaded from '{}'", map.size(), resource);
            headers.putAll(map);
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
