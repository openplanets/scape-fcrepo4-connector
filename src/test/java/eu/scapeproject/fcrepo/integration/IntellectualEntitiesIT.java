/**
 *
 */
package eu.scapeproject.fcrepo.integration;

import static org.junit.Assert.fail;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import eu.scapeproject.util.ScapeMarshaller;


/**
 * @author frank asseg
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/integration-tests/test-container.xml"})
public class IntellectualEntitiesIT {

    private static final String SCAPE_URL = "http://localhost:8080/rest/scape";

    private static final String FEDORA_URL = "http://localhost:8080/rest/";

    private final DefaultHttpClient client = new DefaultHttpClient();

    private ScapeMarshaller marshaller;

    private static final Logger LOG = LoggerFactory.getLogger(IntellectualEntitiesIT.class);

    @Before
    public void setup() throws Exception {
        this.marshaller = ScapeMarshaller.newInstance();
    }

    @Test
    public void testIngestIntellectualEntity() throws Exception {
        fail("this is just a dummy");
    }

}
