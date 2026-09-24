package io.greencap.k8s.kubernetes.dto;

import java.util.List;

public record ComposeImportRequest(
        String namespace,
        List<ServiceConfig> serviceConfigs
) {
    public record ServiceConfig(
            String serviceName,
            String resolvedImage,
            List<VolumeConfig> volumes,
            IngressConfig ingress
    ) {
        public boolean isExposed() { return ingress != null; }
    }

    public record VolumeConfig(
            String volumeName,
            String mountPath,
            String storageClass,
            int storageGi
    ) {}

    public record IngressConfig(String host, String ingressClassName) {}
}
