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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
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

    @GET
    @Path("{entity-id}/{rep-id}")
    public Response retrieveRepresentation(@PathParam("entity-id")
    final String entityId, @PathParam("rep-id")
    final String repId) throws RepositoryException {
        final Representation r = connectorService.fetchRepresentation(this.session, entityId, repId);
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

    @PUT
    @Path("{entity-id}/{rep-id}")
    public Response updateRepresentation(@PathParam("entity-id") final String entityId, @PathParam("rep-id") final String representationId, final InputStream src) throws RepositoryException{
        this.connectorService.updateRepresentation(session, entityId, representationId, src);
        return Response.ok().build();
    }
}
