package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.Pod;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

public final class PodNodeResolver {

    private static final String NO_NODES = "—";

    private PodNodeResolver() {
    }

    public static String resolveNodes(List<Pod> pods, String namespace, Map<String, String> matchLabels) {
        if (matchLabels == null || matchLabels.isEmpty()) {
            return NO_NODES;
        }

        var nodes = new TreeSet<String>();
        for (Pod pod : pods) {
            if (!namespace.equals(pod.getMetadata().getNamespace())) continue;
            if (!matchesLabels(pod, matchLabels)) continue;
            Optional.ofNullable(pod.getSpec())
                    .map(spec -> spec.getNodeName())
                    .ifPresent(nodes::add);
        }

        return nodes.isEmpty() ? NO_NODES : String.join(", ", nodes);
    }

    private static boolean matchesLabels(Pod pod, Map<String, String> matchLabels) {
        Map<String, String> podLabels = Optional.ofNullable(pod.getMetadata().getLabels()).orElse(Map.of());
        return matchLabels.entrySet().stream()
                .allMatch(entry -> entry.getValue().equals(podLabels.get(entry.getKey())));
    }
}
