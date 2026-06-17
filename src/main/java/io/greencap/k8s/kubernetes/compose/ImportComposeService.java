package io.greencap.k8s.kubernetes.compose;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.KubernetesClientFactory;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.dto.ComposeImportRequest;
import io.greencap.k8s.kubernetes.dto.ImportComposeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportComposeService {

    private static final String TOPOLOGY_PART_OF_LABEL = "app.kubernetes.io/part-of";
    private static final String TOPOLOGY_COMPONENT_LABEL = "app.kubernetes.io/component";

    private final KubernetesClientFactory clientFactory;
    private final EncryptionService encryptionService;

    public ImportComposeResult provision(Cluster cluster,
                                         ComposeDocument document,
                                         ComposeImportRequest request) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {

            createNamespace(client, request.namespace());

            Map<String, ComposeImportRequest.ServiceConfig> configByService = request.serviceConfigs()
                    .stream().collect(Collectors.toMap(
                            ComposeImportRequest.ServiceConfig::serviceName, s -> s));

            List<ImportComposeResult.ServiceResult> results = new ArrayList<>();
            for (ComposeDocument.ParsedService service : document.services()) {
                ComposeImportRequest.ServiceConfig config = configByService.get(service.name());
                if (config == null) continue;
                results.add(provisionService(client, request.namespace(), service, config));
            }
            return new ImportComposeResult(results);

        } catch (Exception e) {
            log.error("Failed to provision Compose import for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to provision Compose import: " + e.getMessage(), e);
        }
    }

    private ImportComposeResult.ServiceResult provisionService(KubernetesClient client,
                                                                String namespace,
                                                                ComposeDocument.ParsedService service,
                                                                ComposeImportRequest.ServiceConfig config) {
        List<String> created = new ArrayList<>();
        String serviceName = service.name();
        Map<String, String> labels = Map.of(
                TOPOLOGY_PART_OF_LABEL, namespace,
                TOPOLOGY_COMPONENT_LABEL, serviceName,
                "app", serviceName
        );

        try {
            if (service.hasNonSensitiveEnv()) {
                createConfigMap(client, namespace, serviceName, service, labels);
                created.add("ConfigMap: " + serviceName + "-config");
            }
        } catch (Exception e) {
            log.error("Failed to create ConfigMap for service {}: {}", serviceName, e.getMessage());
            return new ImportComposeResult.ServiceResult(serviceName, created,
                    "ConfigMap failed: " + e.getMessage());
        }

        try {
            if (service.hasSensitiveEnv()) {
                createSecret(client, namespace, serviceName, service, labels);
                created.add("Secret: " + serviceName + "-secret");
            }
        } catch (Exception e) {
            log.error("Failed to create Secret for service {}: {}", serviceName, e.getMessage());
            return new ImportComposeResult.ServiceResult(serviceName, created,
                    "Secret failed: " + e.getMessage());
        }

        try {
            for (ComposeImportRequest.VolumeConfig volume : config.volumes()) {
                createPvc(client, namespace, serviceName, volume, labels);
                created.add("PersistentVolumeClaim: " + pvcName(serviceName, volume.volumeName()));
            }
        } catch (Exception e) {
            log.error("Failed to create PVC for service {}: {}", serviceName, e.getMessage());
            return new ImportComposeResult.ServiceResult(serviceName, created,
                    "PersistentVolumeClaim failed: " + e.getMessage());
        }

        try {
            createDeployment(client, namespace, serviceName, config.resolvedImage(), service, config, labels);
            created.add("Deployment: " + serviceName);
        } catch (Exception e) {
            log.error("Failed to create Deployment for service {}: {}", serviceName, e.getMessage());
            return new ImportComposeResult.ServiceResult(serviceName, created,
                    "Deployment failed: " + e.getMessage());
        }

        try {
            if (!service.containerPorts().isEmpty()) {
                createService(client, namespace, serviceName, service.containerPorts().get(0), labels);
                created.add("Service: " + serviceName);
            }
        } catch (Exception e) {
            log.error("Failed to create Service for service {}: {}", serviceName, e.getMessage());
            return new ImportComposeResult.ServiceResult(serviceName, created,
                    "Service failed: " + e.getMessage());
        }

        return new ImportComposeResult.ServiceResult(serviceName, created, null);
    }

    private void createNamespace(KubernetesClient client, String name) {
        client.namespaces().resource(
                new NamespaceBuilder()
                        .withNewMetadata().withName(name).endMetadata()
                        .build()
        ).create();
    }

    private void createConfigMap(KubernetesClient client, String namespace, String serviceName,
                                  ComposeDocument.ParsedService service,
                                  Map<String, String> labels) {
        Map<String, String> data = service.environment().stream()
                .filter(e -> !e.sensitive() && e.value() != null)
                .collect(Collectors.toMap(
                        ComposeDocument.EnvEntry::key,
                        ComposeDocument.EnvEntry::value));
        client.configMaps().inNamespace(namespace).resource(
                new ConfigMapBuilder()
                        .withNewMetadata()
                            .withName(serviceName + "-config")
                            .withNamespace(namespace)
                            .withLabels(labels)
                        .endMetadata()
                        .withData(data)
                        .build()
        ).create();
    }

    private void createSecret(KubernetesClient client, String namespace, String serviceName,
                               ComposeDocument.ParsedService service,
                               Map<String, String> labels) {
        Map<String, String> data = service.environment().stream()
                .filter(e -> e.sensitive() && e.value() != null)
                .collect(Collectors.toMap(
                        ComposeDocument.EnvEntry::key,
                        e -> java.util.Base64.getEncoder().encodeToString(
                                e.value().getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        client.secrets().inNamespace(namespace).resource(
                new SecretBuilder()
                        .withNewMetadata()
                            .withName(serviceName + "-secret")
                            .withNamespace(namespace)
                            .withLabels(labels)
                        .endMetadata()
                        .withType("Opaque")
                        .withData(data)
                        .build()
        ).create();
    }

    private void createPvc(KubernetesClient client, String namespace, String serviceName,
                            ComposeImportRequest.VolumeConfig volume,
                            Map<String, String> labels) {
        client.persistentVolumeClaims().inNamespace(namespace).resource(
                new PersistentVolumeClaimBuilder()
                        .withNewMetadata()
                            .withName(pvcName(serviceName, volume.volumeName()))
                            .withNamespace(namespace)
                            .withLabels(labels)
                        .endMetadata()
                        .withNewSpec()
                            .withStorageClassName(volume.storageClass())
                            .withAccessModes("ReadWriteOnce")
                            .withNewResources()
                                .addToRequests("storage", new Quantity(volume.storageGi() + "Gi"))
                            .endResources()
                        .endSpec()
                        .build()
        ).create();
    }

    private void createDeployment(KubernetesClient client, String namespace, String serviceName,
                                   String image,
                                   ComposeDocument.ParsedService service,
                                   ComposeImportRequest.ServiceConfig config,
                                   Map<String, String> labels) {
        var containerBuilder = new io.fabric8.kubernetes.api.model.ContainerBuilder()
                .withName(serviceName)
                .withImage(image);

        service.containerPorts().forEach(port ->
                containerBuilder.addNewPort().withContainerPort(port).endPort());

        if (service.hasNonSensitiveEnv()) {
            containerBuilder.addNewEnvFrom()
                    .withNewConfigMapRef().withName(serviceName + "-config").endConfigMapRef()
                    .endEnvFrom();
        }
        if (service.hasSensitiveEnv()) {
            containerBuilder.addNewEnvFrom()
                    .withNewSecretRef().withName(serviceName + "-secret").endSecretRef()
                    .endEnvFrom();
        }

        Map<String, ComposeImportRequest.VolumeConfig> volumeConfigByName = config.volumes().stream()
                .collect(Collectors.toMap(ComposeImportRequest.VolumeConfig::volumeName, v -> v));

        for (ComposeDocument.VolumeEntry volume : service.namedVolumes()) {
            containerBuilder.addNewVolumeMount()
                    .withName(volumeK8sName(volume.name()))
                    .withMountPath(volume.mountPath())
                    .endVolumeMount();
        }

        var podSpecBuilder = new io.fabric8.kubernetes.api.model.PodSpecBuilder()
                .addToContainers(containerBuilder.build());

        for (ComposeDocument.VolumeEntry volume : service.namedVolumes()) {
            String claimName = pvcName(serviceName, volume.name());
            podSpecBuilder.addNewVolume()
                    .withName(volumeK8sName(volume.name()))
                    .withNewPersistentVolumeClaim()
                        .withClaimName(claimName)
                    .endPersistentVolumeClaim()
                    .endVolume();
        }

        client.apps().deployments().inNamespace(namespace).resource(
                new DeploymentBuilder()
                        .withNewMetadata()
                            .withName(serviceName)
                            .withNamespace(namespace)
                            .withLabels(labels)
                        .endMetadata()
                        .withNewSpec()
                            .withReplicas(1)
                            .withNewSelector()
                                .withMatchLabels(Map.of("app", serviceName))
                            .endSelector()
                            .withNewTemplate()
                                .withNewMetadata().withLabels(labels).endMetadata()
                                .withSpec(podSpecBuilder.build())
                            .endTemplate()
                        .endSpec()
                        .build()
        ).create();
    }

    private void createService(KubernetesClient client, String namespace,
                                String serviceName, int port,
                                Map<String, String> labels) {
        client.services().inNamespace(namespace).resource(
                new ServiceBuilder()
                        .withNewMetadata()
                            .withName(serviceName)
                            .withNamespace(namespace)
                            .withLabels(labels)
                        .endMetadata()
                        .withNewSpec()
                            .withType("ClusterIP")
                            .withSelector(Map.of("app", serviceName))
                            .addNewPort()
                                .withPort(port)
                                .withTargetPort(new io.fabric8.kubernetes.api.model.IntOrString(port))
                            .endPort()
                        .endSpec()
                        .build()
        ).create();
    }

    private String pvcName(String serviceName, String volumeName) {
        String cleanVolume = volumeK8sName(volumeName);
        return serviceName.equals(cleanVolume)
                ? serviceName + "-pvc"
                : serviceName + "-" + cleanVolume + "-pvc";
    }

    private String volumeK8sName(String volumeName) {
        return volumeName.replaceAll("[^a-zA-Z0-9-]", "-").toLowerCase();
    }
}
