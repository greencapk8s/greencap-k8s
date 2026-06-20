package io.greencap.k8s.kubernetes.dto;

public record HelmReleaseDetails(
        String notes,
        String values,
        String manifest
) {}
