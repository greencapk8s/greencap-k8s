package io.greencap.k8s.kubernetes.dto;

import java.util.Map;

public record TopologyNode(
        String id,
        String label,
        String type,
        String status,
        String manifestUrl,
        Map<String, String> labels,
        int readyReplicas,
        int desiredReplicas,
        String serviceType,
        String capacity,
        String accessMode,
        String partOfGroup,
        String componentGroup
) {}
