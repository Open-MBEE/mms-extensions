package gov.nasa.jpl.mbee.mms.services;

import org.openmbee.mms.client.ApiClient;
import org.openmbee.mms.client.ApiException;
import org.openmbee.mms.client.api.ElementApi;
import org.openmbee.mms.client.model.Element;
import org.openmbee.mms.client.model.Elements;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.*;

/**
 * Created by dlam on 7/26/18.
 * helper class for getting all views, pes, slots for a doc
 */
public class DocInfo {
    private static Logger logger = LogManager.getLogger(DocInfo.class);

    public Map<String, Element> getViews() {
        return views;
    }

    public void setViews(Map<String, Element> views) {
        this.views = views;
    }

    private Map<String, Element> views = new HashMap<>();

    public Map<String, Element> getPes() {
        return pes;
    }

    public void setPes(Map<String, Element> pes) {
        this.pes = pes;
    }

    private Map<String, Element> pes = new HashMap<>();

    public Map<String, Element> getSlots() {
        return slots;
    }

    public void setSlots(Map<String, Element> slots) {
        this.slots = slots;
    }
    private Map<String, Element> slots = new HashMap<>();

    private ApiClient client;
    private String projectId;
    private String refId;
    private String docId;
    private ElementApi apiInstance;

    public DocInfo(ApiClient client, String docId, String projectId, String refId) {
        this.client = client;
        this.docId = docId;
        this.refId = refId;
        this.projectId = projectId;
        apiInstance = new ElementApi();
        apiInstance.setApiClient(client);
    }

    public void processDoc() {
        try {
            processViews();
            processPes();
            processSlots();

        } catch (Exception e) {
            logger.error("Failed: ", e);
        }
    }

    private void processViews() throws ApiException {
        Element doc = null;
        Elements els = apiInstance.getElement(projectId, refId, docId, null, null, null);
        for (Element el: els.getElements()) {
            if (!projectId.equals(el.get("_projectId")) || !refId.equals(el.get("_refId"))) {
                continue;
            }
            doc = el;
            views.put((String)el.get("id"), el);
        }
        if (doc == null) {
            logger.error("doc doesn't exist on src ref");
            return;
        }
        getChildViews(doc);
    }

    private void getChildViews(Element doc) throws ApiException {
        List<Map<String, Object>> childViews = (List<Map<String, Object>>)doc.get("_childViews");
        if (childViews != null) {
            Set<String> childIds = new HashSet<>();
            for (Map<String, Object> childView : childViews) {
                if (views.containsKey(childView.get("id"))) {
                    continue;
                }
                childIds.add((String) childView.get("id"));
            }
            for (Element el : Utils.getElements(apiInstance, projectId, refId, childIds, null)) {
                views.put((String)el.get("id"), el);
                getChildViews(el);
            }
        }
    }

    public void processPes() throws ApiException {
        Set<String> ids = new HashSet<>();
        for (Element view: views.values()) {
            Map<String, Object> contents = (Map<String, Object>)view.get("_contents");
            if (contents == null) {
                continue;
            }
            if (!(contents.get("operand") instanceof List) || ((List)contents.get("operand")).isEmpty()) {
                continue;
            }
            for (Map<String, Object> operand: (List<Map<String, Object>>)contents.get("operand")) {
                if (operand.get("instanceId") != null && !pes.containsKey(operand.get("instanceId"))) {
                    ids.add((String)operand.get("instanceId"));
                }
            }
        }
        for (Element pe: Utils.getElements(apiInstance, projectId, refId, ids, null)) {
            pes.put((String)pe.get("id"), pe);
            getSectionPes(pe);
        }
    }

    private void getSectionPes(Element section) throws ApiException {
        Set<String> ids = new HashSet<>();
        Map<String, Object> contents = (Map<String, Object>)section.get("specification");
        if (contents == null) {
            return;
        }
        if (!(contents.get("operand") instanceof List) || ((List)contents.get("operand")).isEmpty()) {
            return;
        }
        for (Map<String, Object> operand: (List<Map<String, Object>>)contents.get("operand")) {
            if (operand.get("instanceId") != null && !pes.containsKey(operand.get("instanceId"))) {
                ids.add((String) operand.get("instanceId"));
            }
        }
        for (Element pe: Utils.getElements(apiInstance, projectId, refId, ids, null)) {
            pes.put((String)pe.get("id"), pe);
            getSectionPes(pe);
        }
    }

    private void processSlots() throws ApiException {
        Set<String> ids = new HashSet<>();
        for (String peid: pes.keySet()) {
            ids.add(peid + "-slot-_17_0_5_1_407019f_1430628276506_565_12080");
            ids.add(peid + "-slot-_17_0_5_1_407019f_1430628376067_525763_12104");
        }
        for (Element slot: Utils.getElements(apiInstance, projectId, refId, ids, null)) {
            slots.put((String)slot.get("id"), slot);
        }
    }
}
