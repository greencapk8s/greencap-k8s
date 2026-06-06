package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.NodeInfo;
import io.greencap.k8s.kubernetes.dto.PersistentVolumeClaimInfo;
import io.greencap.k8s.kubernetes.dto.PersistentVolumeInfo;
import io.greencap.k8s.kubernetes.dto.StorageClassInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final KubernetesClientFactory clientFactory;
    private final EncryptionService encryptionService;

    public List<PersistentVolumeClaimInfo> listPersistentVolumeClaims(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            var items = isAllNamespaces(namespace)
                    ? client.persistentVolumeClaims().inAnyNamespace().list().getItems()
                    : client.persistentVolumeClaims().inNamespace(namespace).list().getItems();

            return items.stream()
                    .map(pvc -> new PersistentVolumeClaimInfo(
                            pvc.getMetadata().getName(),
                            pvc.getMetadata().getNamespace(),
                            pvc.getMetadata().getDeletionTimestamp() != null ? "Terminating"
                                    : Optional.ofNullable(pvc.getStatus()).map(s -> s.getPhase()).orElse("Unknown"),
                            Optional.ofNullable(pvc.getSpec())
                                    .map(s -> s.getResources())
                                    .map(r -> r.getRequests())
                                    .map(r -> r.get("storage"))
                                    .map(q -> q.toString())
                                    .orElse("—"),
                            Optional.ofNullable(pvc.getSpec())
                                    .map(s -> s.getAccessModes())
                                    .filter(modes -> !modes.isEmpty())
                                    .map(modes -> modes.get(0))
                                    .orElse("—"),
                            Optional.ofNullable(pvc.getSpec())
                                    .map(s -> s.getStorageClassName())
                                    .filter(sc -> !sc.isBlank())
                                    .orElse("—"),
                            NamespaceService.age(pvc.getMetadata().getCreationTimestamp())
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list PVCs for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list PersistentVolumeClaims: " + e.getMessage(), e);
        }
    }

    public List<PersistentVolumeInfo> listPersistentVolumes(Cluster cluster) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            return client.persistentVolumes().list().getItems().stream()
                    .map(pv -> new PersistentVolumeInfo(
                            pv.getMetadata().getName(),
                            pv.getMetadata().getDeletionTimestamp() != null ? "Terminating"
                                    : Optional.ofNullable(pv.getStatus()).map(s -> s.getPhase()).orElse("Unknown"),
                            Optional.ofNullable(pv.getSpec())
                                    .map(s -> s.getCapacity())
                                    .map(c -> c.get("storage"))
                                    .map(q -> q.toString())
                                    .orElse("—"),
                            Optional.ofNullable(pv.getSpec())
                                    .map(s -> s.getAccessModes())
                                    .filter(modes -> !modes.isEmpty())
                                    .map(modes -> modes.get(0))
                                    .orElse("—"),
                            Optional.ofNullable(pv.getSpec())
                                    .map(s -> s.getPersistentVolumeReclaimPolicy())
                                    .orElse("—"),
                            Optional.ofNullable(pv.getSpec())
                                    .map(s -> s.getStorageClassName())
                                    .filter(sc -> sc != null && !sc.isBlank())
                                    .orElse("—"),
                            Optional.ofNullable(pv.getSpec())
                                    .map(s -> s.getClaimRef())
                                    .map(ref -> ref.getNamespace() + "/" + ref.getName())
                                    .orElse("—"),
                            NamespaceService.age(pv.getMetadata().getCreationTimestamp())
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list PVs for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list PersistentVolumes: " + e.getMessage(), e);
        }
    }

    public List<StorageClassInfo> listStorageClasses(Cluster cluster) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            return client.storage().v1().storageClasses().list().getItems().stream()
                    .map(sc -> new StorageClassInfo(
                            sc.getMetadata().getName(),
                            Optional.ofNullable(sc.getProvisioner()).orElse("—"),
                            Optional.ofNullable(sc.getReclaimPolicy()).orElse("—"),
                            Optional.ofNullable(sc.getVolumeBindingMode()).orElse("—"),
                            Boolean.TRUE.equals(sc.getAllowVolumeExpansion()) ? "Yes" : "No",
                            NamespaceService.age(sc.getMetadata().getCreationTimestamp())
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list StorageClasses for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list StorageClasses: " + e.getMessage(), e);
        }
    }

    public List<NodeInfo> listNodes(Cluster cluster) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            return client.nodes().list().getItems().stream()
                    .map(node -> {
                        var conditions = Optional.ofNullable(node.getStatus())
                                .map(s -> s.getConditions())
                                .orElse(List.of());
                        String status = conditions.stream()
                                .filter(c -> "Ready".equals(c.getType()))
                                .findFirst()
                                .map(c -> switch (c.getStatus()) {
                                    case "True"  -> "Ready";
                                    case "False" -> "NotReady";
                                    default      -> "Unknown";
                                })
                                .orElse("Unknown");

                        var labels = node.getMetadata().getLabels();
                        boolean isControlPlane = labels != null &&
                                (labels.containsKey("node-role.kubernetes.io/control-plane") ||
                                 labels.containsKey("node-role.kubernetes.io/master"));
                        String role = isControlPlane ? "Control Plane" : "Worker";

                        var nodeInfo = Optional.ofNullable(node.getStatus())
                                .map(s -> s.getNodeInfo());
                        String version = nodeInfo.map(i -> i.getKubeletVersion()).orElse("—");
                        String os      = nodeInfo.map(i -> i.getOsImage()).orElse("—");

                        var allocatable = Optional.ofNullable(node.getStatus())
                                .map(s -> s.getAllocatable())
                                .orElse(java.util.Map.of());
                        String cpu = Optional.ofNullable(allocatable.get("cpu"))
                                .map(q -> q.toString())
                                .orElse("—");
                        String memory = Optional.ofNullable(allocatable.get("memory"))
                                .map(q -> formatMemoryGib(q.toString()))
                                .orElse("—");

                        return new NodeInfo(
                                node.getMetadata().getName(),
                                status,
                                role,
                                version,
                                os,
                                cpu,
                                memory,
                                NamespaceService.age(node.getMetadata().getCreationTimestamp())
                        );
                    })
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list nodes for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list Nodes: " + e.getMessage(), e);
        }
    }

    private String formatMemoryGib(String kiString) {
        try {
            String digits = kiString.replaceAll("[^0-9]", "");
            long kibibytes = Long.parseLong(digits);
            double gib = kibibytes / (1024.0 * 1024.0);
            return String.format("%.1f GiB", gib);
        } catch (NumberFormatException e) {
            return kiString;
        }
    }

    private boolean isAllNamespaces(String namespace) {
        return namespace == null || namespace.isBlank() || "all".equalsIgnoreCase(namespace);
    }
}
