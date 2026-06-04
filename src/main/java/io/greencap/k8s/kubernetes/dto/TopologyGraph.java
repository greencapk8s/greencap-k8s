package io.greencap.k8s.kubernetes.dto;

import java.util.List;

public record TopologyGraph(
        List<TopologyNode> nodes,
        List<TopologyEdge> edges
) {}
