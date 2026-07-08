package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.DeployApplicationRequest;
import io.greencap.k8s.kubernetes.dto.DeployApplicationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeployApplicationService {

    private final KubernetesClientFactory clientFactory;

    public DeployApplicationResult deploy(Cluster cluster, DeployApplicationRequest request) {
        List<String> created = new ArrayList<>();
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {

            createNamespace(client, request.namespace());
            created.add("Namespace: " + request.namespace());

            createDeployment(client, request);
            created.add("Deployment: " + request.namespace());

            if (request.containerPort() != null) {
                try {
                    createService(client, request);
                    created.add("Service: " + request.namespace());
                } catch (Exception e) {
                    log.error("Failed to create Service for application {}: {}", request.namespace(), e.getMessage());
                    return new DeployApplicationResult(created, "Service", e.getMessage());
                }
            }

            if (request.volume() != null) {
                try {
                    createPvc(client, request);
                    created.add("PersistentVolumeClaim: " + request.namespace() + "-pvc");
                } catch (Exception e) {
                    log.error("Failed to create PVC for application {}: {}", request.namespace(), e.getMessage());
                    return new DeployApplicationResult(created, "PersistentVolumeClaim", e.getMessage());
                }
            }

            if (request.ingress() != null && request.containerPort() != null) {
                try {
                    createIngress(client, request);
                    created.add("Ingress: " + request.namespace() + "-ingress");
                } catch (Exception e) {
                    log.error("Failed to create Ingress for application {}: {}", request.namespace(), e.getMessage());
                    return new DeployApplicationResult(created, "Ingress", e.getMessage());
                }
            }

            return new DeployApplicationResult(created, null, null);

        } catch (Exception e) {
            log.error("Failed to deploy application {} on cluster {}: {}", request.namespace(), cluster.getName(), e.getMessage());
            throw KubernetesOperationException.from("Failed to deploy application", e);
        }
    }

    private void createNamespace(KubernetesClient client, String name) {
        client.namespaces().resource(
                new NamespaceBuilder()
                        .withNewMetadata().withName(name).endMetadata()
                        .build()
        ).create();
    }

    private void createDeployment(KubernetesClient client, DeployApplicationRequest req) {
        var resources = new ResourceRequirementsBuilder()
                .addToRequests("cpu", new Quantity(req.cpuRequest()))
                .addToRequests("memory", new Quantity(req.memoryRequest()))
                .addToLimits("cpu", new Quantity(req.cpuLimit()))
                .addToLimits("memory", new Quantity(req.memoryLimit()))
                .build();

        var containerBuilder = new ContainerBuilder()
                .withName(req.namespace())
                .withImage(req.image())
                .withResources(resources);

        if (req.containerPort() != null) {
            containerBuilder.addNewPort()
                    .withContainerPort(req.containerPort())
                    .endPort();
        }

        if (req.volume() != null) {
            containerBuilder.addNewVolumeMount()
                    .withName("data")
                    .withMountPath(req.volume().mountPath())
                    .endVolumeMount();
        }

        var podSpecBuilder = new PodSpecBuilder()
                .addToContainers(containerBuilder.build());

        if (req.volume() != null) {
            podSpecBuilder.addNewVolume()
                    .withName("data")
                    .withNewPersistentVolumeClaim()
                        .withClaimName(req.namespace() + "-pvc")
                    .endPersistentVolumeClaim()
                    .endVolume();
        }

        client.apps().deployments().inNamespace(req.namespace()).resource(
                new DeploymentBuilder()
                        .withNewMetadata()
                            .withName(req.namespace())
                            .withNamespace(req.namespace())
                        .endMetadata()
                        .withNewSpec()
                            .withReplicas(req.replicas())
                            .withNewSelector()
                                .withMatchLabels(Map.of("app", req.namespace()))
                            .endSelector()
                            .withNewTemplate()
                                .withNewMetadata()
                                    .withLabels(Map.of("app", req.namespace()))
                                .endMetadata()
                                .withSpec(podSpecBuilder.build())
                            .endTemplate()
                        .endSpec()
                        .build()
        ).create();
    }

    private void createService(KubernetesClient client, DeployApplicationRequest req) {
        client.services().inNamespace(req.namespace()).resource(
                new ServiceBuilder()
                        .withNewMetadata()
                            .withName(req.namespace())
                            .withNamespace(req.namespace())
                        .endMetadata()
                        .withNewSpec()
                            .withType("ClusterIP")
                            .withSelector(Map.of("app", req.namespace()))
                            .addNewPort()
                                .withPort(req.containerPort())
                                .withTargetPort(new IntOrString(req.containerPort()))
                            .endPort()
                        .endSpec()
                        .build()
        ).create();
    }

    private void createPvc(KubernetesClient client, DeployApplicationRequest req) {
        var pvc = req.volume();
        client.persistentVolumeClaims().inNamespace(req.namespace()).resource(
                new PersistentVolumeClaimBuilder()
                        .withNewMetadata()
                            .withName(req.namespace() + "-pvc")
                            .withNamespace(req.namespace())
                        .endMetadata()
                        .withNewSpec()
                            .withStorageClassName(pvc.storageClass())
                            .withAccessModes("ReadWriteOnce")
                            .withNewResources()
                                .addToRequests("storage", new Quantity(pvc.storageGi() + "Gi"))
                            .endResources()
                        .endSpec()
                        .build()
        ).create();
    }

    private void createIngress(KubernetesClient client, DeployApplicationRequest req) {
        var ingress = req.ingress();
        client.network().v1().ingresses().inNamespace(req.namespace()).resource(
                new IngressBuilder()
                        .withNewMetadata()
                            .withName(req.namespace() + "-ingress")
                            .withNamespace(req.namespace())
                        .endMetadata()
                        .withNewSpec()
                            .withIngressClassName(ingress.ingressClassName())
                            .addNewRule()
                                .withHost(ingress.host())
                                .withNewHttp()
                                    .addNewPath()
                                        .withPath("/")
                                        .withPathType("Prefix")
                                        .withNewBackend()
                                            .withNewService()
                                                .withName(req.namespace())
                                                .withNewPort()
                                                    .withNumber(req.containerPort())
                                                .endPort()
                                            .endService()
                                        .endBackend()
                                    .endPath()
                                .endHttp()
                            .endRule()
                        .endSpec()
                        .build()
        ).create();
    }
}
