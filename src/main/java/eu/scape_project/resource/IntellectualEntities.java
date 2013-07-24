/**
 *
 */

package eu.scape_project.resource;

import info.lc.xmlns.premis_v2.PremisComplexType;
import info.lc.xmlns.premis_v2.RightsComplexType;
import info.lc.xmlns.textmd_v3.TextMD;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBException;

import org.fcrepo.Datastream;
import org.fcrepo.FedoraObject;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.rdf.SerializationUtils;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.NodeService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.session.InjectedSession;
import org.purl.dc.elements._1.ElementContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.books.gbs.GbsType;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import edu.harvard.hul.ois.xml.ns.fits.fits_output.Fits;
import eu.scapeproject.model.BitStream;
import eu.scapeproject.model.File;
import eu.scapeproject.model.Identifier;
import eu.scapeproject.model.IntellectualEntity;
import eu.scapeproject.model.LifecycleState;
import eu.scapeproject.model.Representation;
import eu.scapeproject.util.ScapeMarshaller;
import gov.loc.audiomd.AudioType;
import gov.loc.marc21.slim.RecordType;
import gov.loc.mix.v20.Mix;
import gov.loc.videomd.VideoType;

/**
 * @author frank asseg
 *
 */
@Component
@Scope("prototype")
@Path("/scape/entity")
public class IntellectualEntities {

    public final static String ENTITY_FOLDER = "objects/scape/entities";

    private final ScapeMarshaller marshaller;

    private static final Logger LOG = LoggerFactory
            .getLogger(IntellectualEntities.class);

    @Autowired
    private ObjectService objectService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private DatastreamService datastreamService;

    @InjectedSession
    private Session session;

    public IntellectualEntities()
            throws JAXBException {
        marshaller = ScapeMarshaller.newInstance();
    }

    private String addBitStreams(List<BitStream> bitStreams, String filePath)
            throws RepositoryException {

        final StringBuilder sparql = new StringBuilder();

        for (BitStream bs : bitStreams) {
            final String bsId =
                    (bs.getIdentifier() != null) ? bs.getIdentifier()
                            .getValue() : UUID.randomUUID().toString();
            final String bsPath = filePath + "/" + bsId;
            final FedoraObject bsObject =
                    this.objectService.createObject(this.session, bsPath);
            sparql.append(addMetadata(bs.getTechnical(), bsPath + "/TECHNICAL"));

            sparql.append("INSERT {<info:fedora/" + bsObject.getPath() +
                    "> <http://scapeproject.eu/model#hasType> \"bitstream\"} WHERE {};");
            sparql.append("INSERT {<info:fedora/" + bsObject.getPath() +
                    "> <http://scapeproject.eu/model#hasBitstreamType> \"" +
                    bs.getType() + "\"} WHERE {};");
            sparql.append("INSERT {<info:fedora/" +
                    filePath +
                    "> <http://scapeproject.eu/model#hasBitStream> <info:fedora/" +
                    bsObject.getPath() + ">} WHERE {};");
        }

        return sparql.toString();
    }

    private String addFiles(List<File> files, String repPath)
            throws RepositoryException {

        final StringBuilder sparql = new StringBuilder();
        for (File f : files) {

            final String fileId =
                    (f.getIdentifier() != null) ? f.getIdentifier().getValue()
                            : UUID.randomUUID().toString();
            final String filePath = repPath + "/" + fileId;

            /* get a handle on the binary data associated with this file */
            LOG.info("fetching file from " + f.getUri().toASCIIString());
            try (final InputStream src = f.getUri().toURL().openStream()) {

                /* create a datastream in fedora for this file */
                final FedoraObject fileObject =
                        this.objectService.createObject(this.session, filePath);

                /* add the binary data referenced in the file as a datastream */
                final Node fileDs =
                        this.datastreamService.createDatastreamNode(
                                this.session, filePath + "/DATA", "text/xml",
                                src);

                /* add the metadata */
                sparql.append(addMetadata(f.getTechnical(), filePath +
                        "/TECHNICAL"));

                /* add all bitstreams as child objects */
                sparql.append(addBitStreams(f.getBitStreams(), repPath));

                sparql.append("INSERT {<info:fedora/" + fileObject.getPath() +
                        "> <http://scapeproject.eu/model#hasType> \"file\"} WHERE {};");
                sparql.append("INSERT {<info:fedora/" + fileObject.getPath() +
                        "> <http://scapeproject.eu/model#hasFileName> \"" +
                        f.getFilename() + "\"} WHERE {};");
                sparql.append("INSERT {<info:fedora/" + fileObject.getPath() +
                        "> <http://scapeproject.eu/model#hasMimeType> \"" +
                        f.getMimetype() + "\"} WHERE {};");
                sparql.append("INSERT {<info:fedora/" + fileObject.getPath() +
                        "> <http://scapeproject.eu/model#hasIngestSource> \"" +
                        f.getUri() + "\"} WHERE {};");
                sparql.append("INSERT {<info:fedora/" +
                        repPath +
                        "> <http://scapeproject.eu/model#hasFile> <info:fedora/" +
                        fileObject.getPath() + ">} WHERE {};");

            } catch (IOException e) {
                throw new RepositoryException(e);
            } catch (InvalidChecksumException e) {
                throw new RepositoryException(e);
            }
        }

        return sparql.toString();
    }

