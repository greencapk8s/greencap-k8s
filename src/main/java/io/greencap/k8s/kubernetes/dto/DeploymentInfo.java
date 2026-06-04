package io.greencap.k8s.kubernetes.dto;

public record DeploymentInfo(
        String name,
        String namespace,
        int desired,
        int ready,
        int available,
        String age
) {}
