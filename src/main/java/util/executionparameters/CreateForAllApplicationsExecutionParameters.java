package util.executionparameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CreateForAllApplicationsExecutionParameters extends ExecutionParameters {
    private final List<String> applicationsNotToModify;

    protected CreateForAllApplicationsExecutionParameters(ApiCredentials apiCredentials,
                                                          List<String> sandboxNames,
                                                          List<String> applicationsNotToModify) {
        super(apiCredentials, sandboxNames);
        if (applicationsNotToModify != null) {
            this.applicationsNotToModify = applicationsNotToModify
                    .stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
        } else {
            this.applicationsNotToModify = Collections.emptyList();
        }
    }

    public List<String> getApplicationsNotToModify() {
        return new ArrayList<>(applicationsNotToModify);
    }
}
