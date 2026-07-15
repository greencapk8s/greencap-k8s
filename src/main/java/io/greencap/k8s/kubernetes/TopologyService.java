package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.TopologyEdge;
import io.greencap.k8s.kubernetes.dto.TopologyGraph;
import io.greencap.k8s.kubernetes.dto.TopologyNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class TopologyService {

    private static final String LABEL_PART_OF = "app.kubernetes.io/part-of";
    private static final String LABEL_COMPONENT = "app.kubernetes.io/component";
    private static final Pattern CLUSTER_DNS_SUFFIX = Pattern.compile("^\\.([a-z0-9-]+)\\.svc\\.cluster\\.local\\b");

    private final KubernetesClientFactory clientFactory;

    public TopologyGraph buildGraph(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {

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
            List<Ingress> ingresses = client.network().v1().ingresses().inNamespace(namespace).list().getItems();
            List<StatefulSet> statefulSets = client.apps().statefulSets().inNamespace(namespace).list().getItems();
            Map<String, ConfigMap> configMapsByName = client.configMaps().inNamespace(namespace).list().getItems().stream()
                    .collect(Collectors.toMap(cm -> cm.getMetadata().getName(), cm -> cm, (a, b) -> a));
            Map<String, Secret> secretsByName = client.secrets().inNamespace(namespace).list().getItems().stream()
                    .collect(Collectors.toMap(s -> s.getMetadata().getName(), s -> s, (a, b) -> a));

            List<TopologyNode> nodes = new ArrayList<>();
            List<TopologyEdge> edges = new ArrayList<>();

            for (Deployment d : deployments) {
                nodes.add(deploymentNode(d));
            }

            for (StatefulSet sts : statefulSets) {
                nodes.add(statefulSetNode(sts));
            }

            for (ReplicaSet rs : replicaSets) {
                nodes.add(replicaSetNode(rs));
                ownerDeploymentId(rs).ifPresent(ownerId ->
                        edges.add(TopologyEdge.structural(ownerId, nodeId("replicaset", rs.getMetadata().getName()))));
            }

            // Group pods by owner (ReplicaSet or StatefulSet); orphans remain individual.
            // StatefulSet owns its Pods directly — no intermediate ReplicaSet.
            Map<String, List<Pod>> podsByOwner = new LinkedHashMap<>();
            List<Pod> orphanPods = new ArrayList<>();

            for (Pod pod : pods) {
                Optional<String> ownerId = podOwnerId(pod);
                if (ownerId.isPresent()) {
                    podsByOwner.computeIfAbsent(ownerId.get(), k -> new ArrayList<>()).add(pod);
                } else {
                    orphanPods.add(pod);
                }
            }

            for (Map.Entry<String, List<Pod>> entry : podsByOwner.entrySet()) {
                String ownerId = entry.getKey();
                List<Pod> group = entry.getValue();
                nodes.add(podGroupNode(ownerId, group));
                edges.add(TopologyEdge.structural(ownerId, podGroupId(ownerId)));
            }

            for (Pod pod : orphanPods) {
                nodes.add(podNode(pod));
            }

            for (Service svc : services) {
                nodes.add(serviceNode(svc));
                Map<String, String> selector = Optional.ofNullable(svc.getSpec())
                        .map(s -> s.getSelector())
                        .orElse(Map.of());
                if (selector.isEmpty()) continue;

                // Edge to pod groups
                for (Map.Entry<String, List<Pod>> entry : podsByOwner.entrySet()) {
                    if (entry.getValue().stream().anyMatch(pod -> podMatchesSelector(pod, selector))) {
                        edges.add(TopologyEdge.structural(
                                nodeId("service", svc.getMetadata().getName()),
                                podGroupId(entry.getKey())));
                    }
                }
                // Edge to orphan pods
                orphanPods.stream()
                        .filter(pod -> podMatchesSelector(pod, selector))
                        .forEach(pod -> edges.add(TopologyEdge.structural(
                                nodeId("service", svc.getMetadata().getName()),
                                nodeId("pod", pod.getMetadata().getName()))));
            }

            for (PersistentVolumeClaim pvc : pvcs) {
                nodes.add(pvcNode(pvc));
            }

            Set<String> serviceNames = services.stream()
                    .map(svc -> svc.getMetadata().getName())
                    .collect(Collectors.toSet());

            for (Ingress ing : ingresses) {
                nodes.add(ingressNode(ing));
                String ingressId = nodeId("ingress", ing.getMetadata().getName());
                for (String svcName : extractBackendServiceNames(ing)) {
                    if (serviceNames.contains(svcName)) {
                        edges.add(TopologyEdge.structural(ingressId, nodeId("service", svcName)));
                    }
                }
            }

            for (Map.Entry<String, List<Pod>> entry : podsByOwner.entrySet()) {
                Set<String> claimNames = mountedClaimNames(entry.getValue().get(0));
                for (String claimName : claimNames) {
                    edges.add(TopologyEdge.structural(podGroupId(entry.getKey()), nodeId("persistentvolumeclaim", claimName)));
                }
            }

            for (Pod orphan : orphanPods) {
                Set<String> claimNames = mountedClaimNames(orphan);
                for (String claimName : claimNames) {
                    edges.add(TopologyEdge.structural(nodeId("pod", orphan.getMetadata().getName()), nodeId("persistentvolumeclaim", claimName)));
                }
            }

            for (Map.Entry<String, List<Pod>> entry : podsByOwner.entrySet()) {
                addServiceDependencyEdges(edges, podGroupId(entry.getKey()), entry.getValue().get(0),
                        services, configMapsByName, secretsByName, namespace);
            }
            for (Pod orphan : orphanPods) {
                addServiceDependencyEdges(edges, nodeId("pod", orphan.getMetadata().getName()), orphan,
                        services, configMapsByName, secretsByName, namespace);
            }

            return new TopologyGraph(nodes, edges);

        } catch (Exception e) {
            log.error("Failed to build topology graph for cluster {}: {}", cluster.getName(), e.getMessage());
            throw KubernetesOperationException.from("Failed to build topology graph", e);
        }
    }

    private TopologyNode deploymentNode(Deployment d) {
        int ready = Optional.ofNullable(d.getStatus()).map(s -> s.getReadyReplicas()).orElse(0);
        int desired = Optional.ofNullable(d.getSpec()).map(s -> s.getReplicas()).orElse(0);
        String status = desired == 0 ? "Unknown" : (ready >= desired ? "Running" : "Degraded");
        Map<String, String> labels = Optional.ofNullable(d.getMetadata().getLabels()).orElse(Map.of());
        String name = d.getMetadata().getName();
        return new TopologyNode(
                nodeId("deployment", name),
                name,
                "Deployment",
                status,
                resourceViewUrl("deployment", name),
                labels, ready, desired, "", "", "", partOfGroup(labels), componentGroup(labels));
    }

    private TopologyNode statefulSetNode(StatefulSet sts) {
        int ready = Optional.ofNullable(sts.getStatus()).map(s -> s.getReadyReplicas()).orElse(0);
        int desired = Optional.ofNullable(sts.getSpec()).map(s -> s.getReplicas()).orElse(0);
        String status = desired == 0 ? "Unknown" : (ready >= desired ? "Running" : "Degraded");
        Map<String, String> labels = Optional.ofNullable(sts.getMetadata().getLabels()).orElse(Map.of());
        String name = sts.getMetadata().getName();
        return new TopologyNode(
                nodeId("statefulset", name),
                name,
                "StatefulSet",
                status,
                resourceViewUrl("statefulset", name),
                labels, ready, desired, "", "", "", partOfGroup(labels), componentGroup(labels));
    }

    private TopologyNode replicaSetNode(ReplicaSet rs) {
        int ready = Optional.ofNullable(rs.getStatus()).map(s -> s.getReadyReplicas()).orElse(0);
        int desired = Optional.ofNullable(rs.getSpec()).map(s -> s.getReplicas()).orElse(0);
        String status = desired == 0 ? "Unknown" : (ready >= desired ? "Running" : "Degraded");
        Map<String, String> labels = Optional.ofNullable(rs.getMetadata().getLabels()).orElse(Map.of());
        String name = rs.getMetadata().getName();
        return new TopologyNode(
                nodeId("replicaset", name),
                name,
                "ReplicaSet",
                status,
                resourceViewUrl("replicaset", name),
                labels, ready, desired, "", "", "", partOfGroup(labels), componentGroup(labels));
    }

    private TopologyNode podGroupNode(String ownerId, List<Pod> group) {
        int count = group.size();
        String countLabel = count == 1 ? "1 Pod" : count + " Pods";
        String status = aggregatePodStatus(group);
        String baseName = podGroupBaseName(ownerId);
        Map<String, String> labels = Optional.ofNullable(group.get(0).getMetadata().getLabels()).orElse(Map.of());
        return new TopologyNode(
                podGroupId(ownerId),
                baseName,
                countLabel,
                status,
                "workloads/pods",
                Map.of(), 0, count, "", "", "", partOfGroup(labels), componentGroup(labels));
    }

    private TopologyNode podNode(Pod pod) {
        String phase = Optional.ofNullable(pod.getStatus()).map(s -> s.getPhase()).orElse("Unknown");
        Map<String, String> labels = Optional.ofNullable(pod.getMetadata().getLabels()).orElse(Map.of());
        String name = pod.getMetadata().getName();
        return new TopologyNode(
                nodeId("pod", name),
                name,
                "1 Pod",
                phase,
                "workloads/pods",
                labels, 0, 0, "", "", "", partOfGroup(labels), componentGroup(labels));
    }

    private TopologyNode serviceNode(Service svc) {
        String serviceType = Optional.ofNullable(svc.getSpec()).map(s -> s.getType()).orElse("");
        Map<String, String> labels = Optional.ofNullable(svc.getMetadata().getLabels()).orElse(Map.of());
        String name = svc.getMetadata().getName();
        return new TopologyNode(
                nodeId("service", name),
                name,
                "Service",
                "Active",
                resourceViewUrl("service", name),
                labels, 0, 0, serviceType, "", "", partOfGroup(labels), componentGroup(labels));
    }

    private TopologyNode ingressNode(Ingress ing) {
        String ingressClass = Optional.ofNullable(ing.getSpec())
                .map(s -> s.getIngressClassName())
                .filter(c -> c != null && !c.isBlank())
                .orElse("—");
        String hosts = Optional.ofNullable(ing.getSpec())
                .map(s -> s.getRules())
                .filter(rules -> rules != null && !rules.isEmpty())
                .map(rules -> rules.stream()
                        .map(r -> r.getHost() != null ? r.getHost() : "*")
                        .distinct()
                        .collect(Collectors.joining(", ")))
                .orElse("—");
        boolean hasTls = Optional.ofNullable(ing.getSpec())
                .map(s -> s.getTls())
                .map(tls -> !tls.isEmpty())
                .orElse(false);
        String name = ing.getMetadata().getName();
        return new TopologyNode(
                nodeId("ingress", name),
                name,
                "Ingress",
                "Active",
                resourceViewUrl("ingress", name),
                Map.of(), 0, 0, ingressClass, hosts, hasTls ? "Secure" : "Plain", "", "");
    }

    private Set<String> extractBackendServiceNames(Ingress ing) {
        Set<String> names = new java.util.LinkedHashSet<>();
        Optional.ofNullable(ing.getSpec())
                .map(s -> s.getDefaultBackend())
                .map(b -> b.getService())
                .map(s -> s.getName())
                .filter(n -> n != null && !n.isBlank())
                .ifPresent(names::add);
        Optional.ofNullable(ing.getSpec())
                .map(s -> s.getRules())
                .orElse(List.of())
                .forEach(rule -> Optional.ofNullable(rule.getHttp())
                        .map(h -> h.getPaths())
                        .orElse(List.of())
                        .forEach(path -> Optional.ofNullable(path.getBackend())
                                .map(b -> b.getService())
                                .map(s -> s.getName())
                                .filter(n -> n != null && !n.isBlank())
                                .ifPresent(names::add)));
        return names;
    }

    private TopologyNode pvcNode(PersistentVolumeClaim pvc) {
        Map<String, String> labels = Optional.ofNullable(pvc.getMetadata().getLabels()).orElse(Map.of());
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
        String name = pvc.getMetadata().getName();
        return new TopologyNode(
                nodeId("persistentvolumeclaim", name),
                name,
                "PersistentVolumeClaim",
                status,
                resourceViewUrl("persistentvolumeclaim", name),
                Map.of(), 0, 0, storageClass, capacity, accessMode, partOfGroup(labels), componentGroup(labels));
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

    private record EnvValueCandidate(String name, String value) {}

    private void addServiceDependencyEdges(List<TopologyEdge> edges, String sourceId, Pod pod, List<Service> services,
                                            Map<String, ConfigMap> configMapsByName, Map<String, Secret> secretsByName,
                                            String namespace) {
        List<EnvValueCandidate> candidates = collectEnvCandidates(pod, configMapsByName, secretsByName);
        if (candidates.isEmpty()) return;

        Set<String> matchedServiceNames = new LinkedHashSet<>();
        for (Service svc : services) {
            String serviceName = svc.getMetadata().getName();
            if (matchedServiceNames.contains(serviceName)) continue;
            for (EnvValueCandidate candidate : candidates) {
                if (referencesService(candidate.value(), serviceName, namespace)) {
                    matchedServiceNames.add(serviceName);
                    edges.add(TopologyEdge.serviceDependency(
                            sourceId, nodeId("service", serviceName), candidate.name(), candidate.value()));
                    break;
                }
            }
        }
    }

    private List<EnvValueCandidate> collectEnvCandidates(Pod pod, Map<String, ConfigMap> configMapsByName,
                                                           Map<String, Secret> secretsByName) {
        List<Container> containers = new ArrayList<>();
        Optional.ofNullable(pod.getSpec()).ifPresent(spec -> {
            containers.addAll(Optional.ofNullable(spec.getContainers()).orElse(List.of()));
            containers.addAll(Optional.ofNullable(spec.getInitContainers()).orElse(List.of()));
        });

        List<EnvValueCandidate> candidates = new ArrayList<>();
        for (Container container : containers) {
            for (EnvVar env : Optional.ofNullable(container.getEnv()).orElse(List.of())) {
                if (env.getValue() != null && !env.getValue().isBlank()) {
                    candidates.add(new EnvValueCandidate(env.getName(), env.getValue()));
                } else if (env.getValueFrom() != null) {
                    if (env.getValueFrom().getConfigMapKeyRef() != null) {
                        var ref = env.getValueFrom().getConfigMapKeyRef();
                        String value = configMapData(ref.getName(), configMapsByName).get(ref.getKey());
                        if (value != null && !value.isBlank()) candidates.add(new EnvValueCandidate(env.getName(), value));
                    } else if (env.getValueFrom().getSecretKeyRef() != null) {
                        var ref = env.getValueFrom().getSecretKeyRef();
                        String value = secretData(ref.getName(), secretsByName).get(ref.getKey());
                        if (value != null && !value.isBlank()) candidates.add(new EnvValueCandidate(env.getName(), value));
                    }
                }
            }
            for (EnvFromSource envFrom : Optional.ofNullable(container.getEnvFrom()).orElse(List.of())) {
                if (envFrom.getConfigMapRef() != null) {
                    configMapData(envFrom.getConfigMapRef().getName(), configMapsByName)
                            .forEach((key, value) -> candidates.add(new EnvValueCandidate(key, value)));
                } else if (envFrom.getSecretRef() != null) {
                    secretData(envFrom.getSecretRef().getName(), secretsByName)
                            .forEach((key, value) -> candidates.add(new EnvValueCandidate(key, value)));
                }
            }
        }
        return candidates;
    }

    private Map<String, String> configMapData(String name, Map<String, ConfigMap> configMapsByName) {
        return Optional.ofNullable(configMapsByName.get(name)).map(ConfigMap::getData).orElse(Map.of());
    }

    private Map<String, String> secretData(String name, Map<String, Secret> secretsByName) {
        return Optional.ofNullable(secretsByName.get(name)).map(Secret::getData).orElse(Map.of())
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> decodeBase64(e.getValue())));
    }

    private String decodeBase64(String value) {
        try {
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    /**
     * Substring/word-boundary match of serviceName within value, rejecting matches that are
     * part of a cluster-DNS FQDN pointing at a different Namespace (see ADR 0018).
     */
    private boolean referencesService(String value, String serviceName, String activeNamespace) {
        if (value == null || value.isBlank()) return false;
        int idx = value.indexOf(serviceName);
        while (idx != -1) {
            int endIdx = idx + serviceName.length();
            boolean leftBoundary = idx == 0 || !Character.isLetterOrDigit(value.charAt(idx - 1));
            boolean rightBoundary = endIdx == value.length() || !Character.isLetterOrDigit(value.charAt(endIdx));
            if (leftBoundary && rightBoundary && !referencesOtherNamespace(value, endIdx, activeNamespace)) {
                return true;
            }
            idx = value.indexOf(serviceName, idx + 1);
        }
        return false;
    }

    private boolean referencesOtherNamespace(String value, int matchEnd, String activeNamespace) {
        Matcher matcher = CLUSTER_DNS_SUFFIX.matcher(value.substring(matchEnd));
        return matcher.lookingAt() && !activeNamespace.equals(matcher.group(1));
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

    private Optional<String> podOwnerId(Pod pod) {
        return Optional.ofNullable(pod.getMetadata().getOwnerReferences())
                .flatMap(refs -> refs.stream()
                        .filter(ref -> "ReplicaSet".equals(ref.getKind()) || "StatefulSet".equals(ref.getKind()))
                        .findFirst())
                .map(ref -> nodeId(ref.getKind().toLowerCase(), ref.getName()));
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

    private String podGroupId(String ownerId) {
        // Keeps the pre-StatefulSet ID shape ("pod-group/<name>", no kind prefix) so
        // previously saved TopologyLayout positions for ReplicaSet-owned groups still match.
        return "pod-group/" + ownerName(ownerId);
    }

    private String podGroupBaseName(String ownerId) {
        // Strip the RS template-hash suffix to get the base name; StatefulSet owns Pods
        // directly under its own name, with no such suffix to strip.
        return "replicaset".equals(ownerKind(ownerId)) ? stripLastSegment(ownerName(ownerId)) : ownerName(ownerId);
    }

    private String ownerKind(String ownerId) {
        return ownerId.substring(0, ownerId.indexOf('/'));
    }

    private String ownerName(String ownerId) {
        return ownerId.substring(ownerId.indexOf('/') + 1);
    }

    private String partOfGroup(Map<String, String> labels) {
        return labels.getOrDefault(LABEL_PART_OF, "");
    }

    private String componentGroup(Map<String, String> labels) {
        return labels.getOrDefault(LABEL_COMPONENT, "");
    }

    private String nodeId(String type, String name) {
        return type + "/" + name;
    }

    private String resourceViewUrl(String resourceType, String name) {
        return switch (resourceType) {
            case "deployment" -> "workloads/deployments?name=" + name;
            case "statefulset" -> "workloads/statefulsets?name=" + name;
            case "replicaset" -> "workloads/replicasets?name=" + name;
            case "service" -> "networking/services?name=" + name;
            case "persistentvolumeclaim" -> "storage/pvcs?name=" + name;
            case "ingress" -> "networking/ingresses?name=" + name;
            default -> "";
        };
    }
}
