package io.greencap.k8s.kubernetes.dto;

// One entry in template.yaml's "builds" list — a component with no ready-made public image.
// Built via Kaniko using the Template's own repository as Git context; "image" is the destination
// repository (not the sentinel) used to push to the target Cluster's internal Registry.
public record TemplateBuild(String name, String contextPath, String dockerfilePath, String image) {
}
