package io.greencap.k8s.kubernetes.dto;

public record TopologyEdge(
        String sourceId,
        String targetId,
        TopologyEdgeType type,
        String matchedEnvVar,
        String matchedValue
) {

    public static TopologyEdge structural(String sourceId, String targetId) {
        return new TopologyEdge(sourceId, targetId, TopologyEdgeType.STRUCTURAL, "", "");
    }

    public static TopologyEdge serviceDependency(String sourceId, String targetId, String matchedEnvVar, String matchedValue) {
        return new TopologyEdge(sourceId, targetId, TopologyEdgeType.SERVICE_DEPENDENCY, matchedEnvVar, matchedValue);
    }
}
