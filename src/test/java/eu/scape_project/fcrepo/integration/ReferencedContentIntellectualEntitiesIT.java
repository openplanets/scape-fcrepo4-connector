package eu.scape_project.fcrepo.integration;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import eu.scape_project.model.File;
import eu.scape_project.model.IntellectualEntity;
import eu.scape_project.model.Representation;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/integration-tests/referenced-content/test-container.xml"})
@DirtiesContext(classMode=ClassMode.AFTER_CLASS)
@Ignore
public class ReferencedContentIntellectualEntitiesIT extends AbstractIT{

	
	private static final Logger LOG = LoggerFactory.getLogger(ReferencedContentIntellectualEntitiesIT.class);

	@Test
	public void testIngestIntellectualEntityAndCheckRedirectForBinary() throws Exception {
        HttpPost post = new HttpPost(SCAPE_URL + "/entity");
        post.setEntity(new InputStreamEntity(this.getClass().getClassLoader().getResourceAsStream("ONB_mets_example.xml"), -1, ContentType.TEXT_XML));
        HttpResponse resp = this.client.execute(post);
        String id = EntityUtils.toString(resp.getEntity());
        post.releaseConnection();
        
        HttpGet get = new HttpGet(SCAPE_URL + "/entity/" + id);
        resp = client.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        IntellectualEntity e = this.marshaller.deserialize(IntellectualEntity.class, resp.getEntity().getContent());
        get.releaseConnection();
        
        for (Representation r: e.getRepresentations()) {
        	for (File f: r.getFiles()) {
                get = new HttpGet(SCAPE_URL + "/file/" + e.getIdentifier().getValue() + "/" + r.getIdentifier().getValue() + "/" + f.getIdentifier().getValue());
        		this.client.getParams().setBooleanParameter("http.protocol.handle-redirects", false);
        		resp = this.client.execute(get);
        		assertEquals(307, resp.getStatusLine().getStatusCode());
        		assertEquals(f.getUri().toASCIIString(), resp.getFirstHeader("Location").getValue());
        		get.releaseConnection();
        	}
        }
	}
}