package io.greencap.k8s.domain.cluster;

public record CreateClusterRequest(
        String name,
        String kubeconfigContent
) {}
