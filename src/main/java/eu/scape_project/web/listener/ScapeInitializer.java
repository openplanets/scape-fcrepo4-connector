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

package eu.scape_project.web.listener;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.ws.rs.ext.Provider;

import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.http.commons.session.SessionFactory;
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
 * A JAX-RS Provider which initializes the webapp
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

    /*
     * (non-Javadoc)
     * @see
     * com.sun.jersey.api.model.AbstractResourceModelListener#onLoaded(com.sun
     * .jersey.api.model.AbstractResourceModelContext)
     */
    @Override
    public void onLoaded(AbstractResourceModelContext modelContext) {
        try {
            final Session session = this.sessionFactory.getSession();
            /* make sure that the scape namespace is available in fcrepo */
            final Dataset namespace =
                    this.nodeService.getNamespaceRegistryGraph(session);
            UpdateAction.parseExecute(
                    "INSERT {<http://scapeproject.eu/model#> <" +
                            RdfLexicon.HAS_NAMESPACE_PREFIX +
                            "> \"scape\"} WHERE {}", namespace);

            /* make sure that the queue object exists for async ingests */
            this.objectService.createObject(session,
                    ConnectorService.QUEUE_NODE);
            session.save();

            /* add the scape node mixin types */

            // Get the node type manager ...
            final NodeTypeManager mgr =
                    session.getWorkspace().getNodeTypeManager();

            // Create templates for the node types ...
            final NodeTypeTemplate entityType = mgr.createNodeTypeTemplate();
            entityType.setName("scape:intellectual-entity");
            entityType.setDeclaredSuperTypeNames(new String[] {
                    "fedora:resource", "nt:folder", "fedora:object"});
            entityType.setMixin(true);
            entityType.setQueryable(true);
            entityType.setAbstract(false);

            final NodeTypeTemplate repType = mgr.createNodeTypeTemplate();
            repType.setName("scape:representation");
            repType.setDeclaredSuperTypeNames(new String[] {"fedora:resource",
                    "nt:folder", "fedora:object"});
            repType.setMixin(true);
            repType.setQueryable(true);
            repType.setAbstract(false);

            final NodeTypeTemplate fileType = mgr.createNodeTypeTemplate();
            fileType.setName("scape:file");
            fileType.setDeclaredSuperTypeNames(new String[] {"fedora:resource",
                    "nt:folder", "fedora:object"});
            fileType.setMixin(true);
            fileType.setQueryable(true);
            fileType.setAbstract(false);

            // and register them
            mgr.registerNodeTypes(new NodeTypeDefinition[] {fileType,
                    entityType, repType}, true);
        } catch (RepositoryException e) {
            LOG.error("Error while setting up scape connector api", e);
            throw new RuntimeException("Unable to setup scape on fedora");
        }

    }
}
