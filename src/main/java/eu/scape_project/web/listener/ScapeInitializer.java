/**
 *
 */

package eu.scape_project.web.listener;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.ws.rs.ext.Provider;

import org.fcrepo.RdfLexicon;
import org.fcrepo.services.NodeService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.session.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.update.UpdateAction;
import com.sun.jersey.api.model.AbstractResourceModelContext;
import com.sun.jersey.api.model.AbstractResourceModelListener;

import eu.scape_project.service.ConnectorService;

/**
 * @author frank asseg
 *
 */
@Component
@Provider
public class ScapeInitializer implements AbstractResourceModelListener {

    private static final Logger LOG = LoggerFactory
            .getLogger(ScapeInitializer.class);

    @Autowired
    private NodeService nodeService;

    @Autowired
    private ObjectService objectService;

    @Autowired
    private SessionFactory sessionFactory;

    /* (non-Javadoc)
     * @see com.sun.jersey.api.model.AbstractResourceModelListener#onLoaded(com.sun.jersey.api.model.AbstractResourceModelContext)
     */
    @Override
    public void onLoaded(AbstractResourceModelContext modelContext) {
        try {
            final Session session= this.sessionFactory.getSession();
            /* make sure that the scape namespace is available in fcrepo */
            final Dataset namespace =
                    this.nodeService.getNamespaceRegistryGraph(session);
            UpdateAction.parseExecute(
                    "INSERT {<http://scapeproject.eu/model#> <" +
                            RdfLexicon.HAS_NAMESPACE_PREFIX + "> \"scape\"} WHERE {}",
                    namespace);

            /* make sure that the queue object exists for async ingests */
            this.objectService.createObject(session, ConnectorService.QUEUE_NODE);
            session.save();

            /* add the scape node mixin types */

         // Get the node type manager ...
         final NodeTypeManager mgr = session.getWorkspace().getNodeTypeManager();

         // Create a template for the node type ...
         final NodeTypeTemplate type = mgr.createNodeTypeTemplate();
         type.setName("scape:intellectual-entity");
         type.setDeclaredSuperTypeNames(new String[]{"fedora:resource","nt:folder", "fedora:object"});
         type.setMixin(true);
         type.setQueryable(true);
         type.setAbstract(false);
         // and register it
         mgr.registerNodeType(type, true);
        } catch (RepositoryException e) {
            LOG.error("Error while setting up scape connector api", e);
            throw new RuntimeException("Unable to setup scape on fedora");
        }



    }
}
