package io.greencap.k8s.kubernetes.dto;

public record OperatorChannel(
        String name,
        String currentCSV
) {}
