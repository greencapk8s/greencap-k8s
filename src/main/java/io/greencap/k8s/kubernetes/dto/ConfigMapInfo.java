package io.greencap.k8s.kubernetes.dto;

public record ConfigMapInfo(
        String name,
        String namespace,
        int keyCount,
        String age
) {}
