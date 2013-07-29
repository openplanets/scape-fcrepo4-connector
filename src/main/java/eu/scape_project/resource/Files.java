/**
 *
 */

package eu.scape_project.resource;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

import org.fcrepo.session.InjectedSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import eu.scape_project.service.ConnectorService;
import eu.scape_project.util.ContentTypeInputStream;
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
        final ContentTypeInputStream src = connectorService.fetchBinaryFile(this.session, entityId, repId, fileId, null);
        return Response.ok().entity(src).type(src.getContentType()).build();
    }


    @GET
    @Path("{entity-id}/{rep-id}/{file-id}/{version-id}")
    public Response retrieveFile(@PathParam("entity-id")
    final String entityId, @PathParam("rep-id")
    final String repId, @PathParam("file-id")
    final String fileId,@PathParam("version-id")
    final String versionId) throws RepositoryException {
        final ContentTypeInputStream src = connectorService.fetchBinaryFile(this.session, entityId, repId, fileId, versionId);
        return Response.ok().entity(src).type(src.getContentType()).build();
    }
}
