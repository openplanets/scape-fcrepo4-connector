/**
 *
 */

package eu.scape_project.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

import org.fcrepo.FedoraObject;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.NodeService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.session.InjectedSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import eu.scapeproject.model.IntellectualEntity;
import eu.scapeproject.util.ScapeMarshaller;

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

    @GET
    @Produces(MediaType.TEXT_XML)
    @Path("{id}")
    public Response retrieveEntity(@PathParam("id")
    final String id) {
        return Response.ok().build();
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response ingestEntity(final InputStream src)
            throws RepositoryException {
        try {
            long start = System.currentTimeMillis();
            /* read the post body into an IntellectualEntity object */
            final IntellectualEntity ie =
                    this.marshaller.deserialize(IntellectualEntity.class, src);

            /* create the entity top level object in fcrepo */
            final String entityId = UUID.randomUUID().toString();
            final FedoraObject entityObject =
                    objectService.createObject(this.session, ENTITY_FOLDER +
                            "/" + entityId);

            /* add the metadata datastream for descriptive metadata */
            final PipedInputStream dcSrc = new PipedInputStream();
            final PipedOutputStream dcSink = new PipedOutputStream();
            dcSink.connect(dcSrc);
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        IntellectualEntities.this.marshaller
                                .getJaxbMarshaller().marshal(
                                        ie.getDescriptive(), dcSink);
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
                    datastreamService.createDatastreamNode(this.session,
                            ENTITY_FOLDER + "/" + entityId + "/DESCRIPTIVE",
                            "text/xml", dcSrc);

            /* save the changes made to the objects */
            this.session.save();

            LOG.info("ingesting intellectual entity " + entityId + " took " +
                    (System.currentTimeMillis() - start) + " ms");
            return Response.ok(entityObject.getPath(), MediaType.TEXT_PLAIN)
                    .status(201)
                    .build();
        } catch (JAXBException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new RepositoryException(e);
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new RepositoryException(e);
        } catch (InvalidChecksumException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new RepositoryException(e);
        }
    }

}
