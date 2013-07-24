/**
 *
 */

package eu.scapeproject.fcrepo.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
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

import eu.scapeproject.model.File;
import eu.scapeproject.model.IntellectualEntity;
import eu.scapeproject.model.LifecycleState;
import eu.scapeproject.model.LifecycleState.State;
import eu.scapeproject.model.Representation;
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
    public void testIngestIntellectualEntityAndCheckinFedora() throws Exception {
        IntellectualEntity ie = TestUtil.createTestEntity("entity-1");
        HttpPost post = new HttpPost(SCAPE_URL + "/entity");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        this.marshaller.serialize(ie, sink);
        post.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink.toByteArray()), sink.size()));
        long start = System.currentTimeMillis();
        HttpResponse resp = this.client.execute(post);
        LOG.info("INGEST TIME: " + (System.currentTimeMillis() - start));
        assertEquals(201, resp.getStatusLine().getStatusCode());
        String id = EntityUtils.toString(resp.getEntity());
        assertTrue(id.length() > 0);
        post.releaseConnection();

        HttpGet get = new HttpGet(FEDORA_URL + "/objects/scape/entities/" + id);
        resp = this.client.execute(get);
        assertEquals(200,resp.getStatusLine().getStatusCode());
        assertTrue(EntityUtils.toString(resp.getEntity()).length() > 0);
        get.releaseConnection();
    }

    @Test
    public void testIngestAndRetrieveIntellectualEntity() throws Exception {
        IntellectualEntity ie = TestUtil.createTestEntityWithMultipleRepresentations("entity-2");
        HttpPost post = new HttpPost(SCAPE_URL + "/entity");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        this.marshaller.serialize(ie, sink);
        post.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink.toByteArray()), sink.size()));
        long start = System.currentTimeMillis();
        HttpResponse resp = this.client.execute(post);
        LOG.info("INGEST DURATION: " + (System.currentTimeMillis() - start) + " ms");
        assertEquals(201, resp.getStatusLine().getStatusCode());
        String id = EntityUtils.toString(resp.getEntity());
        System.out.println(id);
        assertTrue(id.length() > 0);
        post.releaseConnection();

        HttpGet get = new HttpGet(SCAPE_URL + "/entity/" + id);
        start = System.currentTimeMillis();
        resp = this.client.execute(get);
        LOG.info("RETRIEVE DURATION: " + (System.currentTimeMillis() - start) + " ms");
        assertEquals(200,resp.getStatusLine().getStatusCode());
        IntellectualEntity fetched = this.marshaller.deserialize(IntellectualEntity.class, resp.getEntity().getContent());
        assertEquals(ie.getIdentifier(),fetched.getIdentifier());
        assertEquals(LifecycleState.State.INGESTED, fetched.getLifecycleState().getState());
        assertEquals(ie.getRepresentations().size(), fetched.getRepresentations().size());
        get.releaseConnection();
    }

    @Test
    public void testIngestAndRetrieveFile() throws Exception {
        IntellectualEntity ie = TestUtil.createTestEntityWithMultipleRepresentations("entity-3");
        HttpPost post = new HttpPost(SCAPE_URL + "/entity");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        this.marshaller.serialize(ie, sink);
        post.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink.toByteArray()), sink.size()));
        long start = System.currentTimeMillis();
        HttpResponse resp = this.client.execute(post);
        LOG.info("INGEST DURATION: " + (System.currentTimeMillis() - start) + " ms");
        assertEquals(201, resp.getStatusLine().getStatusCode());
        String id = EntityUtils.toString(resp.getEntity());
        System.out.println(id);
        assertTrue(id.length() > 0);
        post.releaseConnection();

        Representation rep = ie.getRepresentations().get(0);
        File f = ie.getRepresentations().get(0).getFiles().get(0);
        HttpGet get = new HttpGet(SCAPE_URL + "/file/" + id + "/" + rep.getIdentifier().getValue() + "/" + f.getIdentifier().getValue());
        start = System.currentTimeMillis();
        resp = this.client.execute(get);
        LOG.info("RETRIEVE DURATION: " + (System.currentTimeMillis() - start) + " ms");
        assertEquals(200,resp.getStatusLine().getStatusCode());
        File fetched = this.marshaller.deserialize(File.class, resp.getEntity().getContent());
        assertEquals(f.getIdentifier().getValue(), fetched.getIdentifier().getValue());
        get.releaseConnection();
    }

    @Test
    public void testIngestAndRetrieveLifeCycle() throws Exception {
        IntellectualEntity ie = TestUtil.createTestEntityWithMultipleRepresentations("entity-4");
        HttpPost post = new HttpPost(SCAPE_URL + "/entity");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        this.marshaller.serialize(ie, sink);
        post.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink.toByteArray()), sink.size()));
        HttpResponse resp = this.client.execute(post);
        assertEquals(201, resp.getStatusLine().getStatusCode());
        String id = EntityUtils.toString(resp.getEntity());
        assertTrue(id.length() > 0);

        /* check the lifecycle state */
        HttpGet get = new HttpGet(SCAPE_URL + "/lifecycle/" + id);
        resp = this.client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        LifecycleState state = (LifecycleState) this.marshaller.deserialize(resp.getEntity().getContent());
        assertEquals(LifecycleState.State.INGESTED, state.getState());
        get.releaseConnection();
    }

    @Test
    public void testIngestAsyncAndRetrieveLifeCycle() throws Exception {
        IntellectualEntity ie = TestUtil.createTestEntityWithMultipleRepresentations("entity-5");
        HttpPost post = new HttpPost(SCAPE_URL + "/entity-async");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        this.marshaller.serialize(ie, sink);
        post.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink.toByteArray()), sink.size()));
        HttpResponse resp = this.client.execute(post);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        String id = EntityUtils.toString(resp.getEntity());
        assertTrue(id.length() > 0);

        /* check the lifecycle state and wait for the entity to be ingested*/
        LifecycleState state;
        long start = System.currentTimeMillis();
        do {
            HttpGet get = new HttpGet(SCAPE_URL + "/lifecycle/" + id);
            resp = this.client.execute(get);
            assertEquals(200, resp.getStatusLine().getStatusCode());
            state = (LifecycleState) this.marshaller.deserialize(resp.getEntity().getContent());
            get.releaseConnection();
        }while(!state.getState().equals(State.INGESTED) && (System.currentTimeMillis() - start) < 15000);
        assertEquals(State.INGESTED, state.getState());
    }
}
