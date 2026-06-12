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

            var existingDeployments = (isAllNamespaces(namespace)
                    ? client.apps().deployments().inAnyNamespace().list().getItems()
                    : client.apps().deployments().inNamespace(namespace).list().getItems())
                    .stream()
                    .map(d -> d.getMetadata().getName())
                    .collect(java.util.stream.Collectors.toSet());

            return items.stream()
                    .map(hpa -> toInfo(hpa, existingDeployments))
                    .toList();

        } catch (Exception e) {
            log.error("Failed to list horizontal scalers for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list horizontal scalers: " + e.getMessage(), e);
        }
    }

    private HorizontalScalerInfo toInfo(HorizontalPodAutoscaler hpa, java.util.Set<String> existingDeployments) {
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

        boolean targetMissing = !"-".equals(target) && !existingDeployments.contains(target);

        return new HorizontalScalerInfo(
                hpa.getMetadata().getName(),
                hpa.getMetadata().getNamespace(),
                target,
                minReplicas,
                maxReplicas,
                currentReplicas,
                metrics,
                NamespaceService.age(hpa.getMetadata().getCreationTimestamp()),
                targetMissing
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

    public Optional<HorizontalScalerInfo> findHorizontalScalerForTarget(Cluster cluster, String namespace, String targetName) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            return client.autoscaling().v2().horizontalPodAutoscalers()
                    .inNamespace(namespace)
                    .list().getItems().stream()
                    .filter(hpa -> targetName.equals(
                            Optional.ofNullable(hpa.getSpec())
                                    .map(s -> s.getScaleTargetRef().getName())
                                    .orElse(null)))
                    .findFirst()
                    .map(hpa -> toInfo(hpa, java.util.Set.of(targetName)));
        } catch (Exception e) {
            log.error("Failed to find HPA for target {}/{}: {}", namespace, targetName, e.getMessage());
            throw new KubernetesOperationException("Failed to find HPA: " + e.getMessage(), e);
        }
    }

    public void updateHorizontalScaler(Cluster cluster, String namespace, String name, int minReplicas, int maxReplicas) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            client.autoscaling().v2().horizontalPodAutoscalers()
                    .inNamespace(namespace)
                    .withName(name)
                    .edit(hpa -> {
                        hpa.getSpec().setMinReplicas(minReplicas);
                        hpa.getSpec().setMaxReplicas(maxReplicas);
                        return hpa;
                    });
            log.info("Updated HPA {}/{}: min={}, max={}", namespace, name, minReplicas, maxReplicas);
        } catch (Exception e) {
            log.error("Failed to update HPA {}/{}: {}", namespace, name, e.getMessage());
            throw new KubernetesOperationException("Failed to update HPA: " + e.getMessage(), e);
        }
    }

    public void deleteHorizontalScaler(Cluster cluster, String namespace, String name) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            client.autoscaling().v2().horizontalPodAutoscalers().inNamespace(namespace).withName(name).delete();
            log.info("Deleted HPA {}/{}", namespace, name);
        } catch (Exception e) {
            log.error("Failed to delete HPA {}/{}: {}", namespace, name, e.getMessage());
            throw new KubernetesOperationException("Failed to delete HorizontalPodAutoscaler: " + e.getMessage(), e);
        }
    }

    private boolean isAllNamespaces(String namespace) {
        return namespace == null || namespace.isBlank() || "all".equalsIgnoreCase(namespace);
    }
}
