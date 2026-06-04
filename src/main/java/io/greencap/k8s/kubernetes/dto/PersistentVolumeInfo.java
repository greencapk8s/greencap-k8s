package io.greencap.k8s.kubernetes.dto;

public record PersistentVolumeInfo(
        String name,
        String status,
        String capacity,
        String accessMode,
        String reclaimPolicy,
        String storageClass,
        String claim,
        String age
) {}
