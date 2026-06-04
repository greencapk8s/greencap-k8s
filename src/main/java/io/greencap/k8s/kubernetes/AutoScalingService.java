package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.autoscaling.v2.MetricStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.HorizontalScalerInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoScalingService {

    private final KubernetesClientFactory clientFactory;
    private final EncryptionService encryptionService;

    public List<HorizontalScalerInfo> listHorizontalScalers(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {

            var items = isAllNamespaces(namespace)
                    ? client.autoscaling().v2().horizontalPodAutoscalers().inAnyNamespace().list().getItems()
                    : client.autoscaling().v2().horizontalPodAutoscalers().inNamespace(namespace).list().getItems();

            return items.stream()
                    .map(this::toInfo)
                    .toList();

        } catch (Exception e) {
            log.error("Failed to list horizontal scalers for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list horizontal scalers: " + e.getMessage(), e);
        }
    }

    private HorizontalScalerInfo toInfo(HorizontalPodAutoscaler hpa) {
        String target = Optional.ofNullable(hpa.getSpec())
                .map(s -> s.getScaleTargetRef().getName())
                .orElse("-");

        int minReplicas = Optional.ofNullable(hpa.getSpec())
                .map(s -> s.getMinReplicas())
                .orElse(1);

        int maxReplicas = Optional.ofNullable(hpa.getSpec())
                .map(s -> s.getMaxReplicas())
                .orElse(0);

        int currentReplicas = Optional.ofNullable(hpa.getStatus())
                .map(s -> s.getCurrentReplicas())
                .orElse(0);

        String metrics = buildMetricsSummary(hpa);

        return new HorizontalScalerInfo(
                hpa.getMetadata().getName(),
                hpa.getMetadata().getNamespace(),
                target,
                minReplicas,
                maxReplicas,
                currentReplicas,
                metrics,
                NamespaceService.age(hpa.getMetadata().getCreationTimestamp())
        );
    }

    private String buildMetricsSummary(HorizontalPodAutoscaler hpa) {
        var specMetrics = Optional.ofNullable(hpa.getSpec())
                .map(s -> s.getMetrics())
                .filter(m -> !m.isEmpty());

        if (specMetrics.isEmpty()) {
            return "-";
        }

        var firstSpec = specMetrics.get().get(0);
        String type = firstSpec.getType().toLowerCase();

        String target = switch (firstSpec.getType()) {
            case "Resource" -> Optional.ofNullable(firstSpec.getResource())
                    .map(r -> r.getTarget())
                    .map(t -> t.getAverageUtilization() != null
                            ? t.getAverageUtilization() + "%"
                            : t.getAverageValue() != null ? t.getAverageValue().toString() : "-")
                    .orElse("-");
            default -> "-";
        };

        String current = Optional.ofNullable(hpa.getStatus())
                .map(s -> s.getCurrentMetrics())
                .filter(m -> !m.isEmpty())
                .map(m -> currentMetricValue(m.get(0)))
                .orElse(null);

        return current != null
                ? type + ": " + current + "/" + target
                : type + ": " + target;
    }

    private String currentMetricValue(MetricStatus metricStatus) {
        return switch (metricStatus.getType()) {
            case "Resource" -> Optional.ofNullable(metricStatus.getResource())
                    .map(r -> r.getCurrent())
                    .map(c -> c.getAverageUtilization() != null
                            ? c.getAverageUtilization() + "%"
                            : c.getAverageValue() != null ? c.getAverageValue().toString() : null)
                    .orElse(null);
            default -> null;
        };
    }

    private boolean isAllNamespaces(String namespace) {
        return namespace == null || namespace.isBlank() || "all".equalsIgnoreCase(namespace);
    }
}
