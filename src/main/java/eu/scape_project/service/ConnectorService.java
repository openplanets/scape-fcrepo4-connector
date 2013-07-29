/**
 *
 */

package eu.scape_project.service;

import info.lc.xmlns.premis_v2.PremisComplexType;
import info.lc.xmlns.premis_v2.RightsComplexType;
import info.lc.xmlns.textmd_v3.TextMD;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Source;
import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.fcrepo.Datastream;
import org.fcrepo.FedoraObject;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.rdf.SerializationUtils;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.NodeService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.session.SessionFactory;
import org.purl.dc.elements._1.ElementContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.books.gbs.GbsType;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import edu.harvard.hul.ois.xml.ns.fits.fits_output.Fits;
import eu.scape_project.util.ContentTypeInputStream;
import eu.scapeproject.model.BitStream;
import eu.scapeproject.model.File;
import eu.scapeproject.model.Identifier;
import eu.scapeproject.model.IntellectualEntity;
import eu.scapeproject.model.IntellectualEntityCollection;
import eu.scapeproject.model.LifecycleState;
import eu.scapeproject.model.LifecycleState.State;
import eu.scapeproject.model.Representation;
import eu.scapeproject.model.VersionList;
import eu.scapeproject.util.ScapeMarshaller;
import gov.loc.audiomd.AudioType;
import gov.loc.marc21.slim.RecordType;
import gov.loc.mix.v20.Mix;
import gov.loc.videomd.VideoType;

@Component
public class ConnectorService {

    public final static String ENTITY_FOLDER = "objects/scape/entities";

    public final static String QUEUE_NODE = "/objects/scape/queue";

    //TODO: this should be taken from the UriInfo or at least from a bean config
    private final String fedoraUrl = "http://localhost:8080/rest";

    private final ScapeMarshaller marshaller;

    private static final Logger LOG = LoggerFactory
            .getLogger(ConnectorService.class);

    @Autowired
    private ObjectService objectService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private DatastreamService datastreamService;

    @Autowired
    private SessionFactory sessionFactory;

    private final java.io.File tempDirectory;

    public ConnectorService()
            throws JAXBException {
        marshaller = ScapeMarshaller.newInstance();
        tempDirectory =
                new java.io.File(System.getProperty("java.io.tmpdir") +
                        "/scape-connector-queue");
        if (!tempDirectory.exists()) {
            tempDirectory.mkdir();
        }
    }

    public IntellectualEntity
            fetchEntity(final Session session, final String id)
                    throws RepositoryException {
        return fetchEntity(session, id, null);
    }

    public IntellectualEntity fetchEntity(final Session session,
            final String id, final Integer versionNumber)
            throws RepositoryException {

        final IntellectualEntity.Builder ie = new IntellectualEntity.Builder();
        ie.identifier(new Identifier(id));

        final String entityPath = "/" + ENTITY_FOLDER + "/" + id;
        final FedoraObject ieObject =
                this.objectService.getObject(session, entityPath);
        final Model entityModel =
                SerializationUtils.unifyDatasetModel(ieObject
                        .getPropertiesDataset());
        String versionPath;
        if (versionNumber != null) {
            versionPath = entityPath + "/version-" + versionNumber;
        } else {
            versionPath = getCurrentVersionPath(entityModel, entityPath);
        }

        final FedoraObject versionObject =
                this.objectService.getObject(session, versionPath);
        final Model versionModel =
                SerializationUtils.unifyDatasetModel(versionObject
                        .getPropertiesDataset());

        /* fetch the ie's metadata form the repo */
        ie.descriptive(fetchMetadata(session, versionPath + "/DESCRIPTIVE"));

        /* find all the representations of this entity */
        final Resource versionResource =
                versionModel.createResource("info:fedora" + versionPath);
        final List<Representation> reps = new ArrayList<>();
        for (String repUri : getLiteralStrings(versionModel, versionResource,
                "http://scapeproject.eu/model#hasRepresentation")) {
            reps.add(fetchRepresentation(session, repUri.substring(11)));
        }
        ie.representations(reps);

        /* fetch the lifecycle state */
        final Resource entityResource =
                versionModel.createResource("info:fedora" + entityPath);
        final String state =
                getFirstLiteralString(entityModel, entityResource,
                        "http://scapeproject.eu/model#hasLifeCycleState");
        final String details =
                getFirstLiteralString(entityModel, entityResource,
                        "http://scapeproject.eu/model#hasLifeCycleStateDetails");
        ie.lifecycleState(new LifecycleState(details, LifecycleState.State
                .valueOf(state)));

        return ie.build();
    }

