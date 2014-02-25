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
package eu.scape_project.rdf;

/**
 * This Vocabulary is used to match the scape-platform-datamodel to Fedora's JCR
 * Properties
 * 
 * @author frank asseg
 * 
 */
public interface ScapeRDFVocabulary {
    public static final String SCAPE_NAMESPACE = "http://scapeproject.eu/model#";

    public static final String HAS_REPRESENTATION = SCAPE_NAMESPACE + "hasRepresentation";

    public static final String HAS_TYPE = SCAPE_NAMESPACE + "hasType";

    public static final String HAS_SCHEMA = SCAPE_NAMESPACE + "hasSchema";

    public static final String HAS_BITSTREAM_TYPE = SCAPE_NAMESPACE + "hasBitstreamType";

    public static final String HAS_BITSTREAM = SCAPE_NAMESPACE + "hasBitStream";

    public static final String HAS_FILENAME = SCAPE_NAMESPACE + "hasFileName";

    public static final String HAS_MIMETYPE = SCAPE_NAMESPACE + "hasMimeType";

    public static final String HAS_INGEST_SOURCE = SCAPE_NAMESPACE + "hasIngestSource";

    public static final String HAS_TITLE = SCAPE_NAMESPACE + "hasTitle";

    public static final String HAS_LIFECYCLESTATE = SCAPE_NAMESPACE + "hasLifeCycleState";

    public static final String HAS_LIFECYCLESTATE_DETAILS = SCAPE_NAMESPACE + "hasLifeCycleStateDetails";

    public static final String HAS_CURRENT_VERSION = SCAPE_NAMESPACE + "currentVersion";

    public static final String HAS_VERSION = SCAPE_NAMESPACE + "hasVersion";

    public static final String HAS_FILE = SCAPE_NAMESPACE + "hasFile";

    public static final String HAS_ITEM = SCAPE_NAMESPACE + "hasItem";

    public static final String HAS_REFERENCED_CONTENT = SCAPE_NAMESPACE + "hasReferencedContent";

    public static final String HAS_INGEST_STATE = "hasIngestState";
}
