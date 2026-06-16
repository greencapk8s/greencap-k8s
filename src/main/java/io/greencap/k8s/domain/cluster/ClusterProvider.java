package io.greencap.k8s.domain.cluster;

public enum ClusterProvider {
    MinikubeDocker,
    OpenShift;

    public String displayName() {
        return switch (this) {
            case MinikubeDocker -> "Minikube (Docker)";
            case OpenShift -> "OpenShift";
        };
    }
}