    private String getCurrentVersionPath(Model entityModel, String entityPath)
            throws RepositoryException {
        final Resource parent =
                entityModel.createResource("info:fedora" + entityPath);
        return getFirstLiteralString(entityModel, parent,
                "http://scapeproject.eu/model#currentVersion").substring(11);
    }

    public BitStream fetchBitStream(final Session session, final String bsUri)
            throws RepositoryException {
        final BitStream.Builder bs = new BitStream.Builder();
        bs.identifier(new Identifier(bsUri
                .substring(bsUri.lastIndexOf('/') + 1)));
        bs.technical(fetchMetadata(session, bsUri + "/TECHNICAL"));
        return bs.build();
    }

    public ContentTypeInputStream fetchBinaryFile(final Session session,
            final String entityId, final String repId, final String fileId)
            throws RepositoryException {

        final String entityPath = "/" + ENTITY_FOLDER + "/" + entityId;
        final FedoraObject fo =
                this.objectService.getObject(session, entityPath);
        final Model entityModel =
                SerializationUtils.unifyDatasetModel(fo.getPropertiesDataset());
        final String dsPath =
                this.getCurrentVersionPath(entityModel, entityPath) + "/" +
                        repId + "/" + fileId + "/DATA";

        final Datastream ds =
                this.datastreamService.getDatastream(session, dsPath);

        return new ContentTypeInputStream(ds.getMimeType(), ds.getContent());
    }

    public File fetchFile(final Session session, final String fileUri)
            throws RepositoryException {
        final File.Builder f = new File.Builder();
        final FedoraObject fileObject =
                this.objectService.getObject(session, fileUri);
        final Model repModel =
                SerializationUtils.unifyDatasetModel(fileObject
                        .getPropertiesDataset());
        final Resource parent =
                repModel.createResource("info:fedora" + fileObject.getPath());

        /* fetch and add the properties and metadata from the repo */
        f.technical(fetchMetadata(session, fileUri + "/TECHNICAL"));
        String fileId = fileUri.substring(fileUri.lastIndexOf('/') + 1);
        f.identifier(new Identifier(fileId));
        f.filename(getFirstLiteralString(repModel, parent,
                "http://scapeproject.eu/model#hasFileName"));
        f.mimetype(getFirstLiteralString(repModel, parent,
                "http://scapeproject.eu/model#hasMimeType"));
        String[] ids = fileUri.split("/");
        f.uri(URI.create(fedoraUrl + "/scape/file/" + ids[ids.length - 4] +
                "/" + ids[ids.length - 2] + "/" + ids[ids.length - 1]));

        /* discover all the Bistreams and add them to the file */
        final List<BitStream> streams = new ArrayList<>();
        for (String bsUri : getLiteralStrings(repModel, parent,
                "http://scapeproject.eu/model#hasBitStream")) {
            streams.add(fetchBitStream(session, bsUri.substring(11)));
        }
        f.bitStreams(streams);

        return f.build();
    }

    public Object
            fetchCurrentMetadata(final Session session, final String path)
                    throws RepositoryException {

        String[] ids = path.substring(ENTITY_FOLDER.length() + 2).split("/");
        String entityPath = "/" + ENTITY_FOLDER + "/" + ids[0];
        final FedoraObject entityObject =
                objectService.getObject(session, entityPath);

        StringBuilder versionPath = new StringBuilder();
        versionPath.append(this.getCurrentVersionPath(SerializationUtils
                .unifyDatasetModel(entityObject.getPropertiesDataset()),
                entityPath));
        for (int i = 1; i < ids.length; i++) {
            versionPath.append("/");
            versionPath.append(ids[i]);
        }

        try {
            if (!this.datastreamService.exists(session, versionPath.toString())) {
                return null;
            }
            final Datastream mdDs =
                    this.datastreamService.getDatastream(session, versionPath
                            .toString());
            return this.marshaller.deserialize(mdDs.getContent());
        } catch (JAXBException e) {
            throw new RepositoryException(e);
        }
    }

