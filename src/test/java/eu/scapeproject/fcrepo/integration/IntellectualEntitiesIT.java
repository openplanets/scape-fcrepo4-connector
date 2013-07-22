/**
 *
 */

package eu.scapeproject.fcrepo.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import eu.scapeproject.model.IntellectualEntity;
import eu.scapeproject.model.TestUtil;
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

    private static final Logger LOG = LoggerFactory
            .getLogger(IntellectualEntitiesIT.class);

    @Before
    public void setup() throws Exception {
        this.marshaller = ScapeMarshaller.newInstance();
    }

    @Test
    public void testIngestIntellectualEntity() throws Exception {
        IntellectualEntity ie = TestUtil.createTestEntity();
        HttpPost post = new HttpPost(SCAPE_URL + "/entity");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        this.marshaller.serialize(ie, sink);
        post.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink.toByteArray()), sink.size()));
        HttpResponse resp = this.client.execute(post);
        assertEquals(201, resp.getStatusLine().getStatusCode());
        String id = EntityUtils.toString(resp.getEntity());
        assertTrue(id.length() > 0);
        post.releaseConnection();
    }
}
