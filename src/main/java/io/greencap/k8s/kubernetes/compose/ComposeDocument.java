package io.greencap.k8s.kubernetes.compose;

import java.util.List;

public record ComposeDocument(
        List<ParsedService> services,
        List<String> ignoredDirectives
) {
    public record ParsedService(
            String name,
            String image,
            BuildSpec build,
            List<Integer> containerPorts,
            List<EnvEntry> environment,
            List<VolumeEntry> volumes,
            List<String> dependsOn
    ) {
        public boolean hasBuild() { return build != null; }
        public boolean hasSensitiveEnv() { return environment.stream().anyMatch(EnvEntry::sensitive); }
        public boolean hasNonSensitiveEnv() { return environment.stream().anyMatch(e -> !e.sensitive()); }
        public List<VolumeEntry> namedVolumes() { return volumes.stream().filter(v -> !v.bindMount()).toList(); }
        public List<VolumeEntry> bindMounts() { return volumes.stream().filter(VolumeEntry::bindMount).toList(); }
    }

    public record BuildSpec(String context, String dockerfile) {}

    public record EnvEntry(String key, String value, boolean sensitive) {}

    public record VolumeEntry(String name, String mountPath, boolean bindMount) {}
}
