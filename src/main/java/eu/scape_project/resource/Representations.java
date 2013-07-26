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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBException;

import org.fcrepo.session.InjectedSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import eu.scape_project.service.ConnectorService;
import eu.scapeproject.model.Representation;
import eu.scapeproject.util.ScapeMarshaller;

@Component
@Scope("prototype")
@Path("/scape/representation")
public class Representations {

    private final ScapeMarshaller marshaller;

    @Autowired
    private ConnectorService connectorService;

    @InjectedSession
    private Session session;

    public Representations()
            throws JAXBException {
        this.marshaller = ScapeMarshaller.newInstance();
    }
    
	@PUT
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{entity-id}/{rep-id}")
    public Response updateEntity(
    		@PathParam("entity-id") final String entityId, 
    		@PathParam("rep-id") final String repId, 
    		final InputStream src)
            throws RepositoryException, JAXBException {
        String id2 = connectorService.updateRepresentation(this.session, src, entityId, repId);
        return Response.status(Status.CREATED).entity(id2).build();
    }

    @GET
    @Path("{entity-id}/{rep-id}")
    public Response retrieveFile(@PathParam("entity-id")
    final String entityId, @PathParam("rep-id")
    final String repId) throws RepositoryException {
        final String path =
                "/" + ConnectorService.ENTITY_FOLDER + "/" + entityId + "/" +
                        repId;
        final Representation r = connectorService.fetchRepresentation(this.session, path);
        return Response.ok().entity(new StreamingOutput() {

            @Override
            public void write(OutputStream output) throws IOException,
                    WebApplicationException {
                try {
                    Representations.this.marshaller.serialize(r, output);
                } catch (JAXBException e) {
                    throw new IOException(e);
                }
            }
        }).build();
    }
}
