package eu.scape_project.fcrepo.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Before;

import eu.scape_project.model.IntellectualEntity;
import eu.scape_project.util.ScapeMarshaller;

public class AbstractIT {
    protected static final String SCAPE_URL = "http://localhost:8080/rest/scape";

    protected static final String FEDORA_URL = "http://localhost:8080/rest/";

    protected final DefaultHttpClient client = new DefaultHttpClient();

    protected ScapeMarshaller marshaller;

    @Before
    public void setup() throws Exception {
        this.marshaller = ScapeMarshaller.newInstance();
    }

    protected void postEntity(IntellectualEntity ie) throws IOException {
        HttpPost post = new HttpPost(SCAPE_URL + "/entity");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        try {
            this.marshaller.serialize(ie, sink);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
        post.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink
                .toByteArray()), sink.size()));
        HttpResponse resp = this.client.execute(post);
        assertEquals(201, resp.getStatusLine().getStatusCode());
        String id = EntityUtils.toString(resp.getEntity());
        assertTrue(id.length() > 0);
        post.releaseConnection();
    }
}
