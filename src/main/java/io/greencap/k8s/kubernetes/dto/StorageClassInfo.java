package io.greencap.k8s.kubernetes.dto;

public record StorageClassInfo(
        String name,
        String provisioner,
        String reclaimPolicy,
        String volumeBindingMode,
        String allowVolumeExpansion,
        String age
) {}
