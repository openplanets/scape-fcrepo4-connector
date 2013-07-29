/**
 *
 */

package eu.scape_project.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBException;

import org.fcrepo.session.InjectedSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import eu.scape_project.service.ConnectorService;
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

    private final ScapeMarshaller marshaller;

    @Autowired
    private ConnectorService connectorService;

    @InjectedSession
    private Session session;

    public IntellectualEntities()
            throws JAXBException {
        this.marshaller = ScapeMarshaller.newInstance();
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response ingestEntity(final InputStream src)
            throws RepositoryException {
        String id = connectorService.addEntity(this.session, src);
        return Response.status(Status.CREATED).entity(id).build();
    }

    @GET
    @Produces(MediaType.TEXT_XML)
    @Path("{id}")
    public Response retrieveEntity(@PathParam("id")
    final String id, @QueryParam("useReferences")
    @DefaultValue("no")
    final String useReferences) throws RepositoryException {

        final boolean refs = useReferences.equalsIgnoreCase("yes");
        final IntellectualEntity ie =
                connectorService.fetchEntity(this.session, id);
        /* create a streaming METS response using the ScapeMarshaller */
        return Response.ok(new StreamingOutput() {

            @Override
            public void write(OutputStream output) throws IOException,
                    WebApplicationException {
                try {
                    IntellectualEntities.this.marshaller.serialize(ie, output,
                            refs);
                } catch (JAXBException e) {
                    throw new IOException(e);
                }

            }
        }).build();

    }

    @GET
    @Produces(MediaType.TEXT_XML)
    @Path("{id}/{versionNumber}")
    public Response retrieveEntity(@PathParam("id")
    final String id, @PathParam("versionNumber") final Integer versionNumber, @QueryParam("useReferences")
    @DefaultValue("no")
    final String useReferences) throws RepositoryException {

        final boolean refs = useReferences.equalsIgnoreCase("yes");
        final IntellectualEntity ie =
                connectorService.fetchEntity(this.session, id, versionNumber);
        /* create a streaming METS response using the ScapeMarshaller */
        return Response.ok(new StreamingOutput() {

            @Override
            public void write(OutputStream output) throws IOException,
                    WebApplicationException {
                try {
                    IntellectualEntities.this.marshaller.serialize(ie, output,
                            refs);
                } catch (JAXBException e) {
                    throw new IOException(e);
                }

            }
        }).build();

    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.TEXT_XML})
    public Response updateEntity(@PathParam("id")
    final String entityId, final InputStream src) throws RepositoryException {
        connectorService.updateEntity(this.session, src, entityId);
        return Response.ok().build();
    }

}
