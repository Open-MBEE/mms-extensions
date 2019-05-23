package gov.nasa.jpl.mbee.mms.services;


import org.openmbee.mms.client.ApiClient;
import org.openmbee.mms.client.ApiException;
import org.openmbee.mms.client.api.ElementApi;
import org.openmbee.mms.client.model.Element;
import org.openmbee.mms.client.model.Elements;
import org.openmbee.mms.client.model.RejectableElements;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by dlam on 7/26/18.
 */
public class Utils {

    public static Map<String, String> getBasicAuth(String authorization) {
        Map<String, String> result = null;
        try {
            //final String authorization = httpRequest.getHeader("Authorization");
            if (authorization != null && authorization.toLowerCase().startsWith("basic")) {
                // Authorization: Basic base64credentials
                String base64Credentials = authorization.substring("Basic".length()).trim();
                byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                // credentials = username:password
                final String[] values = credentials.split(":", 2);
                result = new HashMap<>();
                result.put("username", values[0]);
                result.put("password", values[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static List<Element> getElements(ElementApi apiInstance, String projectId, String refId, Set<String> ids, String commitId) throws ApiException {
        Elements body = new Elements();
        for (String id: ids) {
            Element a = new Element();
            a.put("id", id);
            body.addElementsItem(a);
        }
        try {
            RejectableElements e =  apiInstance.getElementsInBatch(projectId, refId, body, null, null, commitId);
            return e.getElements();
        } catch (ApiException e) {
            if (e.getCode() != 404) {
                throw e;
            }
            return new ArrayList<Element>();
        }
    }

    public static void removeKeys(Collection<Element> elements, Set<String> keys) {
        for (Element e: elements) {
            for (String s: keys) {
                e.remove(s);
            }
        }
    }

    public static void setKeys(Collection<Element> elements, Map<String, Object> add) {
        for (Element e: elements) {
            e.putAll(add);
        }
    }

    public static ApiClient createClient(String ticket, Optional<String> auth, String server) {
        ApiClient client = new ApiClient();
        client.setConnectTimeout(300000);
        client.setReadTimeout(600000);
        client.setWriteTimeout(600000);
        Map<String, String> basic = getBasicAuth(auth.orElse(null));
        if (ticket != null) {
            client.setApiKey(ticket);
        } else if (basic != null) {
            client.setPassword(basic.get("password"));
            client.setUsername(basic.get("username"));
        }
        client.setBasePath(server + "/alfresco/service");
        return client;
    }
}
