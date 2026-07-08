package io.greencap.k8s.kubernetes.dto;

import java.util.List;

public record OperatorPackage(
        String name,
        String displayName,
        String description,
        String provider,
        String catalogSource,
        List<OperatorChannel> channels,
        String defaultChannel
) {}
