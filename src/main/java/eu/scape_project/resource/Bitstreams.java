/**
 *
 */
package eu.scape_project.resource;

import javax.jcr.Session;
import javax.ws.rs.Path;
import javax.xml.bind.JAXBException;

import org.fcrepo.session.InjectedSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import eu.scape_project.service.ConnectorService;
import eu.scapeproject.util.ScapeMarshaller;


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

}
