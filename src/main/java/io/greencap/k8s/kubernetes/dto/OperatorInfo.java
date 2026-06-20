package io.greencap.k8s.kubernetes.dto;

public record OperatorInfo(
        String name,
        String displayName,
        String version,
        String channel,
        String catalogSource,
        String phase,
        String statusMessage
) {}
