package io.greencap.k8s.kubernetes.dto;

public record NamespaceInfo(
        String name,
        String phase,
        String age,
        int podCount,
        int deploymentCount,
        int serviceCount
) {}
