package io.greencap.k8s.kubernetes.dto;

import java.util.List;

public record ImportComposeResult(List<ServiceResult> serviceResults) {

    public record ServiceResult(
            String serviceName,
            List<String> createdResources,
            String failureMessage
    ) {
        public boolean isSuccess() { return failureMessage == null; }
    }

    public boolean isFullSuccess() {
        return serviceResults.stream().allMatch(ServiceResult::isSuccess);
    }
}
