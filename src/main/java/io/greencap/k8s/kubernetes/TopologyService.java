package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.TopologyEdge;
import io.greencap.k8s.kubernetes.dto.TopologyGraph;
import io.greencap.k8s.kubernetes.dto.TopologyNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class TopologyService {

    private final KubernetesClientFactory clientFactory;
    private final EncryptionService encryptionService;

    public TopologyGraph buildGraph(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {

            List<Deployment> deployments = client.apps().deployments().inNamespace(namespace).list().getItems();
            List<ReplicaSet> replicaSets = client.apps().replicaSets().inNamespace(namespace).list().getItems()
                    .stream()
                    .filter(rs -> Optional.ofNullable(rs.getSpec()).map(s -> s.getReplicas()).orElse(0) > 0)
                    .toList();
            List<Pod> pods = client.pods().inNamespace(namespace).list().getItems().stream()
                    .filter(pod -> !isOwnedByJob(pod))
                    .toList();
            List<Service> services = client.services().inNamespace(namespace).list().getItems();
            List<PersistentVolumeClaim> pvcs = client.persistentVolumeClaims().inNamespace(namespace).list().getItems();

            List<TopologyNode> nodes = new ArrayList<>();
            List<TopologyEdge> edges = new ArrayList<>();

            for (Deployment d : deployments) {
                nodes.add(deploymentNode(d, namespace));
            }

            for (ReplicaSet rs : replicaSets) {
                nodes.add(replicaSetNode(rs, namespace));
                ownerDeploymentId(rs).ifPresent(ownerId ->
                        edges.add(new TopologyEdge(ownerId, nodeId("replicaset", rs.getMetadata().getName()))));
            }

            // Group pods by owner ReplicaSet; orphans remain individual
            Map<String, List<Pod>> podsByOwnerRs = new LinkedHashMap<>();
            List<Pod> orphanPods = new ArrayList<>();

            for (Pod pod : pods) {
                Optional<String> ownerRsName = ownerReplicaSetName(pod);
                if (ownerRsName.isPresent()) {
                    podsByOwnerRs.computeIfAbsent(ownerRsName.get(), k -> new ArrayList<>()).add(pod);
                } else {
                    orphanPods.add(pod);
                }
            }

            for (Map.Entry<String, List<Pod>> entry : podsByOwnerRs.entrySet()) {
                String rsName = entry.getKey();
                List<Pod> group = entry.getValue();
                nodes.add(podGroupNode(rsName, group));
                edges.add(new TopologyEdge(nodeId("replicaset", rsName), podGroupId(rsName)));
            }

            for (Pod pod : orphanPods) {
                nodes.add(podNode(pod, namespace));
            }

            for (Service svc : services) {
                nodes.add(serviceNode(svc, namespace));
                Map<String, String> selector = Optional.ofNullable(svc.getSpec())
                        .map(s -> s.getSelector())
                        .orElse(Map.of());
                if (selector.isEmpty()) continue;

                // Edge to pod groups
                for (Map.Entry<String, List<Pod>> entry : podsByOwnerRs.entrySet()) {
                    if (entry.getValue().stream().anyMatch(pod -> podMatchesSelector(pod, selector))) {
                        edges.add(new TopologyEdge(
                                nodeId("service", svc.getMetadata().getName()),
                                podGroupId(entry.getKey())));
                    }
                }
                // Edge to orphan pods
                orphanPods.stream()
                        .filter(pod -> podMatchesSelector(pod, selector))
                        .forEach(pod -> edges.add(new TopologyEdge(
                                nodeId("service", svc.getMetadata().getName()),
                                nodeId("pod", pod.getMetadata().getName()))));
            }

            for (PersistentVolumeClaim pvc : pvcs) {
                nodes.add(pvcNode(pvc, namespace));
            }

            for (Map.Entry<String, List<Pod>> entry : podsByOwnerRs.entrySet()) {
                Set<String> claimNames = mountedClaimNames(entry.getValue().get(0));
                for (String claimName : claimNames) {
                    edges.add(new TopologyEdge(podGroupId(entry.getKey()), nodeId("persistentvolumeclaim", claimName)));
                }
            }

            for (Pod orphan : orphanPods) {
                Set<String> claimNames = mountedClaimNames(orphan);
                for (String claimName : claimNames) {
                    edges.add(new TopologyEdge(nodeId("pod", orphan.getMetadata().getName()), nodeId("persistentvolumeclaim", claimName)));
                }
            }

            return new TopologyGraph(nodes, edges);

        } catch (Exception e) {
            log.error("Failed to build topology graph for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to build topology graph: " + e.getMessage(), e);
        }
    }

    private TopologyNode deploymentNode(Deployment d, String namespace) {
        int ready = Optional.ofNullable(d.getStatus()).map(s -> s.getReadyReplicas()).orElse(0);
        int desired = Optional.ofNullable(d.getSpec()).map(s -> s.getReplicas()).orElse(0);
        String status = desired == 0 ? "Unknown" : (ready >= desired ? "Running" : "Degraded");
        Map<String, String> labels = Optional.ofNullable(d.getMetadata().getLabels()).orElse(Map.of());
        return new TopologyNode(
                nodeId("deployment", d.getMetadata().getName()),
                d.getMetadata().getName(),
                "Deployment",
                status,
                manifestUrl("deployment", namespace, d.getMetadata().getName()),
                labels, ready, desired, "", "", "");
    }

    private TopologyNode replicaSetNode(ReplicaSet rs, String namespace) {
        int ready = Optional.ofNullable(rs.getStatus()).map(s -> s.getReadyReplicas()).orElse(0);
        int desired = Optional.ofNullable(rs.getSpec()).map(s -> s.getReplicas()).orElse(0);
        String status = desired == 0 ? "Unknown" : (ready >= desired ? "Running" : "Degraded");
        Map<String, String> labels = Optional.ofNullable(rs.getMetadata().getLabels()).orElse(Map.of());
        return new TopologyNode(
                nodeId("replicaset", rs.getMetadata().getName()),
                rs.getMetadata().getName(),
                "ReplicaSet",
                status,
                manifestUrl("replicaset", namespace, rs.getMetadata().getName()),
                labels, ready, desired, "", "", "");
    }

    private TopologyNode podGroupNode(String rsName, List<Pod> group) {
        int count = group.size();
        String countLabel = count == 1 ? "1 Pod" : count + " Pods";
        String status = aggregatePodStatus(group);
        // Strip the RS template-hash suffix to get the base name
        String baseName = stripLastSegment(rsName);
        return new TopologyNode(
                podGroupId(rsName),
                baseName,
                countLabel,
                status,
                "workloads/pods",
                Map.of(), 0, count, "", "", "");
    }

    private TopologyNode podNode(Pod pod, String namespace) {
        String phase = Optional.ofNullable(pod.getStatus()).map(s -> s.getPhase()).orElse("Unknown");
        Map<String, String> labels = Optional.ofNullable(pod.getMetadata().getLabels()).orElse(Map.of());
        return new TopologyNode(
                nodeId("pod", pod.getMetadata().getName()),
                pod.getMetadata().getName(),
                "1 Pod",
                phase,
                manifestUrl("pod", namespace, pod.getMetadata().getName()),
                labels, 0, 0, "", "", "");
    }

    private TopologyNode serviceNode(Service svc, String namespace) {
        String serviceType = Optional.ofNullable(svc.getSpec()).map(s -> s.getType()).orElse("");
        Map<String, String> labels = Optional.ofNullable(svc.getMetadata().getLabels()).orElse(Map.of());
        return new TopologyNode(
                nodeId("service", svc.getMetadata().getName()),
                svc.getMetadata().getName(),
                "Service",
                "Active",
                manifestUrl("service", namespace, svc.getMetadata().getName()),
                labels, 0, 0, serviceType, "", "");
    }

    private TopologyNode pvcNode(PersistentVolumeClaim pvc, String namespace) {
        String phase = Optional.ofNullable(pvc.getStatus()).map(s -> s.getPhase()).orElse("Unknown");
        String status = derivePvcStatus(pvc, phase);
        String storageClass = Optional.ofNullable(pvc.getSpec()).map(s -> s.getStorageClassName()).orElse("");
        String capacity = Optional.ofNullable(pvc.getStatus())
                .map(s -> s.getCapacity())
                .map(c -> c.get("storage"))
                .map(q -> q.toString())
                .orElse("");
        String accessMode = Optional.ofNullable(pvc.getStatus())
                .map(s -> s.getAccessModes())
                .filter(modes -> !modes.isEmpty())
                .map(modes -> modes.get(0))
                .orElse("");
        return new TopologyNode(
                nodeId("persistentvolumeclaim", pvc.getMetadata().getName()),
                pvc.getMetadata().getName(),
                "PersistentVolumeClaim",
                status,
                manifestUrl("persistentvolumeclaim", namespace, pvc.getMetadata().getName()),
                Map.of(), 0, 0, storageClass, capacity, accessMode);
    }

    private String derivePvcStatus(PersistentVolumeClaim pvc, String phase) {
        if (pvc.getMetadata().getDeletionTimestamp() != null) return "Terminating";
        return switch (phase) {
            case "Bound" -> "Bound";
            case "Pending" -> "Pending";
            case "Lost" -> "Lost";
            default -> "Unknown";
        };
    }

    private Set<String> mountedClaimNames(Pod pod) {
        return Optional.ofNullable(pod.getSpec())
                .map(spec -> spec.getVolumes())
                .orElse(List.of())
                .stream()
                .map(Volume::getPersistentVolumeClaim)
                .filter(ref -> ref != null)
                .map(ref -> ref.getClaimName())
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toSet());
    }

    private String aggregatePodStatus(List<Pod> pods) {
        boolean allRunning = pods.stream().allMatch(p ->
                "Running".equals(Optional.ofNullable(p.getStatus()).map(s -> s.getPhase()).orElse("")));
        if (allRunning) return "Running";
        boolean anyFailed = pods.stream().anyMatch(p ->
                "Failed".equals(Optional.ofNullable(p.getStatus()).map(s -> s.getPhase()).orElse("")));
        return anyFailed ? "Failed" : "Degraded";
    }

    private String stripLastSegment(String name) {
        int lastDash = name.lastIndexOf('-');
        return lastDash > 0 ? name.substring(0, lastDash) : name;
    }

    private Optional<String> ownerDeploymentId(ReplicaSet rs) {
        return Optional.ofNullable(rs.getMetadata().getOwnerReferences())
                .flatMap(refs -> refs.stream()
                        .filter(ref -> "Deployment".equals(ref.getKind()))
                        .findFirst())
                .map(ref -> nodeId("deployment", ref.getName()));
    }

    private Optional<String> ownerReplicaSetName(Pod pod) {
        return Optional.ofNullable(pod.getMetadata().getOwnerReferences())
                .flatMap(refs -> refs.stream()
                        .filter(ref -> "ReplicaSet".equals(ref.getKind()))
                        .findFirst())
                .map(ref -> ref.getName());
    }

    private boolean isOwnedByJob(Pod pod) {
        return Optional.ofNullable(pod.getMetadata().getOwnerReferences())
                .map(refs -> refs.stream().anyMatch(ref -> "Job".equals(ref.getKind())))
                .orElse(false);
    }

    private boolean podMatchesSelector(Pod pod, Map<String, String> selector) {
        Map<String, String> podLabels = Optional.ofNullable(pod.getMetadata().getLabels()).orElse(Map.of());
        return selector.entrySet().stream().allMatch(e -> e.getValue().equals(podLabels.get(e.getKey())));
    }

    private String podGroupId(String rsName) {
        return "pod-group/" + rsName;
    }

    private String nodeId(String type, String name) {
        return type + "/" + name;
    }

    private String manifestUrl(String resourceType, String namespace, String name) {
        return "yaml/" + resourceType + "/" + namespace + "/" + name;
    }
}
