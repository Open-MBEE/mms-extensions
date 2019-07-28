package gov.nasa.jpl.mbee.mms.controllers;

import gov.nasa.jpl.mbee.mms.domains.DocSyncRequest;
import gov.nasa.jpl.mbee.mms.services.DocInfo;
import gov.nasa.jpl.mbee.mms.services.Utils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;import org.openmbee.mms.client.ApiClient;
import org.openmbee.mms.client.api.ElementApi;
import org.openmbee.mms.client.model.Element;
import org.openmbee.mms.client.model.Elements;
import org.openmbee.mms.client.model.RejectableElements;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by dlam on 7/26/18.
 * sync a doc (pes) from one mms server to another, same project, can be different ref, assumes view hierarchy
 * on destination is already established
 */
@Controller("/mms-sync-doc")
public class DocSyncController {
    private static Logger logger = LogManager.getLogger(DocSyncController.class);


    @Post
    public HttpResponse<?> syncDoc(@Body DocSyncRequest request,
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
            String docId = request.getDocId();
            String comment = request.getComment();

            DocInfo doc = new DocInfo(fromClient, docId, fromProjectId, fromRefId);
            doc.processDoc();
            Map<String, Element> views = doc.getViews();
            Map<String, Element> pes = doc.getPes();
            Map<String, Element> slots = doc.getSlots();

            Set<String> removeKeys = Stream.of("_childViews","_modified","_created","_modifier","_creator",
                    "_inRefIds","_projectId","_refId","_commitId","_editable","_elasticId","ownedAttributeIds")
                    .collect(Collectors.toSet());

            Utils.removeKeys(views.values(), removeKeys);
            Utils.removeKeys(pes.values(), removeKeys);
            Utils.removeKeys(slots.values(), removeKeys);
            Map<String, Object> ownerSet = new HashMap<>();
            if (request.getExtraKey() != null && request.getExtraValue() != null) {
                //extra key is for resurrecting deleted element on target
                //the key should be prefixed with underscore to indiciate it should be ignored by mdk
                //value can be anything (ex. key can be _dummy and value can be empty string
                ownerSet.put(request.getExtraKey(), request.getExtraValue());
                Utils.setKeys(views.values(), ownerSet);
                Utils.setKeys(slots.values(), ownerSet);
            }
            ownerSet.put("ownerId", "view_instances_bin_" + toProjectId);
            Utils.setKeys(pes.values(), ownerSet);
            
            List<Element> toPost = new ArrayList<>();
            toPost.addAll(views.values());
            toPost.addAll(pes.values());
            toPost.addAll(slots.values());
            postElements.elements(toPost);
            postElements.comment(comment);

            ElementApi apiInstance = new ElementApi();
            apiInstance.setApiClient(toClient);
            RejectableElements result = apiInstance.postElements(toProjectId, toRefId, postElements);

            //delete when move?
            //apiInstance.setApiClient(fromClient);
            //apiInstance.deleteElementsInBatch(fromProjectId, fromRefId, postElements);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "ok");

            //response.put("posted", posted);
            logger.info("posted: " + postElements);
            return HttpResponse.ok(response);
        } catch (Exception e) {
            logger.error("Failed: ", e);
            return HttpResponse.serverError(e);
        }
    }
}
