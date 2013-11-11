/*
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
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

import org.fcrepo.http.commons.session.InjectedSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import eu.scape_project.model.Representation;
import eu.scape_project.service.ConnectorService;
import eu.scape_project.util.ScapeMarshaller;
/**
 * JAX-RS Resource for Representations
 *
 * @author frank asseg
 *
 */

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
        final Representation r = connectorService.fetchRepresentation(this.session, entityId, repId, null);
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

    @GET
    @Path("{entity-id}/{rep-id}/{version-id}")
    public Response retrieveRepresentation(@PathParam("entity-id")
    final String entityId, @PathParam("rep-id")
    final String repId, @PathParam("version-id") final int versionId) throws RepositoryException {
        final Representation r = connectorService.fetchRepresentation(this.session, entityId, repId, versionId);
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
