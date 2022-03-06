package gov.nasa.jpl.mbee.mms.services;

import gov.nasa.jpl.mbee.mms.domains.DocMergeRequest;

import java.util.*;

import io.micronaut.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openmbee.mms.client.ApiClient;
import org.openmbee.mms.client.ApiException;
import org.openmbee.mms.client.api.ElementApi;
import org.openmbee.mms.client.api.ProjectApi;
import org.openmbee.mms.client.api.RefApi;
import org.openmbee.mms.client.model.Commit;
import org.openmbee.mms.client.model.Element;
import org.openmbee.mms.client.model.Elements;

public class DocMerge {

    private static Logger logger = LogManager.getLogger(DocMerge.class);

    private Map<String, Element> fromViews = new HashMap<>();
    private Map<String, Element> toViews = new HashMap<>();
    private Map<String, Element> viewsAtCommit = new HashMap<>();
    private Map<String, Element> fromPes = new HashMap<>();
    private Map<String, Element> pesAtCommit = new HashMap();
    private Map<String, Element> toPes = new HashMap<>();
    private Map<String, Element> fromSlots = new HashMap<>();

    private ApiClient client;
    private String projectId;
    private String toRefId;
    private String fromRefId;
    private String docId;
    private ElementApi apiInstance;
    private String commonTime = "";
    private String commonCommit = null;

    private Map<String, Element> postElements = new HashMap<>();
    private Map<String, Object> response = new HashMap<>();

    public DocMerge() {
        //Do nothing on initialization
    }

    public HttpResponse<?> mergeDoc(DocMergeRequest request, Optional<String> auth) {
        Elements postElements = new Elements();
        try {
            client = Utils.createClient(request.getTicket(), auth, request.getMmsServer());

            apiInstance = new ElementApi();
            apiInstance.setApiClient(client);
            projectId = request.getProjectId();
            fromRefId = request.getFromRefId();
            toRefId = request.getToRefId();
            docId = request.getDocId();
            String comment = request.getComment();

            getCommonTime();

            DocInfo fromDoc = new DocInfo(client, docId, projectId, fromRefId);
            fromDoc.processDoc();
            fromViews = fromDoc.getViews();
            fromPes = fromDoc.getPes();
            fromSlots = fromDoc.getSlots();
            getFromViewsAndPesAtCommit();
            getToViewsAndPes();
            DocInfo toDoc = new DocInfo(client, docId, projectId, toRefId);
            toDoc.setViews(toViews);
            toDoc.setPes(toPes);
            toDoc.processPes();
            toPes = toDoc.getPes();

            for (Element view: fromViews.values()) {
                handleViewOrPe(view, viewsAtCommit.get(view.get("id")), toViews.get(view.get("id")));
            }

            postElements.comment((comment == null || comment.isEmpty()) ? ("doc presentation elements merge for " + docId + " from " + fromRefId + " to " + toRefId) : comment);
            postElements.elements(new ArrayList<Element>(this.postElements.values()));
            apiInstance.postElements(projectId, toRefId, postElements);
        } catch (Exception e) {
            logger.error("Failed: ", e);
            return HttpResponse.badRequest();
        }
        logger.info("finished");
        response.put("status", "ok");

       // Gson gson = new Gson();
       // String posted = gson.toJson(postElements);
        //response.put("posted", posted);
        logger.info("posted: " + postElements);

        return HttpResponse.ok(response);
    }

    //get time of common ancestor
    private void getCommonTime() throws ApiException {
        RefApi refApi = new RefApi();
        refApi.setApiClient(client);
        ProjectApi projectApi = new ProjectApi();
        projectApi.setApiClient(client);
        List<Commit> fromCommits = refApi.getRefHistory(projectId, fromRefId).getCommits();
        List<Commit> toCommits = refApi.getRefHistory(projectId, toRefId).getCommits();
        int length = fromCommits.size();
        int fromLength = length;
        int toLength = toCommits.size();
        if (toLength < length) {
            length = toLength;
        }
        Commit lastCommit = null;
        //earliest commit of any branch must start from initial commit of project, find commit before first divergence
        for (int i = 1; i <= length; i++) {
            Commit fromCommit = fromCommits.get(fromLength - i);
            Commit toCommit = toCommits.get(toLength - i);
            if (!fromCommit.get("id").equals(toCommit.get("id"))) {
                break;
            }
            lastCommit = toCommit;
        }
        if (lastCommit != null) {
            commonCommit = (String)lastCommit.get("id");
            commonTime = (String)lastCommit.get("_created");
        }
    }