    public Object fetchMetadata(final Session session, final String path)
            throws RepositoryException {

        try {
            if (!this.datastreamService.exists(session, path)) {
                return null;
            }
            final Datastream mdDs =
                    this.datastreamService.getDatastream(session, path);
            return this.marshaller.deserialize(mdDs.getContent());
        } catch (JAXBException e) {
            throw new RepositoryException(e);
        }
    }

    public Representation fetchRepresentation(final Session session,
            final String repPath) throws RepositoryException {
        final Representation.Builder rep = new Representation.Builder();
        final FedoraObject repObject =
                this.objectService.getObject(session, repPath);
        final Model repModel =
                SerializationUtils.unifyDatasetModel(repObject
                        .getPropertiesDataset());
        final Resource parent =
                repModel.createResource("info:fedora" + repObject.getPath());

        /* find the title and id */
        rep.identifier(new Identifier(repPath.substring(repPath
                .lastIndexOf('/') + 1)));
        rep.title(getFirstLiteralString(repModel, parent,
                "http://scapeproject.eu/model#hasTitle"));

        /* find and add the metadata */
        rep.technical(fetchMetadata(session, repObject.getPath() + "/TECHNICAL"));
        rep.source(fetchMetadata(session, repObject.getPath() + "/SOURCE"));
        rep.provenance(fetchMetadata(session, repObject.getPath() +
                "/PROVENANCE"));
        rep.rights(fetchMetadata(session, repObject.getPath() + "/RIGHTS"));

        /* add the individual files */
        final List<File> files = new ArrayList<>();
        for (String fileUri : getLiteralStrings(repModel, parent,
                "http://scapeproject.eu/model#hasFile")) {
            files.add(fetchFile(session, fileUri.substring(12)));
        }

        rep.files(files);
        return rep.build();
    }

    public Representation fetchRepresentation(final Session session,
            final String entityId, String repId) throws RepositoryException {

        final String entityPath = "/" + ENTITY_FOLDER + "/" + entityId;
        final FedoraObject fo =
                this.objectService.getObject(session, entityPath);
        final Model entityModel =
                SerializationUtils.unifyDatasetModel(fo.getPropertiesDataset());
        final String repPath =
                this.getCurrentVersionPath(entityModel, entityPath) + "/" +
                        repId;
        return this.fetchRepresentation(session, repPath);
    }

    public VersionList fetchVersionList(final Session session, final String entityId) throws RepositoryException{
        final String entityPath = "/" + ENTITY_FOLDER + "/" + entityId;
        final FedoraObject entityObject =
                this.objectService.getObject(session, entityPath);
        final Model model = SerializationUtils.unifyDatasetModel(entityObject.getPropertiesDataset());
        final Resource subject = model.createResource("info:fedora" + entityPath);
        return new VersionList(entityId, getLiteralStrings(model, subject, "http://scapeproject.eu/model#hasVersion"));
    }


    public String addEntity(final Session session, final InputStream src)
            throws RepositoryException {
        return addEntity(session, src, null);
    }

