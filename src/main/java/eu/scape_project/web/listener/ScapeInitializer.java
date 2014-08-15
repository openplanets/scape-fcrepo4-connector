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
import javax.ws.rs.ext.Provider;

import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.services.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.update.UpdateAction;
import com.sun.jersey.api.model.AbstractResourceModelContext;
import com.sun.jersey.api.model.AbstractResourceModelListener;
import eu.scape_project.rdf.ScapeRDFVocabulary;

/**
 * A JAX-RS Provider which initializes the web application by adding the required namespace and node types to Fedora
 * 
 * @author frank asseg
 */
@Component
@Provider
public class ScapeInitializer implements AbstractResourceModelListener {

    private static final Logger LOG = LoggerFactory.getLogger(ScapeInitializer.class);

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private SessionFactory sessionFactory;

    /*
     * (non-Javadoc)
     * @see com.sun.jersey.api.model.AbstractResourceModelListener#onLoaded(com.sun
     * .jersey.api.model.AbstractResourceModelContext)
     */
    @Override
    public void onLoaded(AbstractResourceModelContext modelContext) {
        try {
            final Session session = this.sessionFactory.getInternalSession();
            /* make sure that the scape namespace is available in fcrepo */
            final Dataset namespace = this.repositoryService
                    .getNamespaceRegistryDataset(session, new DefaultIdentifierTranslator());

            UpdateAction.parseExecute("INSERT {<" + ScapeRDFVocabulary.SCAPE_NAMESPACE + "> <" +
                    RdfLexicon.HAS_NAMESPACE_PREFIX + "> 'scape'} WHERE {}",
                    namespace);
        } catch (RepositoryException e) {
            LOG.error("Error while setting up scape connector api", e);
            throw new RuntimeException("Unable to setup scape on fedora");
        }

    }
}
