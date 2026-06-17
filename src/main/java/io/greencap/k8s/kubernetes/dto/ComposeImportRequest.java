package io.greencap.k8s.kubernetes.dto;

import java.util.List;

public record ComposeImportRequest(
        String namespace,
        List<ServiceConfig> serviceConfigs
) {
    public record ServiceConfig(
            String serviceName,
            String resolvedImage,
            List<VolumeConfig> volumes
    ) {}

    public record VolumeConfig(
            String volumeName,
            String mountPath,
            String storageClass,
            int storageGi
    ) {}
}