    private void handleViewOrPe(Element fromElement, Element elementAtCommit, Element toElement) {
        if (fromElement == null || postElements.containsKey(fromElement.get("id"))) {
            return;
        }
        fromElement.remove("_childViews");
        if (toElement == null) {
            postElements.put((String)fromElement.get("id"), fromElement);
            Map<String, Object> contents = (Map<String, Object>)fromElement.get("_contents");
            if (contents == null) {
                contents = (Map<String, Object>)fromElement.get("specification");
            }
            if (contents == null || !(contents.get("operand") instanceof List)) {
                return;
            }
            for (Map<String, Object> operand: (List<Map<String, Object>>)contents.get("operand")) {
                if (operand.get("instanceId") != null) {
                    Element nextFrom = fromPes.get(operand.get("instanceId"));
                    Element nextTo = toPes.get(operand.get("instanceId"));
                    Element nextAtCommit = pesAtCommit.get(operand.get("instanceId"));
                    if (nextFrom != null) {
                        handleViewOrPe(nextFrom, nextAtCommit, nextTo);
                    }
                }
            }
            return;
        }

        Map<String, Object> fromContents = (Map<String, Object>)fromElement.get("_contents");
        Map<String, Object> contentsAtCommit = elementAtCommit == null ? null : (Map<String, Object>)elementAtCommit.get("_contents");
        Map<String, Object> toContents = (Map<String, Object>)toElement.get("_contents");
        if (fromContents == null) {
            fromContents = (Map<String, Object>)fromElement.get("specification");
            contentsAtCommit = elementAtCommit == null ? null : (Map<String, Object>)elementAtCommit.get("specification");
            toContents = (Map<String, Object>)toElement.get("specification");
        }
        if (fromContents == null || !(fromContents.get("operand") instanceof List)) {
            //a regular pe and not a section
            if ("InstanceSpecification".equals(fromElement.get("type"))) {
                String fromModified = (String)fromElement.get("_modified");
                String toModified = (String)toElement.get("_modified");
                if (fromModified.compareTo(toModified) > 0 || fromModified.compareTo(commonTime) > 0) {
                    //only push to target ref if branch pe has been modified
                    String fromId = (String)fromElement.get("id");
                    postElements.put(fromId, fromElement);
                    Element slot1 = fromSlots.get(fromId + "-slot-17_0_5_1_407019f_1430628276506_565_1208");
                    Element slot2 = fromSlots.get(fromId + "-slot-_17_0_5_1_407019f_1430628376067_525763_12104");
                    if (slot1 != null) {
                        postElements.put((String)slot1.get("id"), slot1);
                    }
                    if (slot2 != null) {
                        postElements.put((String)slot2.get("id"), slot2);
                    }
                }
            }
            return;
        }

        List<Map<String, Object>> fromOperands = (List<Map<String, Object>>)fromContents.get("operand");
        List<Map<String, Object>> toOperands;
        if (toContents == null || !(toContents.get("operand") instanceof List)) {
            toOperands = new ArrayList<>();
        } else {
            toOperands = (List<Map<String, Object>>)toContents.get("operand");
        }
        List<Map<String, Object>> operandsAtCommit = contentsAtCommit == null ? null : (List<Map<String, Object>>)contentsAtCommit.get("operand");
        boolean fromOperandsChanged = false;
        boolean toOperandsChanged = false;
        if (operandsAtCommit == null || fromOperands.size() != operandsAtCommit.size()) {
            fromOperandsChanged = true;
        } else {
            for (int i = 0; i < fromOperands.size(); i++) {
                Object now = fromOperands.get(i) == null ? null : fromOperands.get(i).get("instanceId");
                Object then = operandsAtCommit.get(i) == null ? null : operandsAtCommit.get(i).get("instanceId");
                if (now != null && now.equals(then) || then != null && then.equals(now) || then == now) {
                    continue;
                }
                fromOperandsChanged = true;
                break;
            }
        }
        if (operandsAtCommit == null || toOperands.size() != operandsAtCommit.size()) {
            toOperandsChanged = true;
        } else {
            for (int i = 0; i < toOperands.size(); i++) {
                Object now = toOperands.get(i) == null ? null : toOperands.get(i).get("instanceId");
                Object then = operandsAtCommit.get(i) == null ? null : operandsAtCommit.get(i).get("instanceId");
                if (now != null && now.equals(then) || then != null && then.equals(now) || then == now) {
                    continue;
                }
                toOperandsChanged = true;
                break;
            }
        }
        List<Map<String, Object>> newOperands = new ArrayList<>();
        Set<String> seenInstanceIds = new HashSet<>();
        for (Map<String, Object> operand: fromOperands) {
            newOperands.add(operand);
            Element fromPe = fromPes.get(operand.get("instanceId"));
            Element toPe = toPes.get(operand.get("instanceId"));
            Element peAtCommit = pesAtCommit.get(operand.get("instanceId"));
            handleViewOrPe(fromPe, peAtCommit, toPe);
            seenInstanceIds.add((String)operand.get("instanceId"));
        }
        if (!fromOperandsChanged) {
            if (!fromElement.get("name").equals(elementAtCommit.get("name"))) {
                Element newOb = new Element();
                newOb.put("id", fromElement.get("id"));
                newOb.put("name", fromElement.get("name"));
                postElements.put((String)newOb.get("id"), newOb);
            }
            return; //don't post view or section if operands did not change in fromRef
        }
        if (toOperandsChanged) { //only union to operands if toRef operands has changed
            for (Map<String, Object> operand : toOperands) {
                if (seenInstanceIds.contains(operand.get("instanceId"))) {
                    continue;
                }
                newOperands.add(operand);
            }
        }
        Element newOb = new Element();
        Map<String, Object> newSpec = new HashMap<>();
        newSpec.put("operand", newOperands);
        newSpec.put("type", "Expression");
        newOb.put("id", fromElement.get("id"));
        if (fromElement.containsKey("_contents")) {
            newOb.put("_contents", newSpec);
        } else {
            newOb.put("specification", newSpec);
        }
        postElements.put((String)newOb.get("id"), newOb);
    }

    private void getFromViewsAndPesAtCommit() throws ApiException {
        for (Element view: Utils.getElements(apiInstance, projectId, fromRefId, fromViews.keySet(), commonCommit)) {
            viewsAtCommit.put((String)view.get("id"), view);
        }
        for (Element pe: Utils.getElements(apiInstance, projectId, fromRefId, fromPes.keySet(), commonCommit)) {
            pesAtCommit.put((String)pe.get("id"), pe);
        }
    }

    private void getToViewsAndPes() throws ApiException {
        for (Element view: Utils.getElements(apiInstance, projectId, toRefId, fromViews.keySet(), null)) {
            toViews.put((String)view.get("id"), view);
        }
        for (Element pe: Utils.getElements(apiInstance, projectId, toRefId, fromPes.keySet(), null)) {
            toPes.put((String)pe.get("id"), pe);
        }
    }
}
