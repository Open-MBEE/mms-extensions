package gov.nasa.jpl.mbee.mms.controllers;

import gov.nasa.jpl.mbee.mms.services.DocInfo;
import gov.nasa.jpl.mbee.mms.services.Utils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.QueryValue;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openmbee.mms.client.ApiClient;
import org.openmbee.mms.client.model.Element;
import org.openmbee.mms.client.model.Elements;

//ex. /mms-doc-info/mms.openmbee.org/PROJECT-asdf/master/abcde
@Controller("/mms-doc-info/{host}/{projectId}/{refId}/{docId}")
public class DocInfoController {

    private static Logger logger = LogManager.getLogger(DocInfoController.class);

    @Get
    public HttpResponse<?> docInfo(String host, String projectId, String refId, String docId,
            @Header("Authorization") Optional<String> auth, @QueryValue("alf_ticket") Optional<String> ticket) {
        try {
            logger.info(host + " " + projectId + " " + refId + " " + docId);
            Elements postElements = new Elements();
            ApiClient fromClient = Utils.createClient(ticket.orElse(null), auth, "https://" + host);

            DocInfo doc = new DocInfo(fromClient, docId, projectId, refId);
            doc.processDoc();
            Map<String, Element> views = doc.getViews();
            Map<String, Element> pes = doc.getPes();
            Map<String, Element> slots = doc.getSlots();

            /*Set<String> removeKeys = Stream
                    .of("_childViews","_modified","ownedAttributeIds")
                    .collect(Collectors.toSet());

            Utils.removeKeys(views.values(), removeKeys);
            Utils.removeKeys(pes.values(), removeKeys);
            Utils.removeKeys(slots.values(), removeKeys);*/

            Map<String, Object> response = new HashMap<>();
            response.put("views", views.values());
            response.put("instances", pes.values());
            //response.put("slots", slots.values());
            return HttpResponse.ok(response);
        } catch (Exception e) {
            logger.error("Failed: ", e);
            return HttpResponse.serverError(e);
        }
    }
}
