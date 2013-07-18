/**
 *
 */

package eu.scapeproject.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author frank asseg
 *
 */
@Component
@Scope("prototype")
@Path("/entity")
public class IntellectualEntities {

    @GET
    @Produces(MediaType.TEXT_XML)
    @Path("{id}")
    public Response retrieveEntity(@PathParam("id")
    final String id) {
        return Response.ok().build();
    }
}
