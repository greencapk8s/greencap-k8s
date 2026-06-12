package io.greencap.k8s.kubernetes.dto;

public record StatefulSetInfo(
        String name,
        String namespace,
        int desired,
        int ready,
        int available,
        String serviceName,
        String age
) {}
