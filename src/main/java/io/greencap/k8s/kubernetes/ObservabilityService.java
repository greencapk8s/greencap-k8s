package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.EventInfo;
import io.greencap.k8s.kubernetes.dto.PodMetricInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObservabilityService {

    private final KubernetesClientFactory clientFactory;
    private final EncryptionService encryptionService;

    public List<EventInfo> listEvents(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {

            var items = isAllNamespaces(namespace)
                    ? client.v1().events().inAnyNamespace().list().getItems()
                    : client.v1().events().inNamespace(namespace).list().getItems();

            log.debug("Found {} events in namespace '{}' for cluster '{}'", items.size(), namespace, cluster.getName());

            return items.stream()
                    .sorted(Comparator.comparing(
                            (Event e) -> Optional.ofNullable(e.getLastTimestamp()).orElse(""),
                            Comparator.reverseOrder()))
                    .map(e -> new EventInfo(
                            Optional.ofNullable(e.getType()).orElse("Normal"),
                            Optional.ofNullable(e.getReason()).orElse("-"),
                            formatObject(e),
                            Optional.ofNullable(e.getMessage()).orElse("-"),
                            Optional.ofNullable(e.getCount()).orElse(1),
                            NamespaceService.age(
                                    Optional.ofNullable(e.getLastTimestamp())
                                            .orElse(e.getMetadata().getCreationTimestamp()))
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list events for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list events: " + e.getMessage(), e);
        }
    }

    public List<EventInfo> listEventsForResource(Cluster cluster, String namespace, String kind, String name) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {

            var items = client.v1().events()
                    .inNamespace(namespace)
                    .withField("involvedObject.name", name)
                    .withField("involvedObject.kind", kind)
                    .list()
                    .getItems();

            log.debug("Found {} events for {}/{} in namespace '{}' on cluster '{}'",
                    items.size(), kind, name, namespace, cluster.getName());

            return items.stream()
                    .sorted(Comparator.comparing(
                            (Event e) -> Optional.ofNullable(e.getLastTimestamp()).orElse(""),
                            Comparator.reverseOrder()))
                    .map(e -> new EventInfo(
                            Optional.ofNullable(e.getType()).orElse("Normal"),
                            Optional.ofNullable(e.getReason()).orElse("-"),
                            formatObject(e),
                            Optional.ofNullable(e.getMessage()).orElse("-"),
                            Optional.ofNullable(e.getCount()).orElse(1),
                            NamespaceService.age(
                                    Optional.ofNullable(e.getLastTimestamp())
                                            .orElse(e.getMetadata().getCreationTimestamp()))
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list events for {}/{} on cluster {}: {}", kind, name, cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list events for " + kind + "/" + name + ": " + e.getMessage(), e);
        }
    }

    public List<PodMetricInfo> listPodMetrics(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {

            List<PodMetrics> items = isAllNamespaces(namespace)
                    ? client.top().pods().metrics("").getItems()
                    : client.top().pods().metrics(namespace).getItems();

            return items.stream()
                    .map(pm -> {
                        long cpu = pm.getContainers().stream()
                                .mapToLong(c -> toMillicores(c.getUsage().get("cpu")))
                                .sum();
                        long mem = pm.getContainers().stream()
                                .mapToLong(c -> toMiB(c.getUsage().get("memory")))
                                .sum();
                        return new PodMetricInfo(
                                pm.getMetadata().getName(),
                                pm.getMetadata().getNamespace(),
                                cpu,
                                mem
                        );
                    })
                    .sorted(Comparator.comparingLong(PodMetricInfo::cpuMillicores).reversed())
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list pod metrics for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list metrics: " + e.getMessage(), e);
        }
    }

    private long toMillicores(Quantity q) {
        if (q == null) return 0;
        String amount = q.getAmount();
        String format = q.getFormat();
        if (amount == null || amount.isBlank()) return 0;
        return switch (format == null ? "" : format) {
            case "m"  -> Long.parseLong(amount);
            case "n"  -> Long.parseLong(amount) / 1_000_000;
            case "u"  -> Long.parseLong(amount) / 1_000;
            default   -> (long) (Double.parseDouble(amount) * 1000);
        };
    }

    private long toMiB(Quantity q) {
        if (q == null) return 0;
        String amount = q.getAmount();
        String format = q.getFormat();
        if (amount == null || amount.isBlank()) return 0;
        long value = Long.parseLong(amount);
        return switch (format == null ? "" : format) {
            case "Ki" -> value / 1024;
            case "Mi" -> value;
            case "Gi" -> value * 1024;
            case "Ti" -> value * 1024 * 1024;
            case "k"  -> value * 1_000 / (1024 * 1024);
            case "M"  -> value * 1_000_000L / (1024 * 1024);
            case "G"  -> value * 1_000_000_000L / (1024 * 1024);
            default   -> value / (1024 * 1024);
        };
    }

    private boolean isAllNamespaces(String namespace) {
        return namespace == null || namespace.isBlank() || "all".equalsIgnoreCase(namespace);
    }

    private String formatObject(Event event) {
        var ref = event.getInvolvedObject();
        if (ref == null) return "-";
        String kind = Optional.ofNullable(ref.getKind()).orElse("?");
        String name = Optional.ofNullable(ref.getName()).orElse("?");
        return kind + "/" + name;
    }
}
