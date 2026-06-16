package io.greencap.k8s.kubernetes.dto;

public record DeployApplicationRequest(
        String namespace,
        String image,
        int replicas,
        String cpuRequest,
        String cpuLimit,
        String memoryRequest,
        String memoryLimit,
        Integer containerPort,
        PvcConfig volume,
        IngressConfig ingress
) {
    public record PvcConfig(String storageClass, int storageGi, String mountPath) {}
    public record IngressConfig(String host, String ingressClassName) {}
}
