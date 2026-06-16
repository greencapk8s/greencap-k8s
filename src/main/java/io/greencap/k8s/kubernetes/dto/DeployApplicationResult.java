package io.greencap.k8s.kubernetes.dto;

import java.util.List;

public record DeployApplicationResult(
        List<String> createdResources,
        String failedStep,
        String failureMessage
) {
    public boolean isFullSuccess() {
        return failedStep == null;
    }
}
