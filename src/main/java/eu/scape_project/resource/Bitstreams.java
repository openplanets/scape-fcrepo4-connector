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
import eu.scapeproject.model.BitStream;
import eu.scapeproject.util.ScapeMarshaller;

/**
 * JAX-RS Resource for BitStreams
 *
 * @author frank asseg
 *
 */

@Component
@Scope("prototype")
@Path("/scape/bitstream")
public class Bitstreams {

    private final ScapeMarshaller marshaller;

    @Autowired
    private ConnectorService connectorService;

    @InjectedSession
    private Session session;

    public Bitstreams()
            throws JAXBException {
        marshaller = ScapeMarshaller.newInstance();
    }

    @GET
    @Path("{entity-id}/{rep-id}/{file-id}/{bitstream-id}")
    public Response retrieveBitstream(@PathParam("entity-id")
    final String entityId, @PathParam("rep-id")
    final String repId, @PathParam("file-id")
    final String fileId, @PathParam("bitstream-id")
    final String bsId) throws RepositoryException {
        final String path =
                "/" + ConnectorService.ENTITY_FOLDER + "/" + entityId + "/" +
                        repId + "/" + fileId + "/" + bsId;
        final BitStream bs = connectorService.fetchBitStream(session, path);
        return Response.ok().entity(new StreamingOutput() {

            @Override
            public void write(OutputStream output) throws IOException,
                    WebApplicationException {
                try {
                    Bitstreams.this.marshaller.serialize(bs, output);
                } catch (JAXBException e) {
                    throw new IOException(e);
                }
            }
        }).build();
    }

    @GET
    @Path("{entity-id}/{rep-id}/{file-id}/{bitstream-id}/{version-id}")
    public Response retrieveBitstream(@PathParam("entity-id")
    final String entityId, @PathParam("rep-id")
    final String repId, @PathParam("file-id")
    final String fileId, @PathParam("bitstream-id")
    final String bsId, @PathParam("version-id")
    final String versionId) throws RepositoryException {

        final String path;
        if (versionId == null) {
            path = "/" + ConnectorService.ENTITY_FOLDER + "/" + entityId +
                            "/" + repId + "/" + fileId + "/" + bsId;

        }else{
            path = "/" + ConnectorService.ENTITY_FOLDER + "/" + entityId + "/version-" + versionId +
                    "/" + repId + "/" + fileId + "/" + bsId;

        }
        final BitStream bs = connectorService.fetchBitStream(session, path);
        return Response.ok().entity(new StreamingOutput() {

            @Override
            public void write(OutputStream output) throws IOException,
                    WebApplicationException {
                try {
                    Bitstreams.this.marshaller.serialize(bs, output);
                } catch (JAXBException e) {
                    throw new IOException(e);
                }
            }
        }).build();
    }

}
