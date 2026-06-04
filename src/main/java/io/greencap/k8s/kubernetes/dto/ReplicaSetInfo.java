package io.greencap.k8s.kubernetes.dto;

public record ReplicaSetInfo(
        String name,
        String namespace,
        String owner,
        int desired,
        int ready,
        String age
) {}
