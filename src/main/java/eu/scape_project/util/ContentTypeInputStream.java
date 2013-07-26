/**
 *
 */

package eu.scape_project.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author frank asseg
 *
 */
public class ContentTypeInputStream extends InputStream {

    private final InputStream src;

    private final String contentType;

    public ContentTypeInputStream(String contentType, InputStream src) {
        this.src = src;
        this.contentType = contentType;

    }

    @Override
    public int read() throws IOException {
        return src.read();
    }

    public String getContentType() {
        return contentType;
    }

}
