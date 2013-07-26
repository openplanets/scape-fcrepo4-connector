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
import eu.scapeproject.util.ScapeMarshaller;

@Component
@Scope("prototype")
@Path("/scape/metadata")
public class Metadata {

    private final ScapeMarshaller marshaller;

    @Autowired
    private ConnectorService connectorService;

    @InjectedSession
    private Session session;

    public Metadata()
            throws JAXBException {
        this.marshaller = ScapeMarshaller.newInstance();
    }
    
	@PUT
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{id}")
    public Response updateEntity(
    		@PathParam("id") final String entityId,
    		final InputStream src)
            throws RepositoryException, JAXBException {
		System.out.println(" asd");
        String id2 = connectorService.updateMetadata(this.session, src, entityId);
        return Response.status(Status.CREATED).entity(id2).build();
    }
	
    @GET
    @Path("{path: .*}")
    public Response retrieveFile(@PathParam("path")
    String path) throws RepositoryException {
        path = "/" + ConnectorService.ENTITY_FOLDER + "/" + path;
        final Object md = connectorService.fetchMetadata(this.session, path);
        return Response.ok().entity(new StreamingOutput() {

            @Override
            public void write(OutputStream output) throws IOException,
                    WebApplicationException {
                try {
                    Metadata.this.marshaller.serialize(md, output);
                } catch (JAXBException e) {
                    throw new IOException(e);
                }
            }
        }).build();
    }
}
