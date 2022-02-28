package util.executionparameters;

import util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class ExecutionParameters {
    private final ApiCredentials apiCredentials;
    private final List<String> sandboxNames;

    protected ExecutionParameters(ApiCredentials apiCredentials, List<String> sandboxNames) {
        if (sandboxNames == null || sandboxNames.isEmpty()) {
            throw new IllegalArgumentException("Sandbox Names argument is mandatory (--sandbox_names, -sn)");
        }
        this.apiCredentials = apiCredentials;
        this.sandboxNames = new ArrayList<>(sandboxNames);
    }

    public static Optional<ExecutionParameters> of(String[] commandLineArguments) {
        Logger.log("Parsing Execution Parameters");
        ParameterParser parameterParser = new ParameterParser(commandLineArguments);
        String actionName = parameterParser.getParameterAsString("--action", "-a");
        if (actionName == null) {
            throw new IllegalArgumentException("Action argument is mandatory (--action, -a)");
        }
        if (actionName.equalsIgnoreCase("createForNewApplication")) {
            return Optional.of(parseParametersForCreateForNewApplication(parameterParser));
        } else if (actionName.equalsIgnoreCase("createForAllApplications")) {
            return Optional.of(parseParametersForCreateForAllApplications(parameterParser));
        }
        throw new IllegalArgumentException("Invalid Action Provided: " + actionName+ "\n" +
                "\t Supported Actions:\n" +
                "\t\t createForNewApplication\n" +
                "\t\t createForAllApplications");
    }

    private static CreateForAllApplicationsExecutionParameters parseParametersForCreateForAllApplications(
            ParameterParser parameterParser) {
        return new CreateForAllApplicationsExecutionParameters(
                new ApiCredentials(
                        parameterParser.getParameterAsString("--veracode_id", "-vi"),
                        parameterParser.getParameterAsString("--veracode_key", "-vk")),
                parameterParser.getParameterAsList("--sandbox_names", "-sn"),
                parameterParser.getParameterAsList("--exceptions", "-e"));
    }

    private static CreateForNewApplicationExecutionParameters parseParametersForCreateForNewApplication(
            ParameterParser parameterParser) {
        return new CreateForNewApplicationExecutionParameters(
                new ApiCredentials(
                        parameterParser.getParameterAsString("--veracode_id", "-vi"),
                        parameterParser.getParameterAsString("--veracode_key", "-vk")),
                parameterParser.getParameterAsList("--sandbox_names", "-sn"),
                parameterParser.getParameterAsString("--application_name", "-an"),
                parameterParser.getParameterAsString("--business_criticality", "-bc"),
                parameterParser.getParameterAsString("--description", "-d"),
                parameterParser.getParameterAsString("--business_unit", "-bu"),
                parameterParser.getParameterAsString("--business_owner", "-bo"),
                parameterParser.getParameterAsString("--business_owner_email", "-boe"),
                parameterParser.getParameterAsString("--teams", "-t"));
    }

    public ApiCredentials getApiCredentials() {
        return apiCredentials;
    }

    public List<String> getSandboxNames() {
        return new ArrayList<>(this.sandboxNames);
    }
}
