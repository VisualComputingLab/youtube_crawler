
package gr.iti.vcl.youtubecrawl.rest;

import gr.iti.vcl.youtubecrawl.impl.YouTubeCrawl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import org.codehaus.jettison.json.JSONObject;

/**
 * REST Web Service
 *
 * @author dimitris.samaras@iti.gr
 */
@Path("crawl")
public class YouTubeCrawl_Rest {

    @Context
    private UriInfo context;
    private static YouTubeCrawl impl = new YouTubeCrawl();

    /**
     * Creates a new instance of CrawlerRest
     */
    public YouTubeCrawl_Rest() {
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public JSONObject postJson(JSONObject content) throws Exception {
        return impl.parseOut(content);

        //return null;
    }
}
