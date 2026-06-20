package io.greencap.k8s.kubernetes.dto;

public record HelmReleaseInfo(
        String name,
        String namespace,
        String chart,
        String appVersion,
        String revision,
        String status,
        String updated
) {}
