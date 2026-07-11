package io.greencap.k8s.kubernetes.dto;

import java.util.List;

// One entry from catalog.json at the root of greencap-templates — see ADR 0015.
public record TemplateSummary(
        String id,
        String title,
        String description,
        List<String> technologies,
        String path,
        String namespace) {
}
