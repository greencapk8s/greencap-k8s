package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.ServiceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkingService {

    private final KubernetesClientFactory clientFactory;
    private final EncryptionService encryptionService;

    public List<ServiceInfo> listServices(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            var items = isAllNamespaces(namespace)
                    ? client.services().inAnyNamespace().list().getItems()
                    : client.services().inNamespace(namespace).list().getItems();

            return items.stream()
                    .map(svc -> new ServiceInfo(
                            svc.getMetadata().getName(),
                            svc.getMetadata().getNamespace(),
                            Optional.ofNullable(svc.getSpec()).map(s -> s.getType()).orElse("ClusterIP"),
                            Optional.ofNullable(svc.getSpec()).map(s -> s.getClusterIP()).orElse("-"),
                            formatPorts(Optional.ofNullable(svc.getSpec())
                                    .map(s -> s.getPorts())
                                    .orElse(List.of())),
                            NamespaceService.age(svc.getMetadata().getCreationTimestamp())
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list services for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list services: " + e.getMessage(), e);
        }
    }

    private boolean isAllNamespaces(String namespace) {
        return namespace == null || namespace.isBlank() || "all".equalsIgnoreCase(namespace);
    }

    private String formatPorts(List<ServicePort> ports) {
        if (ports.isEmpty()) return "-";
        return ports.stream()
                .map(p -> p.getPort() + "/" + p.getProtocol())
                .collect(Collectors.joining(", "));
    }
}
