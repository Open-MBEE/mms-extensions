package gov.nasa.jpl.mbee.mms.controllers;

import gov.nasa.jpl.mbee.mms.domains.FnSyncRequest;
import gov.nasa.jpl.mbee.mms.services.Utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openmbee.mms.client.ApiClient;
import org.openmbee.mms.client.ApiException;
import org.openmbee.mms.client.api.ElementApi;
import org.openmbee.mms.client.model.Element;
import org.openmbee.mms.client.model.Elements;
import org.openmbee.mms.client.model.RejectableElements;

/**
 * sync a doc (pes) from one mms server to another, same project, can be different ref
 */
@Controller("/fn-sync-project")
public class FnSyncController {
    private static Logger logger = LogManager.getLogger(FnSyncController.class);


    @Post
    public HttpResponse<?> syncDoc(@Body FnSyncRequest request,
                                   @Header("Authorization") Optional<String> auth) {
        try {
            logger.info(request.toString());
            Elements postElements = new Elements();
            ApiClient fromClient = Utils.createClient(request.getFromTicket(), auth, request.getFromMmsServer());
            ApiClient toClient = Utils.createClient(request.getToTicket(), auth, request.getToMmsServer());

            String fromProjectId = request.getFromProjectId();
            String toProjectId = request.getToProjectId();
            String fromRefId = request.getFromRefId();
            String toRefId = request.getToRefId();
            String comment = request.getComment();

            ElementApi api = new ElementApi();
            api.setApiClient(fromClient);
            List<Element> els = getProjectElements(api, fromProjectId, fromRefId, null);

            logger.info("got project elements");

            Set<String> removeKeys = Stream.of("_childViews","_modified","_created","_modifier","_creator",
                    "_inRefIds","_projectId","_refId","_commitId","_editable","_elasticId")
                    .collect(Collectors.toSet());

            Utils.removeKeys(els, removeKeys);

            logger.info("removed keys");

            fn(els, fromProjectId);

            logger.info("removed -fn");

            if (!fromProjectId.equals(toProjectId)) {
                changeProject(els, fromProjectId, toProjectId);
                logger.info("changed project ids");
            }

            postElements.elements(els);
            postElements.comment(comment);

            ElementApi apiInstance = new ElementApi();
            apiInstance.setApiClient(toClient);
            RejectableElements result = apiInstance.postElements(toProjectId, toRefId, postElements);

            logger.info("finished");
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ok");
            //Gson gson = new Gson();
            //String posted = gson.toJson(postElements);
            //response.put("posted", posted);
            //logger.info("posted: " + posted);
            return HttpResponse.ok(response);
        } catch (Exception e) {
            logger.error("Failed: ", e);
            return HttpResponse.badRequest();
        }
    }

    private List<Element> getProjectElements(ElementApi apiInstance, String projectId, String refId, String commitId) throws ApiException {
        try {
            Elements e =  apiInstance.getElements(projectId, refId, null, commitId);
            return e.getElements();
        } catch (ApiException e) {
            if (e.getCode() != 404) {
                throw e;
            }
            return new ArrayList<Element>();
        }
    }

    private void fn(Collection<Element> elements, String projectId) {
        Element toRemove = null;
        for (Element e: elements) {
            try {
                if (projectId.equals(e.get("id"))) {
                    toRemove = e;
                    continue;
                }
                String doc = (String) e.get("documentation");
                if (doc != null) {
                    doc = doc.replace("opencae-fn", "opencae");
                    e.put("documentation", doc.replace("artifactory-fn", "artifactory"));
                }
                String name = (String) e.get("name");
                if (name != null) {
                    name = name.replace("opencae-fn", "opencae");
                    e.put("name", name.replace("artifactory-fn", "artifactory"));
                }
                fnVal((Map<String, Object>) e.get("defaultValue"));
                List<Map<String, Object>> values = (List<Map<String, Object>>) e.get("value");
                if (values != null) {
                    for (Map<String, Object> val : values) {
                        fnVal(val);
                    }
                }
                fnVal((Map<String, Object>) e.get("specification"));
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("element " + e.get("id"));
            }

        }
        if (toRemove != null) {
            elements.remove(toRemove);
        }
    }

    private void fnVal(Map<String, Object> valSpec) {
        if (valSpec != null && valSpec.get("type") != null) {
            if (valSpec.get("type").equals("LiteralString")) {
                String val = (String)valSpec.get("value");
                if (val != null) {
                    val = val.replace("opencae-fn", "opencae");
                    valSpec.put("value", val.replace("artifactory-fn", "artifactory"));
                }
            }
        }
    }

    private void changeProject(Collection<Element> elements, String fromProj, String toProj) {
        for (Element e: elements) {
            String id = (String)e.get("id");
            String ownerId = (String)e.get("ownerId");
            if (id != null && id.contains(fromProj)) {
                e.put("id", id.replace(fromProj, toProj));
            }
            if (ownerId != null && ownerId.contains(fromProj)) {
                e.put("ownerId", ownerId.replace(fromProj, toProj));
            }
        }
    }
}