    private String addMetadata(final Object descriptive, final String path)
            throws RepositoryException {
        final StringBuilder sparql = new StringBuilder();
        try {

            /* use piped streams to copy the data to the repo */
            final PipedInputStream dcSrc = new PipedInputStream();
            final PipedOutputStream dcSink = new PipedOutputStream();
            dcSink.connect(dcSrc);
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        IntellectualEntities.this.marshaller
                                .getJaxbMarshaller().marshal(descriptive,
                                        dcSink);
                        dcSink.flush();
                        dcSink.close();
                    } catch (JAXBException e) {
                        LOG.error(e.getLocalizedMessage(), e);
                    } catch (IOException e) {
                        LOG.error(e.getLocalizedMessage(), e);
                    }
                }
            }).start();

            final Node desc =
                    datastreamService.createDatastreamNode(this.session, path,
                            "text/xml", dcSrc);

            /* get the type of the metadata */
            String type = "unknown";
            String schema = "";

            if (descriptive.getClass() == ElementContainer.class) {
                type = "dublin-core";
                schema = "http://purl.org/dc/elements/1.1/";
            } else if (descriptive.getClass() == GbsType.class) {
                type = "gbs";
                schema = "http://books.google.com/gbs";
            } else if (descriptive.getClass() == Fits.class) {
                type = "fits";
                schema = "http://hul.harvard.edu/ois/xml/ns/fits/fits_output";
            } else if (descriptive.getClass() == AudioType.class) {
                type = "audiomd";
                schema = "http://www.loc.gov/audioMD/";
            } else if (descriptive.getClass() == RecordType.class) {
                type = "marc21";
                schema = "http://www.loc.gov/MARC21/slim";
            } else if (descriptive.getClass() == Mix.class) {
                type = "mix";
                schema = "http://www.loc.gov/mix/v20";
            } else if (descriptive.getClass() == VideoType.class) {
                type = "videomd";
                schema = "http://www.loc.gov/videoMD/";
            } else if (descriptive.getClass() == PremisComplexType.class) {
                type = "premis-provenance";
                schema = "info:lc/xmlns/premis-v2";
            } else if (descriptive.getClass() == RightsComplexType.class) {
                type = "premis-rights";
                schema = "info:lc/xmlns/premis-v2";
            } else if (descriptive.getClass() == TextMD.class) {
                type = "textmd";
                schema = "info:lc/xmlns/textmd-v3";
            }

            /* add a sparql query to set the type of this object */
            sparql.append("INSERT {<info:fedora/" + desc.getPath() +
                    "> <http://scapeproject.eu/model#hasType> \"" + type +
                    "\"} WHERE {};");
            sparql.append("INSERT {<info:fedora/" + desc.getPath() +
                    "> <http://scapeproject.eu/model#hasSchema> \"" + schema +
                    "\"} WHERE {};");

            return sparql.toString();

        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (InvalidChecksumException e) {
            throw new RepositoryException(e);
        }
    }

    private String addRepresentations(List<Representation> representations,
            String entityPath) throws RepositoryException {
        final StringBuilder sparql = new StringBuilder();
        for (Representation rep : representations) {

            final String repId =
                    (rep.getIdentifier() != null) ? rep.getIdentifier()
                            .getValue() : UUID.randomUUID().toString();
            final String repPath = entityPath + "/" + repId;
            final FedoraObject repObject =
                    objectService.createObject(this.session, repPath);

            /* add the metadatasets of the rep as datastreams */
            sparql.append(addMetadata(rep.getTechnical(), repPath +
                    "/TECHNICAL"));
            sparql.append(addMetadata(rep.getSource(), repPath + "/SOURCE"));
            sparql.append(addMetadata(rep.getRights(), repPath + "/RIGHTS"));
            sparql.append(addMetadata(rep.getProvenance(), repPath +
                    "/PROVENANCE"));

            /* add all the files */
            sparql.append(addFiles(rep.getFiles(), repPath));

            /* add a sparql query to set the type of this object */
            sparql.append("INSERT {<info:fedora/" + repObject.getPath() +
                    "> <http://scapeproject.eu/model#hasType> \"representation\"} WHERE {};");
            sparql.append("INSERT {<info:fedora/" + repObject.getPath() +
                    "> <http://scapeproject.eu/model#hasTitle> \"" +
                    rep.getTitle() + "\"} WHERE {};");
            sparql.append("INSERT {<info:fedora/" +
                    entityPath +
                    "> <http://scapeproject.eu/model#hasRepresentation> <info:fedora/" +
                    repObject.getPath() + ">} WHERE {};");

        }
        return sparql.toString();
    }

    private BitStream fetchBitStream(String bsUri) throws RepositoryException {
        final BitStream.Builder bs = new BitStream.Builder();
        bs.identifier(new Identifier(bsUri));
        bs.technical(fetchMetadata(bsUri + "/TECHNICAL"));
        return bs.build();
    }

    private File fetchFile(String fileUri) throws RepositoryException {
        final File.Builder f = new File.Builder();
        final FedoraObject fileObject =
                this.objectService.getObject(this.session, fileUri);
        final Model repModel =
                SerializationUtils.unifyDatasetModel(fileObject
                        .getPropertiesDataset());
        final Resource parent =
                repModel.createResource("info:fedora" + fileObject.getPath());

        /* fetch and add the properties and metadata from the repo */
        f.technical(fetchMetadata(fileUri + "/TECHNICAL"));
        f.identifier(new Identifier(fileUri));
        f.filename(getFirstLiteralString(repModel, parent,
                "http://scapeproject.eu/model#hasFileName"));
        f.mimetype(getFirstLiteralString(repModel, parent,
                "http://scapeproject.eu/model#hasMimeType"));

        /* discover all the Bistreams and add them to the file */
        final List<BitStream> streams = new ArrayList<>();
        for (String bsUri : getLiteralStrings(repModel, parent,
                "http://scapeproject.eu/model#hasBitStream")) {
            streams.add(fetchBitStream(bsUri.substring(11)));
        }
        f.bitStreams(streams);

        return f.build();
    }

    private Object fetchMetadata(final String path) throws RepositoryException {
        try {
            final Datastream mdDs =
                    this.datastreamService.getDatastream(this.session, path);
            return this.marshaller.deserialize(mdDs.getContent());
        } catch (JAXBException e) {
            throw new RepositoryException(e);
        }
    }

    private Representation fetchRepresentation(String repUri)
            throws RepositoryException {
        final Representation.Builder rep = new Representation.Builder();
        final FedoraObject repObject =
                this.objectService.getObject(this.session, repUri);
        final Model repModel =
                SerializationUtils.unifyDatasetModel(repObject
                        .getPropertiesDataset());
        final Resource parent =
                repModel.createResource("info:fedora" + repObject.getPath());

        /* find the title and id */
        rep.identifier(new Identifier(repUri));
        rep.title(getFirstLiteralString(repModel, parent,
                "http://scapeproject.eu/model#hasTitle"));

        /* find and add the metadata */
        rep.technical(fetchMetadata(repObject.getPath() + "/TECHNICAL"));
        rep.source(fetchMetadata(repObject.getPath() + "/SOURCE"));
        rep.provenance(fetchMetadata(repObject.getPath() + "/PROVENANCE"));
        rep.rights(fetchMetadata(repObject.getPath() + "/RIGHTS"));

        /* add the individual files */
        final List<File> files = new ArrayList<>();
        for (String fileUri : getLiteralStrings(repModel, parent,
                "http://scapeproject.eu/model#hasFile")) {
            files.add(fetchFile(fileUri.substring(11)));
        }

        rep.files(files);
        return rep.build();
    }

    private String getFirstLiteralString(Model model, Resource subject,
            String propertyName) {

        final Property p = model.createProperty(propertyName);
        final StmtIterator it =
                model.listStatements(subject, p, (RDFNode) null);
        return it.next().getObject().toString();
    }

    private List<String> getLiteralStrings(Model model, Resource subject,
            String propertyName) {
        final List<String> result = new ArrayList<>();
        final Property p = model.createProperty(propertyName);
        final StmtIterator it =
                model.listStatements(subject, p, (RDFNode) null);
        while (it.hasNext()) {
            result.add(it.next().getObject().toString());
        }
        return result;
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response ingestEntity(final InputStream src)
            throws RepositoryException {
        try {

            /* read the post body into an IntellectualEntity object */
            final IntellectualEntity ie =
                    this.marshaller.deserialize(IntellectualEntity.class, src);
            final StringBuilder sparql = new StringBuilder();

            /* create the entity top level object in fcrepo */
            final String entityId =
                    (ie.getIdentifier() != null) ? ie.getIdentifier()
                            .getValue() : UUID.randomUUID().toString();
            final String entityPath = ENTITY_FOLDER + "/" + entityId;
            final FedoraObject entityObject =
                    objectService.createObject(this.session, entityPath);

            /* add the metadata datastream for descriptive metadata */
            sparql.append(addMetadata(ie.getDescriptive(), entityPath +
                    "/DESCRIPTIVE"));

            /* add all the representations */
            sparql.append(addRepresentations(ie.getRepresentations(),
                    entityPath));

            /* update the intellectual entity's properties */
            sparql.append("INSERT {<info:fedora/" + entityObject.getPath() +
                    "> <http://scapeproject.eu/model#hasLifeCycleState> \"" +
                    LifecycleState.State.INGESTED + "\"} WHERE {};");
            sparql.append("INSERT {<info:fedora/" + entityObject.getPath() +
                    "> <http://scapeproject.eu/model#hasLifeCycleStateDetails> \"successfully ingested at " + new Date().getTime() + "\"} WHERE {};");
            sparql.append("INSERT {<info:fedora/" + entityObject.getPath() +
                    "> <http://scapeproject.eu/model#hasType> \"intellectualentity\"} WHERE {};");

            /* update the object and it's child's using sparql */
            entityObject.updatePropertiesDataset(sparql.toString());

            /* save the changes made to the objects */
            this.session.save();

            return Response.ok(entityId, MediaType.TEXT_PLAIN).status(201)
                    .build();
        } catch (JAXBException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new RepositoryException(e);
        }
    }

    @GET
    @Produces(MediaType.TEXT_XML)
    @Path("{id}")
    public Response retrieveEntity(@PathParam("id")
    final String id, @Context
    final UriInfo uriInfo) throws RepositoryException {

        final IntellectualEntity.Builder ie = new IntellectualEntity.Builder();
        ie.identifier(new Identifier(id));

        final String entityPath = "/" + ENTITY_FOLDER + "/" + id;
        final FedoraObject ieObject =
                this.objectService.getObject(this.session, entityPath);


        /* fetch the ie's metadata form the repo */
        ie.descriptive(fetchMetadata(entityPath + "/DESCRIPTIVE"));

        /* fetch the representations */
        final Model entityModel =
                SerializationUtils.unifyDatasetModel(ieObject
                        .getPropertiesDataset());


        /* find all the representations of this entity */
        final Resource parent =
                entityModel.createResource("info:fedora" + ieObject.getPath());
        final List<Representation> reps = new ArrayList<>();
        for (String repUri : getLiteralStrings(entityModel, parent,
                "http://scapeproject.eu/model#hasRepresentation")) {
            reps.add(fetchRepresentation(repUri.substring(11)));
        }
        ie.representations(reps);

        /* fetch the lifecycle state */
        final String state = getFirstLiteralString(entityModel, parent, "http://scapeproject.eu/model#hasLifeCycleState");
        final String details = getFirstLiteralString(entityModel, parent, "http://scapeproject.eu/model#hasLifeCycleStateDetails");
        ie.lifecycleState(new LifecycleState(details, LifecycleState.State.valueOf(state)));

        /* create a streaming METS response using the ScapeMarshaller */
        return Response.ok(new StreamingOutput() {

            @Override
            public void write(OutputStream output) throws IOException,
                    WebApplicationException {
                try {
                    IntellectualEntities.this.marshaller.serialize(ie.build(),
                            output);
                } catch (JAXBException e) {
                    throw new IOException(e);
                }

            }
        }).build();

    }
}
