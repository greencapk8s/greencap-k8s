package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.NamespaceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NamespaceService {

    private final KubernetesClientFactory clientFactory;

    public List<NamespaceInfo> listNamespaces(Cluster cluster) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {
            return client.namespaces().list().getItems().stream()
                    .map(ns -> new NamespaceInfo(
                            ns.getMetadata().getName(),
                            Optional.ofNullable(ns.getStatus())
                                    .map(s -> s.getPhase())
                                    .orElse("Unknown"),
                            age(ns.getMetadata().getCreationTimestamp()),
                            0, 0, 0
                    ))
                    .sorted(Comparator.comparing(NamespaceInfo::name))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list namespaces for cluster {}: {}", cluster.getName(), e.getMessage());
            throw KubernetesOperationException.from("Failed to list namespaces", e);
        }
    }

    public List<NamespaceInfo> listNamespacesWithCounts(Cluster cluster) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {

            Map<String, Long> podCounts = client.pods().inAnyNamespace().list().getItems().stream()
                    .collect(Collectors.groupingBy(p -> p.getMetadata().getNamespace(), Collectors.counting()));

            Map<String, Long> deploymentCounts = client.apps().deployments().inAnyNamespace().list().getItems().stream()
                    .collect(Collectors.groupingBy(d -> d.getMetadata().getNamespace(), Collectors.counting()));

            Map<String, Long> serviceCounts = client.services().inAnyNamespace().list().getItems().stream()
                    .collect(Collectors.groupingBy(s -> s.getMetadata().getNamespace(), Collectors.counting()));

            return client.namespaces().list().getItems().stream()
                    .map(ns -> {
                        String name = ns.getMetadata().getName();
                        return new NamespaceInfo(
                                name,
                                Optional.ofNullable(ns.getStatus())
                                        .map(s -> s.getPhase())
                                        .orElse("Unknown"),
                                age(ns.getMetadata().getCreationTimestamp()),
                                podCounts.getOrDefault(name, 0L).intValue(),
                                deploymentCounts.getOrDefault(name, 0L).intValue(),
                                serviceCounts.getOrDefault(name, 0L).intValue()
                        );
                    })
                    .sorted(Comparator.comparing(NamespaceInfo::name))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list namespaces with counts for cluster {}: {}", cluster.getName(), e.getMessage());
            throw KubernetesOperationException.from("Failed to list namespaces", e);
        }
    }

    public List<String> listNamespaceNames(Cluster cluster) {
        return listNamespaces(cluster).stream()
                .filter(ns -> !"Terminating".equals(ns.phase()))
                .map(NamespaceInfo::name)
                .toList();
    }

    public void createNamespace(Cluster cluster, String name) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {
            client.namespaces().resource(
                    new NamespaceBuilder()
                            .withNewMetadata()
                            .withName(name)
                            .endMetadata()
                            .build()
            ).create();
        } catch (Exception e) {
            log.error("Failed to create namespace {} in cluster {}: {}", name, cluster.getName(), e.getMessage());
            throw KubernetesOperationException.from("Failed to create namespace", e);
        }
    }

    public void deleteNamespace(Cluster cluster, String name) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {
            client.namespaces().withName(name).delete();
        } catch (Exception e) {
            log.error("Failed to delete namespace {} in cluster {}: {}", name, cluster.getName(), e.getMessage());
            throw KubernetesOperationException.from("Failed to delete namespace", e);
        }
    }

    static String age(String creationTimestamp) {
        if (creationTimestamp == null) return "-";
        try {
            Duration d = Duration.between(Instant.parse(creationTimestamp), Instant.now());
            if (d.toDays() > 0)  return d.toDays() + "d";
            if (d.toHours() > 0) return d.toHours() + "h";
            return d.toMinutes() + "m";
        } catch (Exception e) {
            return "-";
        }
    }
}
