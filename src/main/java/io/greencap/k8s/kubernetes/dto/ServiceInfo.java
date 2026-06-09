package io.greencap.k8s.kubernetes.dto;

public record ServiceInfo(
        String name,
        String namespace,
        String type,
        String clusterIP,
        String ports,
        String age,
        boolean hasReadyEndpoints
) {}
