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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBException;

import org.fcrepo.session.InjectedSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import eu.scape_project.service.ConnectorService;
import eu.scapeproject.model.File;
import eu.scapeproject.util.ScapeMarshaller;

@Component
@Scope("prototype")
@Path("/scape/file")
public class Files {

    private final ScapeMarshaller marshaller;

    @Autowired
    private ConnectorService connectorService;

    @InjectedSession
    private Session session;

    public Files()
            throws JAXBException {
        this.marshaller = ScapeMarshaller.newInstance();
    }

    @GET
    @Path("{entity-id}/{rep-id}/{file-id}")
    public Response retrieveFile(@PathParam("entity-id")
    final String entityId, @PathParam("rep-id")
    final String repId, @PathParam("file-id")
    final String fileId) throws RepositoryException {
        final String path =
                "/" + ConnectorService.ENTITY_FOLDER + "/" + entityId + "/" +
                        repId + "/" + fileId;
        final File f = connectorService.fetchFile(this.session, path);
        return Response.ok().entity(new StreamingOutput() {

            @Override
            public void write(OutputStream output) throws IOException,
                    WebApplicationException {
                try {
                    Files.this.marshaller.serialize(f, output);
                } catch (JAXBException e) {
                    throw new IOException(e);
                }
            }
        }).build();
    }
}
