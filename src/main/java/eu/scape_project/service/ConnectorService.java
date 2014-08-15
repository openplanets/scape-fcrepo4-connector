/*
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package eu.scape_project.service;

import static eu.scape_project.rdf.ScapeRDFVocabulary.*;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.bind.JAXBException;

import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.ObjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.scape_project.model.BitStream;
import eu.scape_project.model.File;
import eu.scape_project.model.Identifier;
import eu.scape_project.rdf.RDFRecord;
import eu.scape_project.util.ScapeMarshaller;

/**
 * Component which does all the interaction with fcrepo4
 * 
 * @author frank asseg
 */
@Component
public class ConnectorService {

    private final String SCAPE_PATH = "/objects/scape/";

    private final String MIXIN_BITSTREAM = "scape:bitstream";

    private final String MIXIN_FILE = "scape:file";

    private final String MIXIN_REPRESENTATION = "scape:representation";

    private final String MIXIN_ENTITY = "scape:entity";

    private final String MIXIN_QUEUE = "scape:queue";

    private final String MIXIN_METADATA = "scape:metadata";

    public boolean referencedContent;

    private static final Logger LOG = LoggerFactory.getLogger(ConnectorService.class);

    private ScapeMarshaller marshaller;

    private final IdentifierTranslator subjects = new DefaultIdentifierTranslator();

    @Autowired
    private ObjectService objectService;

    @Autowired
    private DatastreamService datastreamService;

    /**
     * Create a new {@link ConnectorService} instance
     * 
     * @throws JAXBException if the initizialization of the JAX-B marshalling mechanism failed
     */
    public ConnectorService() throws JAXBException {
        marshaller = ScapeMarshaller.newInstance();
    }

    /**
     * Check if Fedora 4 is used for persisting binary files or for persisting only refernces to binaries
     * 
     * @return Returns <code>true</code> if binary content is referenced. Returns <code>false</code> if binary conten
     *         of {@link File}s is persisted inside Fedora 4
     */
    public boolean isReferencedContent() {
        return referencedContent;
    }

    /**
     * Set the behaviour for the binary content of {@link File}s.
     * 
     * @param referencedContent If <code>true</code> {@link File}s' binary content will only be referenced in Fedora.
     *        If <code>false</code> Fedora is used to save the actual binary content of the {@link File}
     */
    public void setReferencedContent(boolean referencedContent) {
        this.referencedContent = referencedContent;
    }

    public String addBitstream(Session session, String path, BitStream bitStream) throws RepositoryException {
        /* create BS node */
        final FedoraObject obj = this.objectService.createObject(session, path);
        obj.getNode().addMixin(MIXIN_BITSTREAM);

        /* add the technical metadata if existent */
        String techUri = addMetadata(session, path, bitStream.getTechnical());

        /* add the properties via SPARQL */
        final String uri = subjects.getSubject(obj.getPath()).getURI();
        final String sparql = createBitstreamSparql(bitStream, obj.getPath());
        obj.updatePropertiesDataset(subjects, sparql);

        return uri;
    }

    private String addMetadata(Session session, String path, final Object metadata) throws RepositoryException {
        try {
            final Datastream ds = writeMetadata(session, path, metadata);
            final Node desc = ds.getContentNode();
            desc.addMixin(MIXIN_METADATA);
            return subjects.getSubject(path).getURI();
        } catch (IOException | InvalidChecksumException e) {
            throw new RepositoryException(e);
        }
    }

    private Datastream writeMetadata(Session session, String path, final Object metadata) throws IOException, RepositoryException, InvalidChecksumException {
        /* use piped streams to copy the data to the repo */
        final PipedInputStream dcSrc = new PipedInputStream();
        final PipedOutputStream dcSink = new PipedOutputStream();
        dcSink.connect(dcSrc);
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    ConnectorService.this.marshaller.getJaxbMarshaller().marshal(metadata, dcSink);
                    dcSink.flush();
                    dcSink.close();
                } catch (JAXBException e) {
                    LOG.error(e.getLocalizedMessage(), e);
                } catch (IOException e) {
                    LOG.error(e.getLocalizedMessage(), e);
                }
            }
        }).start();
        return datastreamService.createDatastream(session, path, "text/xml", null, dcSrc);
    }

    public BitStream retrieveBitstream(Session session, String path) throws RepositoryException {
        FedoraObject obj = this.objectService.getObject(session, path);
        String idVal = obj.getNode().getProperty(HAS_IDENTIFIER_VALUE).getString();
        String idType = obj.getNode().getProperty(HAS_IDENTIFIER_TYPE).getString();
        String metadataPath = obj.getNode().getProperty(HAS_METADATA).getString();
        BitStream.Type type = BitStream.Type.valueOf(obj.getNode().getProperty(HAS_BITSTREAM_TYPE).getString());

        return new BitStream.Builder()
                .identifier(new Identifier(idType, idVal))
                .technical(retrieveMetadata(session, metadataPath))
                .type(type)
                .build();
    }

    private Object retrieveMetadata(Session session, String path) throws RepositoryException {
        final Datastream mdDs = this.datastreamService.getDatastream(session, path);
        try {
            return this.marshaller.deserialize(mdDs.getContent());
        } catch (JAXBException e) {
            throw new RepositoryException(e);
        }
    }

    private String createBitstreamSparql(BitStream bitStream, String uri) {
        RDFRecord[] records =
                new RDFRecord[] {
                    new RDFRecord(uri, HAS_IDENTIFIER_VALUE, bitStream.getIdentifier().getValue()),
                    new RDFRecord(uri, HAS_IDENTIFIER_TYPE, bitStream.getIdentifier().getType()),
                    new RDFRecord(uri, HAS_TYPE, MIXIN_BITSTREAM),
                    new RDFRecord(uri, HAS_BITSTREAM_TYPE, bitStream.getType() == null ? BitStream.Type.STREAM.name()
                            : bitStream.getType().name())
                };
        return createSparql(Arrays.asList(records));
    }

    public String createSparql(List<RDFRecord> records) {
        final StringBuilder sparql = new StringBuilder("PREFIX scape: <http://scapeproject.eu/model#> ");
        for (RDFRecord r : records) {
            sparql.append("INSERT DATA {<")
                    .append(r.getUri())
                    .append("> <")
                    .append(r.getProperty())
                    .append("> '")
                    .append(r.getValue() + "';");
        }
        return sparql.toString();
    }
}
