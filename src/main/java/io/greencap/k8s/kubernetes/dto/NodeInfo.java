package io.greencap.k8s.kubernetes.dto;

public record NodeInfo(
        String name,
        String status,
        String role,
        String version,
        String os,
        String cpu,
        String memory,
        String age,
        boolean schedulingDisabled
) {}
