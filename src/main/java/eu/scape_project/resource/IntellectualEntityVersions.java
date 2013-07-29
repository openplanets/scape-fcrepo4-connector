/**
 *
 */

package eu.scape_project.resource;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBException;

import org.fcrepo.session.InjectedSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import eu.scape_project.service.ConnectorService;
import eu.scapeproject.model.VersionList;
import eu.scapeproject.util.ScapeMarshaller;

/**
 * @author frank asseg
 *
 */
@Component
@Scope("prototype")
@Path("/scape/entity-version-list")
public class IntellectualEntityVersions {

    private final ScapeMarshaller marshaller;

    @Autowired
    private ConnectorService connectorService;

    @InjectedSession
    private Session session;

    public IntellectualEntityVersions() throws JAXBException {
        this.marshaller = ScapeMarshaller.newInstance();
    }

    @GET
    @Produces(MediaType.TEXT_XML)
    @Path("{id}")
    public Response retrieveVersionList(@PathParam("id")
    final String entityId) throws RepositoryException {
        final VersionList list = this.connectorService.fetchVersionList(session, entityId);
        return Response.ok(new StreamingOutput() {

            @Override
            public void write(OutputStream output) throws IOException,
                    WebApplicationException {
                try {
                    IntellectualEntityVersions.this.marshaller.serialize(list, output);
                } catch (JAXBException e) {
                    throw new IOException(e);
                }
            }
        }).build();

    }

}
