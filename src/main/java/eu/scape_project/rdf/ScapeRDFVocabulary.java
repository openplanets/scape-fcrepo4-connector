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

    public static final String HAS_REPRESENTATION = "scape:hasRepresentation";

    public static final String HAS_TYPE = "scape:hasType";

    public static final String HAS_SCHEMA = "scape:hasSchema";

    public static final String HAS_BITSTREAM_TYPE = "scape:hasBitstreamType";

    public static final String HAS_BITSTREAM = "scape:hasBitStream";

    public static final String HAS_FILENAME = "scape:hasFileName";

    public static final String HAS_MIMETYPE = "scape:hasMimeType";

    public static final String HAS_INGEST_SOURCE = "scape:hasIngestSource";

    public static final String HAS_TITLE = "scape:hasTitle";

    public static final String HAS_LIFECYCLESTATE = "scape:hasLifeCycleState";

    public static final String HAS_LIFECYCLESTATE_DETAILS = "scape:hasLifeCycleStateDetails";

    public static final String HAS_CURRENT_VERSION = "scape:currentVersion";

    public static final String HAS_VERSION = "scape:hasVersion";

    public static final String HAS_FILE = "scape:hasFile";

    public static final String HAS_ITEM = "scape:hasItem";

    public static final String HAS_REFERENCED_CONTENT = "scape:hasReferencedContent";

    public static final String HAS_INGEST_STATE = "scape:hasIngestState";
}
