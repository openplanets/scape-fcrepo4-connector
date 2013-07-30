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
import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.fcrepo.session.InjectedSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import eu.scape_project.service.ConnectorService;
import eu.scapeproject.model.IntellectualEntityCollection;
import eu.scapeproject.util.ScapeMarshaller;

/**
 * JAX-RS Resource for Intellectual Entity Collections
 *
 * @author frank asseg
 *
 */
@Component
@Scope("prototype")
@Path("/scape/entity-list")
public class IntellectualEntityCollections {

    private final ScapeMarshaller marshaller;

    @Autowired
    private ConnectorService connectorService;

    @InjectedSession
    private Session session;

    public IntellectualEntityCollections()
            throws JAXBException {
        this.marshaller = ScapeMarshaller.newInstance();
    }

    @POST
    @Produces(MediaType.TEXT_XML)
    @Consumes("text/uri-list")
    public Response retrieveEntityCollection(final InputStream src)
            throws RepositoryException {
        try {
            List<String> paths =
                    Arrays.asList(IOUtils.toString(src).split("\n"));

            final IntellectualEntityCollection entities =
                    connectorService.fetchEntites(this.session, paths);

            /* create a streaming METS response using the ScapeMarshaller */
            return Response.ok(new StreamingOutput() {

                @Override
                public void write(OutputStream output) throws IOException,
                        WebApplicationException {
                    try {
                        IntellectualEntityCollections.this.marshaller
                                .serialize(entities, output);
                    } catch (JAXBException e) {
                        throw new IOException(e);
                    }

                }
            }).build();
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }
}
