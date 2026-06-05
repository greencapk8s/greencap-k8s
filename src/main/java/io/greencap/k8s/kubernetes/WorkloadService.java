package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.DeploymentInfo;
import io.greencap.k8s.kubernetes.dto.PodInfo;
import io.greencap.k8s.kubernetes.dto.ReplicaSetInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkloadService {

    private final KubernetesClientFactory clientFactory;
    private final EncryptionService encryptionService;

    public List<PodInfo> listPods(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            var items = isAllNamespaces(namespace)
                    ? client.pods().inAnyNamespace().list().getItems()
                    : client.pods().inNamespace(namespace).list().getItems();

            return items.stream()
                    .map(pod -> new PodInfo(
                            pod.getMetadata().getName(),
                            pod.getMetadata().getNamespace(),
                            Optional.ofNullable(pod.getStatus()).map(s -> s.getPhase()).orElse("Unknown"),
                            Optional.ofNullable(pod.getSpec()).map(s -> s.getNodeName()).orElse("-"),
                            Optional.ofNullable(pod.getStatus())
                                    .map(s -> s.getContainerStatuses())
                                    .map(cs -> cs.stream()
                                            .mapToInt(c -> c.getRestartCount() != null ? c.getRestartCount() : 0)
                                            .sum())
                                    .orElse(0),
                            NamespaceService.age(pod.getMetadata().getCreationTimestamp())
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list pods for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list pods: " + e.getMessage(), e);
        }
    }

    public List<DeploymentInfo> listDeployments(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            var items = isAllNamespaces(namespace)
                    ? client.apps().deployments().inAnyNamespace().list().getItems()
                    : client.apps().deployments().inNamespace(namespace).list().getItems();

            return items.stream()
                    .map(d -> new DeploymentInfo(
                            d.getMetadata().getName(),
                            d.getMetadata().getNamespace(),
                            Optional.ofNullable(d.getSpec()).map(s -> s.getReplicas()).orElse(0),
                            Optional.ofNullable(d.getStatus()).map(s -> s.getReadyReplicas()).orElse(0),
                            Optional.ofNullable(d.getStatus()).map(s -> s.getAvailableReplicas()).orElse(0),
                            NamespaceService.age(d.getMetadata().getCreationTimestamp())
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list deployments for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list deployments: " + e.getMessage(), e);
        }
    }

    public List<ReplicaSetInfo> listReplicaSets(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            var items = isAllNamespaces(namespace)
                    ? client.apps().replicaSets().inAnyNamespace().list().getItems()
                    : client.apps().replicaSets().inNamespace(namespace).list().getItems();

            Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);

            return items.stream()
                    .filter(rs -> {
                        int desired = Optional.ofNullable(rs.getSpec()).map(s -> s.getReplicas()).orElse(0);
                        if (desired > 0) return true;
                        String ts = rs.getMetadata().getCreationTimestamp();
                        return ts != null && Instant.parse(ts).isAfter(oneDayAgo);
                    })
                    .map(rs -> {
                        String owner = Optional.ofNullable(rs.getMetadata().getOwnerReferences())
                                .flatMap(refs -> refs.stream()
                                        .filter(ref -> "Deployment".equals(ref.getKind()))
                                        .findFirst())
                                .map(ref -> ref.getName())
                                .orElse("—");

                        int desired = Optional.ofNullable(rs.getSpec())
                                .map(s -> s.getReplicas())
                                .orElse(0);

                        int ready = Optional.ofNullable(rs.getStatus())
                                .map(s -> s.getReadyReplicas())
                                .orElse(0);

                        return new ReplicaSetInfo(
                                rs.getMetadata().getName(),
                                rs.getMetadata().getNamespace(),
                                owner,
                                desired,
                                ready,
                                NamespaceService.age(rs.getMetadata().getCreationTimestamp())
                        );
                    })
                    .sorted(Comparator.comparingInt((ReplicaSetInfo rs) -> rs.desired() > 0 ? 0 : 1))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list replicasets for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list replicasets: " + e.getMessage(), e);
        }
    }

    public void scaleDeployment(Cluster cluster, String namespace, String name, int replicas) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            client.apps().deployments().inNamespace(namespace).withName(name).scale(replicas);
            log.info("Scaled deployment {}/{} to {} replicas", namespace, name, replicas);
        } catch (Exception e) {
            log.error("Failed to scale deployment {}/{}: {}", namespace, name, e.getMessage());
            throw new KubernetesOperationException("Failed to scale deployment: " + e.getMessage(), e);
        }
    }

    public void restartDeployment(Cluster cluster, String namespace, String name) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            client.apps().deployments().inNamespace(namespace).withName(name).rolling().restart();
            log.info("Restarted deployment {}/{}", namespace, name);
        } catch (Exception e) {
            log.error("Failed to restart deployment {}/{}: {}", namespace, name, e.getMessage());
            throw new KubernetesOperationException("Failed to restart deployment: " + e.getMessage(), e);
        }
    }

    private boolean isAllNamespaces(String namespace) {
        return namespace == null || namespace.isBlank() || "all".equalsIgnoreCase(namespace);
    }
}
