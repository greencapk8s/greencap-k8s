package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.ConfigMapInfo;
import io.greencap.k8s.kubernetes.dto.SecretInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurationService {

    private final KubernetesClientFactory clientFactory;
    private final EncryptionService encryptionService;

    public List<ConfigMapInfo> listConfigMaps(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            var items = isAllNamespaces(namespace)
                    ? client.configMaps().inAnyNamespace().list().getItems()
                    : client.configMaps().inNamespace(namespace).list().getItems();

            return items.stream()
                    .map(cm -> new ConfigMapInfo(
                            cm.getMetadata().getName(),
                            cm.getMetadata().getNamespace(),
                            Optional.ofNullable(cm.getData()).map(d -> d.size()).orElse(0),
                            NamespaceService.age(cm.getMetadata().getCreationTimestamp())
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list configmaps for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list configmaps: " + e.getMessage(), e);
        }
    }

    public List<SecretInfo> listSecrets(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            var items = isAllNamespaces(namespace)
                    ? client.secrets().inAnyNamespace().list().getItems()
                    : client.secrets().inNamespace(namespace).list().getItems();

            return items.stream()
                    .map(secret -> new SecretInfo(
                            secret.getMetadata().getName(),
                            secret.getMetadata().getNamespace(),
                            Optional.ofNullable(secret.getType()).orElse("Opaque"),
                            Optional.ofNullable(secret.getData()).map(d -> d.size()).orElse(0),
                            NamespaceService.age(secret.getMetadata().getCreationTimestamp())
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list secrets for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list secrets: " + e.getMessage(), e);
        }
    }

    private boolean isAllNamespaces(String namespace) {
        return namespace == null || namespace.isBlank() || "all".equalsIgnoreCase(namespace);
    }
}
