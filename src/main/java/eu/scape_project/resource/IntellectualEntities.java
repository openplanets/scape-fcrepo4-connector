/**
 *
 */

package eu.scape_project.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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



    public IntellectualEntities() throws JAXBException {
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
    final String id) throws RepositoryException {
        final IntellectualEntity ie = connectorService.fetchEntity(this.session, id);
        /* create a streaming METS response using the ScapeMarshaller */
        return Response.ok(new StreamingOutput() {

            @Override
            public void write(OutputStream output) throws IOException,
                    WebApplicationException {
                try {
                    IntellectualEntities.this.marshaller.serialize(ie,
                            output);
                } catch (JAXBException e) {
                    throw new IOException(e);
                }

            }
        }).build();

    }
}
