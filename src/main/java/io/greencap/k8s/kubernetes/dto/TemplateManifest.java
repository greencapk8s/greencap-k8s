package io.greencap.k8s.kubernetes.dto;

import java.util.List;

// Deserialized from a Template's template.yaml. "resources" are applied in the listed order —
// the file defining the Template's Namespace must come first. "builds" may be absent (null here;
// SampleCatalogService normalizes it to an empty list) when every component uses a public image.
public record TemplateManifest(List<String> resources, List<TemplateBuild> builds) {
}
