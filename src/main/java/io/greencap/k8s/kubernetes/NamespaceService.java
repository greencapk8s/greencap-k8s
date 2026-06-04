package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.NamespaceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NamespaceService {

    private final KubernetesClientFactory clientFactory;
    private final EncryptionService encryptionService;

    public List<NamespaceInfo> listNamespaces(Cluster cluster) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            return client.namespaces().list().getItems().stream()
                    .map(ns -> new NamespaceInfo(
                            ns.getMetadata().getName(),
                            Optional.ofNullable(ns.getStatus())
                                    .map(s -> s.getPhase())
                                    .orElse("Unknown"),
                            age(ns.getMetadata().getCreationTimestamp())
                    ))
                    .sorted(Comparator.comparing(NamespaceInfo::name))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list namespaces for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list namespaces: " + e.getMessage(), e);
        }
    }

    public List<String> listNamespaceNames(Cluster cluster) {
        return listNamespaces(cluster).stream()
                .map(NamespaceInfo::name)
                .toList();
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
