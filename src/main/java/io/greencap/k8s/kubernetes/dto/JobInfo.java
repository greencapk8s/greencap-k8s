package io.greencap.k8s.kubernetes.dto;

public record JobInfo(
        String name,
        String namespace,
        String status,
        String completions,
        String duration,
        String age,
        String owner
) {}
