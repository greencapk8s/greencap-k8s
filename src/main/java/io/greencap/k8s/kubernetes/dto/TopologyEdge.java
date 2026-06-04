package io.greencap.k8s.kubernetes.dto;

public record TopologyEdge(
        String sourceId,
        String targetId
) {}
