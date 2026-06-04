package io.greencap.k8s.kubernetes.dto;

public record PersistentVolumeClaimInfo(
        String name,
        String namespace,
        String status,
        String capacity,
        String accessMode,
        String storageClass,
        String age
) {}
