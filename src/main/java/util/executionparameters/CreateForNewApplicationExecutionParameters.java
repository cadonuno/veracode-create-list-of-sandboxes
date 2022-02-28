package util.executionparameters;

import java.util.List;

public class CreateForNewApplicationExecutionParameters extends ExecutionParameters {
    private final String applicationName;
    private final String businessCriticality;
    private final String description;
    private final String businessUnity;
    private final String businessOwner;
    private final String businessOwnerEmail;
    private final String teams;

    protected CreateForNewApplicationExecutionParameters(ApiCredentials apiCredentials, List<String> sandboxNames,
                                                         String applicationName, String businessCriticality,
                                                         String description, String businessUnity,
                                                         String businessOwner, String businessOwnerEmail, String teams) {
        super(apiCredentials, sandboxNames);
        this.applicationName = applicationName;
        this.businessCriticality = businessCriticality;
        this.description = description;
        this.businessUnity = businessUnity;
        this.businessOwner = businessOwner;
        this.businessOwnerEmail = businessOwnerEmail;
        this.teams = teams;
        validateParameters();
    }

    private void validateParameters() {
        if (applicationName == null || applicationName.isEmpty()) {
            throw new IllegalArgumentException("Application Name argument is mandatory (--application_name, -an)");
        }
        if (businessCriticality == null || businessCriticality.isEmpty()) {
            throw new IllegalArgumentException("Business Criticality argument is mandatory (--business_criticality, -bc)");
        }
    }

    public String getTeams() {
        return teams;
    }

    public String getBusinessOwnerEmail() {
        return businessOwnerEmail;
    }

    public String getBusinessOwner() {
        return businessOwner;
    }

    public String getBusinessUnity() {
        return businessUnity;
    }

    public String getDescription() {
        return description;
    }

    public String getBusinessCriticality() {
        return businessCriticality;
    }

    public String getApplicationName() {
        return applicationName;
    }
}
