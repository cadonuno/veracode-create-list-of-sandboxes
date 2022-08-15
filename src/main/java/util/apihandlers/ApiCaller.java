package util.apihandlers;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import util.HmacRequestSigner;
import util.Logger;
import util.executionparameters.ApiCredentials;
import util.executionparameters.CreateForAllApplicationsExecutionParameters;
import util.executionparameters.CreateForNewApplicationExecutionParameters;
import util.executionparameters.ExecutionParameters;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class ApiCaller {
    private static final String APIS_BASE_URL = "api.veracode.";
    private static final String APPLICATIONS_API_URL = "/appsec/v1/applications/";
    private static final String SANDBOX_API_URL_SUFFIX = "/sandboxes";
    private static final String GET_REQUEST = "GET";
    private static final String POST_REQUEST = "POST";
    private static String instanceUrl;

    public static void handleApiCalls(ExecutionParameters executionParameters) {
        instanceUrl = APIS_BASE_URL + executionParameters.getPlatformInstance().getTopLevelDomain();
        if (executionParameters instanceof CreateForAllApplicationsExecutionParameters) {
            createForAllApps((CreateForAllApplicationsExecutionParameters) executionParameters);
        } else if (executionParameters instanceof CreateForNewApplicationExecutionParameters) {
            createForNewApplication((CreateForNewApplicationExecutionParameters) executionParameters);
        }
    }

    private static void createForNewApplication(CreateForNewApplicationExecutionParameters executionParameters) {
        Logger.log("Creating Application Profile");
        runApi(APPLICATIONS_API_URL, POST_REQUEST,
                buildNewApplicationProfileJsonOutput(executionParameters), executionParameters.getApiCredentials())
                .flatMap(JsonHandler::getGuidNodeValue)
                .ifPresent(appGuid -> createSandboxes(executionParameters.getApiCredentials(), appGuid,
                        executionParameters.getSandboxNames()));
    }

    private static String buildNewApplicationProfileJsonOutput(
            CreateForNewApplicationExecutionParameters executionParameters) {
        String json = "{\n" +
                "\t\"profile\": {\n" +
                getParameterAsJsonString("name",
                        executionParameters.getApplicationName(), 2, true) +
                getParameterAsJsonString("business_criticality",
                        executionParameters.getBusinessCriticality().toUpperCase(Locale.ROOT), 2, true) +
                buildBusinessOwnerField(executionParameters) +
                buildBusinessUnityField(executionParameters) +
                buildDescriptionField(executionParameters) +
                buildTeamsField(executionParameters);
        return json.substring(0, json.length()-2) + "\n" +
                "\t}\n" +
                "}";
    }

    private static String buildDescriptionField(CreateForNewApplicationExecutionParameters executionParameters) {
        return isNullOrEmpty(executionParameters.getDescription())
                ? ""
                : getParameterAsJsonString("description", executionParameters.getDescription(), 1, true);
    }

    private static String getParameterAsJsonString(String parameterName, String parameterValue, int level, boolean addComma) {
        return "\t".repeat(level) +
                "\"" + parameterName + "\": \"" + parameterValue + "\"" +
                (addComma ? "," : "") + "\n";
    }

    private static String buildTeamsField(CreateForNewApplicationExecutionParameters executionParameters) {
        if (isNullOrEmpty(executionParameters.getTeams())) {
            return "";
        }
        String teamsString = Arrays.stream(Optional.ofNullable(executionParameters.getTeams())
                        .map(nonNullParameter -> nonNullParameter.split(","))
                        .orElse(new String[0]))
                .map(team -> "\t\t{\n" +
                        getParameterAsJsonString("guid", team, 3, true) +
                        "\t\t}\n")
                .collect(Collectors.joining());
        return "\t\t\"teams\": [\n" +
                teamsString.substring(0, teamsString.length()-2) + "\n"
                + "\t\t],";
    }

    private static String buildBusinessOwnerField(CreateForNewApplicationExecutionParameters executionParameters) {
        if (isNullOrEmpty(executionParameters.getBusinessOwner())
                && isNullOrEmpty(executionParameters.getBusinessOwnerEmail())) {
            return "";
        }
        String parameters = "\t\t\"business_owners\": [\n" +
                "\t\t{\n";
        if (!isNullOrEmpty(executionParameters.getBusinessOwnerEmail())) {
            parameters += getParameterAsJsonString(
                    "email", executionParameters.getBusinessOwnerEmail(), 3,
                    !isNullOrEmpty(executionParameters.getBusinessOwner()));
        }
        if (!isNullOrEmpty(executionParameters.getBusinessOwner())) {
            parameters += getParameterAsJsonString(
                    "name", executionParameters.getBusinessOwner(), 3, false);
        }
        return parameters + "\t\t}\n" +
                "\t\t],";
    }

    private static String buildBusinessUnityField(CreateForNewApplicationExecutionParameters executionParameters) {
        if (isNullOrEmpty(executionParameters.getBusinessUnit())) {
            return "";
        }
        return "\t\t\"business_unit\": [\n" +
                "\t\t{\n" +
                getParameterAsJsonString(
                "guid", executionParameters.getBusinessUnit(), 3, true) +
                "\t\t}\n" +
                "\t\t],";
    }

    private static boolean isNullOrEmpty(String parameter) {
        return parameter == null || parameter.isEmpty();
    }

    private static void createForAllApps(CreateForAllApplicationsExecutionParameters executionParameters) {
        Logger.log("Obtaining list of application profiles");
        List<String> applicationsToModify = getAllApplicationProfiles(executionParameters.getApplicationsNotToModify(),
                executionParameters.getApiCredentials());
        Logger.log("Starting to create Sandboxes for " + applicationsToModify.size() + " applications");
        for (String applicationProfileId : applicationsToModify) {
            createSandboxes(executionParameters.getApiCredentials(), applicationProfileId,
                    executionParameters.getSandboxNames());
        }
        Logger.log("Finished creating Sandboxes for " + applicationsToModify.size() + " applications");
    }

    private static void createSandboxes(ApiCredentials apiCredentials, String applicationProfileGuid, List<String> sandboxNames) {
        Logger.log("  Starting to create Sandboxes for application " + applicationProfileGuid);
        sandboxNames.forEach(sandboxName ->
                runApi(APPLICATIONS_API_URL + applicationProfileGuid + SANDBOX_API_URL_SUFFIX, POST_REQUEST,
                        buildNewSandboxJsonOutput(sandboxName), apiCredentials));
        Logger.log("  Finished creating Sandboxes for application " + applicationProfileGuid);
    }

    private static String buildNewSandboxJsonOutput(String sandboxName) {
        return "{\n" +
                "\t\"name\": \"" + sandboxName + "\"\n" +
                "}";
    }

    private static List<String> getAllApplicationProfiles(List<String> applicationsNotToModify,
                                                          ApiCredentials apiCredentials) {
        return runApi(APPLICATIONS_API_URL, GET_REQUEST, null, apiCredentials)
                .flatMap(response ->
                        JsonHandler.getAllApplicationIdsFromBaseJsonExcept(response, applicationsNotToModify))
                .orElse(Collections.emptyList());
    }

    private static Optional<JSONObject> runApi(String apiUrl, String requestType,
                                               String jsonParameters, ApiCredentials apiCredentials) {
        try {
            final URL applicationsApiUrl = new URL("https://" + instanceUrl + apiUrl);
            final String authorizationHeader =
                    HmacRequestSigner.getVeracodeAuthorizationHeader(apiCredentials, applicationsApiUrl, requestType);

            final HttpsURLConnection connection = (HttpsURLConnection) applicationsApiUrl.openConnection();
            connection.setRequestMethod(requestType);
            connection.setRequestProperty("Authorization", authorizationHeader);

            if (jsonParameters != null) {
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    byte[] input = jsonParameters.getBytes(StandardCharsets.UTF_8);
                    outputStream.write(input, 0, input.length);
                }
            }

            try (InputStream responseInputStream = connection.getInputStream()) {
                return Optional.of(readResponse(responseInputStream));
            }
        } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalStateException
                | IOException | JSONException e) {
            Logger.log("Unable to run API at: " + apiUrl + "\nWith parameters: " + jsonParameters);
        }
        return Optional.empty();
    }

    /*
     * A simple method to read an input stream (containing JSON) to System.out.
     */
    private static JSONObject readResponse(InputStream responseInputStream) throws IOException, JSONException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] responseBytes = new byte[16384];
        int x;
        while ((x = responseInputStream.read(responseBytes, 0, responseBytes.length)) != -1) {
            outputStream.write(responseBytes, 0, x);
        }
        outputStream.flush();
        return new JSONObject(outputStream.toString());
    }
}
