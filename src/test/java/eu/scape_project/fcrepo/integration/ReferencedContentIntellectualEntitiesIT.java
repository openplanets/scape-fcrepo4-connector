package eu.scape_project.fcrepo.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import eu.scape_project.model.File;
import eu.scape_project.model.IntellectualEntity;
import eu.scape_project.model.Representation;
import eu.scape_project.model.TestUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/integration-tests/referenced-content/test-container.xml"})
public class ReferencedContentIntellectualEntitiesIT extends AbstractIT{

	
	private static final Logger LOG = LoggerFactory.getLogger(ReferencedContentIntellectualEntitiesIT.class);

	@Test
	public void testIngestIntellectualEntityAndCheckinFedora() throws Exception {
	    IntellectualEntity ie = TestUtil.createTestEntity("entity-1");
	    this.postEntity(ie);
	    HttpGet get =
	            new HttpGet(SCAPE_URL + "/file/entity-1/representation-1/file-1");
	    HttpResponse resp = this.client.execute(get);
	    assertEquals(200, resp.getStatusLine().getStatusCode());
	    System.out.println(EntityUtils.toString(resp.getEntity()));
	    IntellectualEntity e = this.marshaller.deserialize(IntellectualEntity.class, resp.getEntity().getContent());
	    get.releaseConnection();
	    for (Representation r: e.getRepresentations()) {
	    	for (File f: r.getFiles()) {
	    		System.out.println(f.getUri());
	    	}
	    }
	}
	
	
}