    public String addEntity(final Session session, final InputStream src,
            String entityId) throws RepositoryException {
        try {

            /* read the post body into an IntellectualEntity object */
            final IntellectualEntity ie =
                    this.marshaller.deserialize(IntellectualEntity.class, src);
            final StringBuilder sparql = new StringBuilder();

            if (entityId == null) {
                if (ie.getIdentifier() != null) {
                    entityId = ie.getIdentifier().getValue();
                } else {
                    entityId = UUID.randomUUID().toString();
                }

            }
            /* create the entity top level object in fcrepo as a first version */
            final String entityPath = ENTITY_FOLDER + "/" + entityId;
            final String versionPath = entityPath + "/version-1";

            if (this.objectService.exists(session, "/" + entityPath)) {
                /* return a 409: Conflict result */
                throw new ItemExistsException("Entity '" + entityId +
                        "' already exists");
            }

            final FedoraObject entityObject =
                    objectService.createObject(session, entityPath);
            entityObject.getNode().addMixin("scape:intellectual-entity");

            final FedoraObject versionObject =
                    objectService.createObject(session, versionPath);

            /* add the metadata datastream for descriptive metadata */
            sparql.append(addMetadata(session, ie.getDescriptive(),
                    versionPath + "/DESCRIPTIVE"));

            /* add all the representations */
            sparql.append(addRepresentations(session, ie.getRepresentations(),
                    versionPath));

            /* update the intellectual entity's properties */
            sparql.append("INSERT {<info:fedora/" + entityPath +
                    "> <http://scapeproject.eu/model#hasLifeCycleState> \"" +
                    LifecycleState.State.INGESTED + "\"} WHERE {};");
            sparql.append("INSERT {<info:fedora/" +
                    entityPath +
                    "> <http://scapeproject.eu/model#hasLifeCycleStateDetails> \"successfully ingested at " +
                    new Date().getTime() + "\"} WHERE {};");
            sparql.append("INSERT {<info:fedora/" + entityPath +
                    "> <http://scapeproject.eu/model#hasType> \"intellectualentity\"} WHERE {};");
            sparql.append("INSERT {<info:fedora/" +
                    entityPath +
                    "> <http://scapeproject.eu/model#hasVersion> \"info:fedora/" +
                    versionPath + "\"} WHERE {};");
            sparql.append("INSERT {<info:fedora/" +
                    entityPath +
                    "> <http://scapeproject.eu/model#currentVersion>  \"info:fedora/" +
                    versionPath + "\"} WHERE {};");

            /* update the object and it's child's using sparql */
            entityObject.updatePropertiesDataset(sparql.toString());

            /* save the changes made to the objects */
            session.save();

            return entityId;

        } catch (JAXBException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new RepositoryException(e);
        }
    }

