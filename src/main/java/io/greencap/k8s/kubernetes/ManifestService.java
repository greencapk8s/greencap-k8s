package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManifestService {

    private final KubernetesClientFactory clientFactory;
    private final EncryptionService encryptionService;

    public String fetchYaml(Cluster cluster, String resourceType, String namespace, String name) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {

            Object resource = switch (resourceType.toLowerCase()) {
                case "pod"        -> client.pods().inNamespace(namespace).withName(name).get();
                case "deployment" -> client.apps().deployments().inNamespace(namespace).withName(name).get();
                case "service"    -> client.services().inNamespace(namespace).withName(name).get();
                case "configmap"  -> client.configMaps().inNamespace(namespace).withName(name).get();
                case "secret"     -> client.secrets().inNamespace(namespace).withName(name).get();
                case "replicaset"       -> client.apps().replicaSets().inNamespace(namespace).withName(name).get();
                case "horizontalscaler" -> client.autoscaling().v2().horizontalPodAutoscalers().inNamespace(namespace).withName(name).get();
                case "persistentvolumeclaim" -> client.persistentVolumeClaims().inNamespace(namespace).withName(name).get();
                case "persistentvolume" -> client.persistentVolumes().withName(name).get();
                case "storageclass"     -> client.storage().v1().storageClasses().withName(name).get();
                case "job"     -> client.batch().v1().jobs().inNamespace(namespace).withName(name).get();
                case "cronjob" -> client.batch().v1().cronjobs().inNamespace(namespace).withName(name).get();
                case "node"    -> client.nodes().withName(name).get();
                case "ingress" -> client.network().v1().ingresses().inNamespace(namespace).withName(name).get();
                default -> throw new KubernetesOperationException("Unknown resource type: " + resourceType, null);
            };

            if (resource == null) {
                throw new KubernetesOperationException(
                        resourceType + " '" + name + "' not found in namespace '" + namespace + "'", null);
            }

            return Serialization.asYaml(resource);

        } catch (KubernetesOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch manifest for {}/{}/{}: {}", resourceType, namespace, name, e.getMessage());
            throw new KubernetesOperationException("Failed to fetch manifest: " + e.getMessage(), e);
        }
    }
}
