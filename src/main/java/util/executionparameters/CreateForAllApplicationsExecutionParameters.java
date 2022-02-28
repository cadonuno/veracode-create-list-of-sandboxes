package util.executionparameters;

import java.util.ArrayList;
import java.util.List;

public class CreateForAllApplicationsExecutionParameters extends ExecutionParameters {
    private final List<String> applicationsNotToModify;

    protected CreateForAllApplicationsExecutionParameters(ApiCredentials apiCredentials,
                                                          List<String> sandboxNames,
                                                          List<String> applicationsNotToModify) {
        super(apiCredentials, sandboxNames);
        this.applicationsNotToModify = new ArrayList<>(applicationsNotToModify);
    }

    public List<String> getApplicationsNotToModify() {
        return new ArrayList<>(applicationsNotToModify);
    }
}