    private String addBitStreams(final Session session,
            final List<BitStream> bitStreams, final String filePath)
            throws RepositoryException {

        final StringBuilder sparql = new StringBuilder();

        for (BitStream bs : bitStreams) {
            final String bsId =
                    (bs.getIdentifier() != null) ? bs.getIdentifier()
                            .getValue() : UUID.randomUUID().toString();
            final String bsPath = filePath + "/" + bsId;
            final FedoraObject bsObject =
                    this.objectService.createObject(session, bsPath);
            sparql.append(addMetadata(session, bs.getTechnical(), bsPath +
                    "/TECHNICAL"));

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

    private String addFiles(final Session session, final List<File> files,
            final String repPath) throws RepositoryException {

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
                        this.objectService.createObject(session, filePath);
                fileObject.getNode().addMixin("scape:file");

                /* add the binary data referenced in the file as a datastream */
                final Node fileDs =
                        this.datastreamService.createDatastreamNode(session,
                                filePath + "/DATA", f.getMimetype(), src);

                /* add the metadata */
                sparql.append(addMetadata(session, f.getTechnical(), filePath +
                        "/TECHNICAL"));

                /* add all bitstreams as child objects */
                sparql.append(addBitStreams(session, f.getBitStreams(),
                        filePath));

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

    private String addMetadata(final Session session, final Object descriptive,
            final String path) throws RepositoryException {
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
                        ConnectorService.this.marshaller.getJaxbMarshaller()
                                .marshal(descriptive, dcSink);
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
                    datastreamService.createDatastreamNode(session, path,
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

    private String
            addRepresentations(final Session session,
                    final List<Representation> representations,
                    final String versionPath) throws RepositoryException {
        final StringBuilder sparql = new StringBuilder();
        for (Representation rep : representations) {

            final String repId =
                    (rep.getIdentifier() != null) ? rep.getIdentifier()
                            .getValue() : UUID.randomUUID().toString();
            final String repPath = versionPath + "/" + repId;
            final FedoraObject repObject =
                    objectService.createObject(session, repPath);
            repObject.getNode().addMixin("scape:representation");

            /* add the metadatasets of the rep as datastreams */
            sparql.append(addMetadata(session, rep.getTechnical(), repPath +
                    "/TECHNICAL"));
            sparql.append(addMetadata(session, rep.getSource(), repPath +
                    "/SOURCE"));
            sparql.append(addMetadata(session, rep.getRights(), repPath +
                    "/RIGHTS"));
            sparql.append(addMetadata(session, rep.getProvenance(), repPath +
                    "/PROVENANCE"));

            /* add all the files */
            sparql.append(addFiles(session, rep.getFiles(), repPath));

            /* add a sparql query to set the type of this object */
            sparql.append("INSERT {<info:fedora" + repObject.getPath() +
                    "> <http://scapeproject.eu/model#hasType> \"representation\"} WHERE {};");
            sparql.append("INSERT {<info:fedora" + repObject.getPath() +
                    "> <http://scapeproject.eu/model#hasTitle> \"" +
                    rep.getTitle() + "\"} WHERE {};");
            sparql.append("INSERT {<info:fedora/" +
                    versionPath +
                    "> <http://scapeproject.eu/model#hasRepresentation> <info:fedora" +
                    repObject.getPath() + ">} WHERE {};");

        }
        return sparql.toString();
    }

    public void updateEntity(final Session session, final InputStream src,
            final String entityId) throws RepositoryException {
        final String entityPath = "/" + ENTITY_FOLDER + "/" + entityId;
        final FedoraObject entityObject =
                this.objectService.getObject(session, entityPath);
        /* fetch the current version number from the repo */
        final String oldVersionPath =
                getCurrentVersionPath(
                        SerializationUtils.unifyDatasetModel(entityObject
                                .getPropertiesDataset()), entityPath);
        int versionNumber =
                Integer.parseInt(oldVersionPath.substring(oldVersionPath
                        .lastIndexOf('-') + 1)) + 1;
        final String newVersionPath = entityPath + "/version-" + versionNumber;

        try {
            /* read the post body into an IntellectualEntity object */
            final IntellectualEntity ie =
                    this.marshaller.deserialize(IntellectualEntity.class, src);
            final StringBuilder sparql = new StringBuilder();

            final FedoraObject versionObject =
                    objectService.createObject(session, newVersionPath);

            /* add the metadata datastream for descriptive metadata */
            sparql.append(addMetadata(session, ie.getDescriptive(),
                    newVersionPath + "/DESCRIPTIVE"));

            /* add all the representations */
            sparql.append(addRepresentations(session, ie.getRepresentations(),
                    newVersionPath));

            sparql.append("DELETE {<info:fedora" +
                    entityPath +
                    "> <http://scapeproject.eu/model#currentVersion> \"info:fedora" +
                    oldVersionPath + "\"} WHERE {};");
            sparql.append("INSERT {<info:fedora" +
                    entityPath +
                    "> <http://scapeproject.eu/model#hasVersion> \"info:fedora" +
                    newVersionPath + "\"} WHERE {};");
            sparql.append("INSERT {<info:fedora" +
                    entityPath +
                    "> <http://scapeproject.eu/model#currentVersion>  \"info:fedora" +
                    newVersionPath + "\"} WHERE {};");

            /* update the object and it's child's using sparql */
            entityObject.updatePropertiesDataset(sparql.toString());

            /* save the changes made to the objects */
            session.save();

        } catch (JAXBException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new RepositoryException(e);
        }

    }

    public void updateRepresentation(Session session, String entityId,
            String repId, InputStream src) throws RepositoryException {
        try {
            final Representation rep =
                    (Representation) this.marshaller.deserialize(src);
            final List<Representation> representations = new ArrayList<>();
            final IntellectualEntity orig = this.fetchEntity(session, entityId);
            for (Representation r : orig.getRepresentations()) {
                if (!r.getIdentifier().getValue().equals(repId)) {
                    representations.add(r);
                }
            }
            representations.add(rep);

            final IntellectualEntity ieUpdate =
                    new IntellectualEntity.Builder(orig).representations(
                            representations).build();

            final ByteArrayOutputStream sink = new ByteArrayOutputStream();
            this.marshaller.serialize(ieUpdate, sink);
            this.updateEntity(session, new ByteArrayInputStream(sink
                    .toByteArray()), entityId);

        } catch (JAXBException e) {
            throw new RepositoryException(e);
        }

    }

    public void updateMetadata(final Session session, final String path,
            final InputStream src) throws RepositoryException {
        String[] ids = path.split("/");
        final String entityId = ids[0];
        final String metadataName = ids[ids.length - 1];
        switch (ids.length) {
            case 2:
                /* it's entity metadata */
                updateEntityMetadata(session, entityId, metadataName, src);
                break;
            case 3:
                /* it's rep metadata */
                updateRepresentationMetadata(session, entityId, ids[1],
                        metadataName, src);
                break;
            case 4:
                /* it's file metadata */
                updateFileMetadata(session, entityId, ids[1], ids[2],
                        metadataName, src);
                break;
            case 5:
                /* it's bitstream metadata */
                updateBitStreamMetadata(session, entityId, ids[1], ids[2],
                        ids[3], metadataName, src);
                break;
            default:
                throw new RepositoryException(
                        "Unable to parse path for metadata update");
        }
    }

    private void updateBitStreamMetadata(Session session, String entityId,
            String repId, String fileId, String bsId, String metadataName,
            InputStream src) throws RepositoryException{

        try {

            if (!metadataName.equals("TECHNICAL")) {
                throw new RepositoryException("Unknown metadata type " +
                        metadataName);
            }
            final Object metadata = this.marshaller.deserialize(src);

            final List<Representation> representations = new ArrayList<>();
            final IntellectualEntity orig = this.fetchEntity(session, entityId);
            for (Representation r : orig.getRepresentations()) {
                if (!r.getIdentifier().getValue().equals(repId)) {
                    representations.add(r);
                } else {
                    Representation.Builder newRep = new Representation.Builder(r);
                    List<File> files = new ArrayList<>();
                    for (File f: r.getFiles()){
                        if (!f.getIdentifier().getValue().equals(fileId)){
                            files.add(f);
                        }else{
                            File.Builder newFile = new File.Builder(f);
                            List<BitStream> bitstreams = new ArrayList<>();
                            for (BitStream bs: f.getBitStreams()){
                                if (!bs.getIdentifier().getValue().equals(bsId)){
                                    bitstreams.add(bs);
                                }else{
                                    BitStream newBs = new BitStream.Builder(bs)
                                        .technical(metadata)
                                        .build();
                                    bitstreams.add(newBs);
                                }
                            }
                            newFile.bitStreams(bitstreams);
                            files.add(newFile.build());
                        }
                    }
                    newRep.files(files);
                    representations.add(newRep.build());
                }
            }

            final IntellectualEntity ieUpdate =
                    new IntellectualEntity.Builder(orig).representations(
                            representations).build();

            final ByteArrayOutputStream sink = new ByteArrayOutputStream();
            this.marshaller.serialize(ieUpdate, sink);
            this.updateEntity(session, new ByteArrayInputStream(sink
                    .toByteArray()), entityId);

        } catch (JAXBException e) {
            throw new RepositoryException(e);
        }
    }

    private void updateFileMetadata(Session session, String entityId,
            String repId, String fileId, String metadataName, InputStream src)
            throws RepositoryException {
        try {

            if (!metadataName.equals("TECHNICAL")) {
                throw new RepositoryException("Unknown metadata type " +
                        metadataName);
            }
            final Object metadata = this.marshaller.deserialize(src);

            final List<Representation> representations = new ArrayList<>();
            final IntellectualEntity orig = this.fetchEntity(session, entityId);
            for (Representation r : orig.getRepresentations()) {
                if (!r.getIdentifier().getValue().equals(repId)) {
                    representations.add(r);
                } else {
                    Representation.Builder newRep = new Representation.Builder(r);
                    List<File> files = new ArrayList<>();
                    for (File f: r.getFiles()){
                        if (!f.getIdentifier().getValue().equals(fileId)){
                            files.add(f);
                        }else{
                            File newFile = new File.Builder(f)
                                .technical(metadata)
                                .build();
                            files.add(newFile);
                        }
                    }
                    newRep.files(files);
                    representations.add(newRep.build());
                }
            }

            final IntellectualEntity ieUpdate =
                    new IntellectualEntity.Builder(orig).representations(
                            representations).build();

            final ByteArrayOutputStream sink = new ByteArrayOutputStream();
            this.marshaller.serialize(ieUpdate, sink);
            this.updateEntity(session, new ByteArrayInputStream(sink
                    .toByteArray()), entityId);

        } catch (JAXBException e) {
            throw new RepositoryException(e);
        }
    }

    private void updateRepresentationMetadata(Session session, String entityId,
            String repId, String metadataName, InputStream src)
            throws RepositoryException {

        try {

            if (!(metadataName.equals("TECHNICAL") ||
                    metadataName.equals("SOURCE") ||
                    metadataName.equals("PROVENANCE") || metadataName
                        .equals("RIGHTS"))) {
                throw new RepositoryException("Unknown metadata type " +
                        metadataName);
            }
            final Object metadata = this.marshaller.deserialize(src);

            final List<Representation> representations = new ArrayList<>();
            final IntellectualEntity orig = this.fetchEntity(session, entityId);
            for (Representation r : orig.getRepresentations()) {
                if (!r.getIdentifier().getValue().equals(repId)) {
                    representations.add(r);
                } else {
                    Representation.Builder newRep =
                            new Representation.Builder(r);
                    if (metadataName.equals("TECHNICAL")) {
                        newRep.technical(metadata);
                    } else if (metadataName.equals("SOURCE")) {
                        newRep.source(metadata);
                    } else if (metadataName.equals("PROVENANCE")) {
                        newRep.provenance(metadata);
                    } else if (metadataName.equals("RIGHTS")) {
                        newRep.rights(metadata);
                    }
                    representations.add(newRep.build());
                }
            }

            final IntellectualEntity ieUpdate =
                    new IntellectualEntity.Builder(orig).representations(
                            representations).build();

            final ByteArrayOutputStream sink = new ByteArrayOutputStream();
            this.marshaller.serialize(ieUpdate, sink);
            this.updateEntity(session, new ByteArrayInputStream(sink
                    .toByteArray()), entityId);

        } catch (JAXBException e) {
            throw new RepositoryException(e);
        }
    }

    private void updateEntityMetadata(Session session, String entityId,
            String metadataName, InputStream src) throws RepositoryException {
        try {
            final IntellectualEntity orig = this.fetchEntity(session, entityId);
            if (!metadataName.equals("DESCRIPTIVE")) {
                throw new RepositoryException("Unknown metadata type " +
                        metadataName);
            }
            final Object desc = this.marshaller.deserialize(src);
            final IntellectualEntity ieUpdate =
                    new IntellectualEntity.Builder(orig).descriptive(desc)
                            .build();

            final ByteArrayOutputStream sink = new ByteArrayOutputStream();
            this.marshaller.serialize(ieUpdate, sink);
            this.updateEntity(session, new ByteArrayInputStream(sink
                    .toByteArray()), entityId);

        } catch (JAXBException e) {
            throw new RepositoryException(e);
        }
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

    public String queueEntityForIngest(final Session session,
            final InputStream src) throws RepositoryException {
        final String fileName = UUID.randomUUID().toString() + ".tmp";
        final java.io.File tmp = new java.io.File(tempDirectory, fileName);
        try {
            if (!tmp.createNewFile()) {
                throw new RepositoryException("temporary file for id'" +
                        fileName + "' does already exist");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RepositoryException(e);
        }

        /* copy the data to a temp location */
        try (final FileOutputStream sink = new FileOutputStream(tmp)) {
            IOUtils.copy(src, sink);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }

        /* update the ingest queue */
        final FedoraObject queue =
                this.objectService.getObject(session, QUEUE_NODE);
        final String sparql =
                "INSERT {<info:fedora/" + QUEUE_NODE +
                        "> <http://scapeproject.eu/model#hasItem> \"" +
                        tmp.getAbsolutePath() + "\"} WHERE {}";
        queue.updatePropertiesDataset(sparql);
        session.save();
        return fileName.substring(0, fileName.length() - 4);
    }

    public LifecycleState fetchLifeCycleState(Session session, String entityId)
            throws RepositoryException {
        /* check if the entity exists */
        if (this.objectService.exists(session, "/" + ENTITY_FOLDER + "/" +
                entityId)) {
            /* fetch the state form the entity itself */
            final FedoraObject entityObject =
                    this.objectService.getObject(session, "/" + ENTITY_FOLDER +
                            "/" + entityId);
            final Model entityModel =
                    SerializationUtils.unifyDatasetModel(entityObject
                            .getPropertiesDataset());
            final Resource parent =
                    entityModel.createResource("info:fedora" +
                            entityObject.getPath());
            final String state =
                    this.getFirstLiteralString(entityModel, parent,
                            "http://scapeproject.eu/model#hasLifeCycleState");
            final String details =
                    this.getFirstLiteralString(entityModel, parent,
                            "http://scapeproject.eu/model#hasLifeCycleStateDetails");
            return new LifecycleState(details, LifecycleState.State
                    .valueOf(state));
        } else {
            /* check the async queue for the id */
            final FedoraObject queueObject =
                    this.objectService.getObject(session, QUEUE_NODE);
            final Model queueModel =
                    SerializationUtils.unifyDatasetModel(queueObject
                            .getPropertiesDataset());
            final Resource parent =
                    queueModel.createResource("info:fedora" +
                            queueObject.getPath());
            final List<String> asyncIds =
                    this.getLiteralStrings(queueModel, parent,
                            "http://scapeproject.eu/model#hasItem");
            if (asyncIds.contains(tempDirectory + "/" + entityId + ".tmp")) {
                return new LifecycleState("", State.INGESTING);
            }
        }
        throw new ItemNotFoundException("Unable to find lifecycle for '" +
                entityId + "'");

    }

    @Scheduled(fixedDelay = 1000, initialDelay = 5000)
    public void ingestFromQueue() throws RepositoryException {
        final Session session = sessionFactory.getSession();
        if (!this.objectService.exists(session, QUEUE_NODE)) {
            return;
        }
        for (String item : getItemsFromQueue(session)) {
            try {
                addEntity(session, new FileInputStream(item), item.substring(
                        item.lastIndexOf('/') + 1, item.length() - 4));
                deleteFromQueue(session, item);
            } catch (IOException e) {
                LOG.error("Error while processing async ingest", e);
            }
        }
        session.logout();
    }

    private void deleteFromQueue(final Session session, final String item)
            throws RepositoryException {
        final FedoraObject queueObject =
                this.objectService.getObject(session, QUEUE_NODE);
        final String sparql =
                "DELETE {<info:fedora/" + QUEUE_NODE +
                        "> <http://scapeproject.eu/model#hasItem> \"" + item +
                        "\"} WHERE {}";
        queueObject.updatePropertiesDataset(sparql);
        session.save();
    }

    private List<String> getItemsFromQueue(final Session session)
            throws RepositoryException {
        final FedoraObject queueObject =
                this.objectService.getObject(session, QUEUE_NODE);
        final Model queueModel =
                SerializationUtils.unifyDatasetModel(queueObject
                        .getPropertiesDataset());
        final Resource parent =
                queueModel
                        .createResource("info:fedora" + queueObject.getPath());
        return this.getLiteralStrings(queueModel, parent,
                "http://scapeproject.eu/model#hasItem");
    }

    public IntellectualEntityCollection fetchEntites(final Session session,
            final List<String> paths) throws RepositoryException {
        List<IntellectualEntity> entities = new ArrayList<>();
        for (String path : paths) {
            path = path.substring(path.indexOf("/scape/entity") + 14);
            entities.add(this.fetchEntity(session, path));
        }
        return new IntellectualEntityCollection(entities);
    }

    public List<String> searchEntities(Session session, String terms,
            int offset, int limit) throws RepositoryException {

        return searchObjectOfType(session, "scape:intellectual-entity", terms,
                offset, limit);
    }

    public List<String> searchRepresentations(Session session, String terms,
            int offset, int limit) throws RepositoryException {
        return searchObjectOfType(session, "scape:representation", terms,
                offset, limit);
    }

    public List<String> searchFiles(Session session, String terms, int offset,
            int limit) throws RepositoryException {
        return searchObjectOfType(session, "scape:file", terms, offset, limit);
    }

    public List<String> searchObjectOfType(final Session session,
            final String mixinType, final String terms, final int offset,
            final int limit) throws RepositoryException {
        final QueryManager queryManager =
                session.getWorkspace().getQueryManager();

        final QueryObjectModelFactory factory = queryManager.getQOMFactory();

        final Source selector =
                factory.selector(mixinType, "resourcesSelector");
        final Constraint constraints =
                factory.fullTextSearch("resourcesSelector", null, factory
                        .literal(session.getValueFactory().createValue(terms)));

        final Query query =
                factory.createQuery(selector, constraints, null, null);

        query.setLimit(limit);
        query.setOffset(offset);
        final QueryResult result = query.execute();
        final NodeIterator it = result.getNodes();
        final List<String> uris = new ArrayList<>();
        while (it.hasNext()) {
            Node n = it.nextNode();
            uris.add(n.getPath());
        }
        return uris;
    }
}
