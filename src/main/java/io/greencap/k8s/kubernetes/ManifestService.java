package io.greencap.k8s.kubernetes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManifestService {

    // Resource types that support Apply, mapped to the Kubernetes `kind` they must declare.
    private static final Map<String, String> EDITABLE_RESOURCE_KINDS = Map.ofEntries(
            Map.entry("pod", "Pod"),
            Map.entry("deployment", "Deployment"),
            Map.entry("statefulset", "StatefulSet"),
            Map.entry("replicaset", "ReplicaSet"),
            Map.entry("job", "Job"),
            Map.entry("cronjob", "CronJob"),
            Map.entry("service", "Service"),
            Map.entry("ingress", "Ingress"),
            Map.entry("configmap", "ConfigMap"),
            Map.entry("secret", "Secret"),
            Map.entry("horizontalscaler", "HorizontalPodAutoscaler"),
            Map.entry("persistentvolumeclaim", "PersistentVolumeClaim")
    );

    // Server-managed fields stripped before Apply so the replace targets the latest
    // server state instead of failing optimistic locking on status churn (ADR 0005).
    private static final List<String> SERVER_MANAGED_METADATA_FIELDS =
            List.of("resourceVersion", "uid", "creationTimestamp", "generation", "managedFields");

    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();

    private final KubernetesClientFactory clientFactory;
    private final EncryptionService encryptionService;

    public static boolean isEditable(String resourceType) {
        return EDITABLE_RESOURCE_KINDS.containsKey(resourceType.toLowerCase());
    }

    public String fetchYaml(Cluster cluster, String resourceType, String namespace, String name) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {

            Object resource = switch (resourceType.toLowerCase()) {
                case "pod"        -> client.pods().inNamespace(namespace).withName(name).get();
                case "deployment" -> client.apps().deployments().inNamespace(namespace).withName(name).get();
                case "statefulset" -> client.apps().statefulSets().inNamespace(namespace).withName(name).get();
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

    public void applyYaml(Cluster cluster, String resourceType, String namespace, String name, String yamlContent) {
        ObjectNode resource = parseAndValidate(resourceType, namespace, name, yamlContent);

        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {

            String cleanedYaml = YAML_MAPPER.writeValueAsString(resource);
            client.resource(cleanedYaml).inNamespace(namespace).update();

        } catch (KubernetesOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to apply manifest for {}/{}/{}: {}", resourceType, namespace, name, e.getMessage());
            throw new KubernetesOperationException("Failed to apply manifest: " + e.getMessage(), e);
        }
    }

    private ObjectNode parseAndValidate(String resourceType, String namespace, String name, String yamlContent) {
        String expectedKind = EDITABLE_RESOURCE_KINDS.get(resourceType.toLowerCase());
        if (expectedKind == null) {
            throw new KubernetesOperationException("Resource type '" + resourceType + "' does not support editing", null);
        }

        ObjectNode resource;
        try {
            resource = (ObjectNode) YAML_MAPPER.readTree(yamlContent);
        } catch (JsonProcessingException e) {
            throw new KubernetesOperationException("Invalid YAML: " + e.getMessage(), e);
        }

        if (!expectedKind.equals(textValue(resource, "kind"))) {
            throw new KubernetesOperationException("Cannot change resource kind via Manifest editing", null);
        }

        JsonNode metadata = resource.get("metadata");
        if (metadata == null || !metadata.isObject()) {
            throw new KubernetesOperationException("Invalid YAML: missing metadata", null);
        }
        if (!name.equals(textValue(metadata, "name"))) {
            throw new KubernetesOperationException("Cannot change resource name via Manifest editing", null);
        }
        if (!namespace.equals(textValue(metadata, "namespace"))) {
            throw new KubernetesOperationException("Cannot change resource namespace via Manifest editing", null);
        }

        ((ObjectNode) metadata).remove(SERVER_MANAGED_METADATA_FIELDS);
        resource.remove("status");

        return resource;
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null ? null : value.asText();
    }
}
