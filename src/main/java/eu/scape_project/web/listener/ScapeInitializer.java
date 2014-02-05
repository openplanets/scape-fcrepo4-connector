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

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import javax.ws.rs.ext.Provider;

import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.ObjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.update.UpdateAction;
import com.sun.jersey.api.model.AbstractResourceModelContext;
import com.sun.jersey.api.model.AbstractResourceModelListener;

import eu.scape_project.rdf.ScapeRDFVocabulary;
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
            final Session session = this.sessionFactory.getInternalSession();
            /* make sure that the scape namespace is available in fcrepo */
            final Dataset namespace =
                    this.nodeService.getNamespaceRegistryDataset(session);
            UpdateAction.parseExecute(
                    "INSERT {<" + ScapeRDFVocabulary.SCAPE_NAMESPACE + "> <" +
                            RdfLexicon.HAS_NAMESPACE_PREFIX +
                            "> \"scape\"} WHERE {}", namespace);


            /* add the scape node mixin types */

            // Get the node type manager ...
            final NodeTypeManager mgr =
                    session.getWorkspace().getNodeTypeManager();

            // Create templates for the node types ...
            final NodeTypeTemplate entityType = mgr.createNodeTypeTemplate();
            entityType.setName("scape:intellectual-entity");
            entityType.setDeclaredSuperTypeNames(new String[] {
                    "fedora:resource", "fedora:object"});
            entityType.setMixin(true);
            entityType.setQueryable(true);
            entityType.setAbstract(false);
            entityType.getPropertyDefinitionTemplates().add(createMultiPropertyDefTemplate(session, mgr, "scape:hasRepresentation"));
            entityType.getPropertyDefinitionTemplates().add(createMultiPropertyDefTemplate(session, mgr, "scape:hasVersion"));


            final NodeTypeTemplate versionType = mgr.createNodeTypeTemplate();
            versionType.setName("scape:intellectual-entity-version");
            versionType.setDeclaredSuperTypeNames(new String[] {
                    "fedora:resource", "fedora:object"});
            versionType.setMixin(true);
            versionType.setQueryable(true);
            versionType.setAbstract(false);
            versionType.getPropertyDefinitionTemplates().add(createMultiPropertyDefTemplate(session, mgr, "scape:hasRepresentation"));

            final NodeTypeTemplate repType = mgr.createNodeTypeTemplate();
            repType.setName("scape:representation");
            repType.setDeclaredSuperTypeNames(new String[] {"fedora:resource", "fedora:object"});
            repType.setMixin(true);
            repType.setQueryable(true);
            repType.setAbstract(false);
            repType.getPropertyDefinitionTemplates().add(createMultiPropertyDefTemplate(session, mgr, "scape:hasFile"));

            final NodeTypeTemplate fileType = mgr.createNodeTypeTemplate();
            fileType.setName("scape:file");
            fileType.setDeclaredSuperTypeNames(new String[] {"fedora:resource",
                    "fedora:object"});
            fileType.setMixin(true);
            fileType.setQueryable(true);
            fileType.setAbstract(false);
            fileType.getPropertyDefinitionTemplates().add(createMultiPropertyDefTemplate(session, mgr, "scape:hasBitstream"));


            final NodeTypeTemplate queueType = mgr.createNodeTypeTemplate();
            queueType.setName("scape:async-queue");
            queueType.setDeclaredSuperTypeNames(new String[] {"fedora:resource",
                    "fedora:object"});
            queueType.setMixin(true);
            queueType.setQueryable(true);
            queueType.setAbstract(false);
            queueType.getPropertyDefinitionTemplates().add(createMultiPropertyDefTemplate(session, mgr, "scape:hasItem"));

            // and register them
            mgr.registerNodeTypes(new NodeTypeDefinition[] {fileType, versionType,
                    entityType, repType, queueType}, true);
            /* make sure that the entities folder exists for ingestion */
            if (!this.objectService.exists(session, "/" + ConnectorService.ENTITY_FOLDER)) {
                this.objectService.createObject(session, "/" + ConnectorService.ENTITY_FOLDER);
            }
            
            /* make sure that the queue object exists for async ingests */
            this.objectService.createObject(session,
                    ConnectorService.QUEUE_NODE).getNode().addMixin("scape:async-queue");
            session.save();
        } catch (RepositoryException e) {
            LOG.error("Error while setting up scape connector api", e);
            throw new RuntimeException("Unable to setup scape on fedora");
        }

    }

    private PropertyDefinitionTemplate createMultiPropertyDefTemplate(final Session session, final NodeTypeManager mgr, final String name) throws UnsupportedRepositoryOperationException, RepositoryException {
        PropertyDefinitionTemplate propDefn = mgr.createPropertyDefinitionTemplate();
        propDefn.setName(name);
        propDefn.setRequiredType(PropertyType.STRING);
        ValueFactory valueFactory = session.getValueFactory();
        propDefn.setMultiple(true);
        propDefn.setFullTextSearchable(false);
        propDefn.setQueryOrderable(false);
        return propDefn;
    }
}
