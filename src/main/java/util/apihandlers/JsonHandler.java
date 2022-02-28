package util.apihandlers;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class JsonHandler {
    public static Optional<List<String>> getAllApplicationIdsFromBaseJsonExcept(JSONObject apiCallResult,
                                                                                List<String> applicationsNotToModify) {
        return Optional.of(apiCallResult)
                .flatMap(JsonHandler::getEmbeddedNode)
                .flatMap(JsonHandler::getApplicationsNode)
                .map(allApplicationsNode -> getAllApplicationIds(allApplicationsNode, applicationsNotToModify));
    }

    private static Optional<JSONObject> getEmbeddedNode(JSONObject baseNode) {
        return tryGetElementFromJsonObject(baseNode, "_embedded")
                .filter(result -> result instanceof JSONObject)
                .map(JsonHandler::mapToJsonObject);
    }

    private static Optional<JSONArray> getApplicationsNode(JSONObject embeddedNode) {
        return tryGetElementFromJsonObject(embeddedNode, "applications")
                .filter(result -> result instanceof JSONArray)
                .map(JsonHandler::mapToJsonArray);
    }

    private static List<String> getAllApplicationIds(JSONArray allApplications,
                                                     List<String> applicationsNotToModify) {
        List<String> foundIds = new ArrayList<>();
        for (int currentIndex = 0; currentIndex < allApplications.length(); currentIndex++) {
            tryGetElementAtJsonArrayIndex(allApplications, currentIndex)
                    .filter(applicationNode -> isNotException(applicationNode, applicationsNotToModify))
                    .flatMap(JsonHandler::getGuidNodeValue)
                    .ifPresent(foundIds::add);
        }
        return foundIds;
    }

    private static boolean isNotException(JSONObject applicationNode, List<String> applicationsNotToModify) {
        return !Optional.of(applicationNode)
                .flatMap(JsonHandler::getProfileNode)
                .flatMap(JsonHandler::getApplicationName)
                .map(foundApplication -> isIgnoredApplication(foundApplication, applicationsNotToModify))
                .orElse(false);
    }

    private static boolean isIgnoredApplication(String foundApplication, List<String> applicationsNotToModify) {
        boolean shouldIgnore = applicationsNotToModify.contains(foundApplication.toLowerCase(Locale.ROOT));
        Logger.log("Found Application Profile: " + foundApplication + "\n" +
                (shouldIgnore ? "It is ignored and will not be modified" : "It will be modified"));
        return shouldIgnore;
    }


    private static Optional<String> getApplicationName(JSONObject profileNode) {
        return tryGetElementAsString(profileNode, "name");
    }

    private static Optional<JSONObject> getProfileNode(JSONObject applicationNode) {
        return tryGetElementFromJsonObject(applicationNode, "profile")
                .filter(result -> result instanceof JSONObject)
                .map(JsonHandler::mapToJsonObject);
    }

    public static Optional<String> getGuidNodeValue(JSONObject applicationNode) {
        return tryGetElementAsString(applicationNode, "guid");
    }

    private static Optional<String> tryGetElementAsString(JSONObject jsonObject, String elementToGet) {
        return tryGetElementFromJsonObject(jsonObject, elementToGet)
                .filter(result -> result instanceof String)
                .map(result -> (String) result);
    }

    private static Optional<JSONObject> tryGetElementAtJsonArrayIndex(JSONArray allApplications, int currentIndex) {
        try {
            Object element = allApplications.get(currentIndex);
            if (element instanceof JSONObject) {
                return Optional.of((JSONObject) element);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private static Optional<Object> tryGetElementFromJsonObject(JSONObject jsonObject, String elementToGet) {
        try {
            return Optional.of(jsonObject.get(elementToGet));
        } catch (JSONException e) {
            return Optional.empty();
        }
    }

    private static JSONObject mapToJsonObject(Object jsonResult) {
        return (JSONObject) jsonResult;
    }

    private static JSONArray mapToJsonArray(Object jsonResult) {
        return (JSONArray) jsonResult;
    }
}
