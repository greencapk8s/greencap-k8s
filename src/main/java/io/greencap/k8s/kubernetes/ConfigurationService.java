package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
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

    public List<ConfigMapInfo> listConfigMaps(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {
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
            throw KubernetesOperationException.from("Failed to list configmaps", e);
        }
    }

    public List<SecretInfo> listSecrets(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {
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
            throw KubernetesOperationException.from("Failed to list secrets", e);
        }
    }

    public void deleteConfigMap(Cluster cluster, String namespace, String name) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {
            client.configMaps().inNamespace(namespace).withName(name).delete();
            log.info("Deleted configmap {}/{}", namespace, name);
        } catch (Exception e) {
            log.error("Failed to delete configmap {}/{}: {}", namespace, name, e.getMessage());
            throw KubernetesOperationException.from("Failed to delete ConfigMap", e);
        }
    }

    public void deleteSecret(Cluster cluster, String namespace, String name) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {
            client.secrets().inNamespace(namespace).withName(name).delete();
            log.info("Deleted secret {}/{}", namespace, name);
        } catch (Exception e) {
            log.error("Failed to delete secret {}/{}: {}", namespace, name, e.getMessage());
            throw KubernetesOperationException.from("Failed to delete Secret", e);
        }
    }

    private boolean isAllNamespaces(String namespace) {
        return namespace == null || namespace.isBlank() || "all".equalsIgnoreCase(namespace);
    }
}
