package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.IngressInfo;
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

    public List<ServiceInfo> listServices(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {
            var items = isAllNamespaces(namespace)
                    ? client.services().inAnyNamespace().list().getItems()
                    : client.services().inNamespace(namespace).list().getItems();

            var endpointsByName = (isAllNamespaces(namespace)
                    ? client.endpoints().inAnyNamespace().list().getItems()
                    : client.endpoints().inNamespace(namespace).list().getItems())
                    .stream()
                    .collect(java.util.stream.Collectors.toMap(
                            ep -> ep.getMetadata().getName(),
                            ep -> ep,
                            (a, b) -> a
                    ));

            return items.stream()
                    .map(svc -> {
                        String svcType = Optional.ofNullable(svc.getSpec()).map(s -> s.getType()).orElse("ClusterIP");
                        boolean hasReadyEndpoints = hasReadyAddresses(endpointsByName.get(svc.getMetadata().getName()), svcType);
                        return new ServiceInfo(
                                svc.getMetadata().getName(),
                                svc.getMetadata().getNamespace(),
                                svcType,
                                Optional.ofNullable(svc.getSpec()).map(s -> s.getClusterIP()).orElse("-"),
                                formatPorts(Optional.ofNullable(svc.getSpec())
                                        .map(s -> s.getPorts())
                                        .orElse(List.of())),
                                NamespaceService.age(svc.getMetadata().getCreationTimestamp()),
                                hasReadyEndpoints
                        );
                    })
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list services for cluster {}: {}", cluster.getName(), e.getMessage());
            throw KubernetesOperationException.from("Failed to list services", e);
        }
    }

    private boolean hasReadyAddresses(io.fabric8.kubernetes.api.model.Endpoints endpoints, String serviceType) {
        // ExternalName services route via DNS — no endpoints object is meaningful
        if ("ExternalName".equals(serviceType)) return true;
        if (endpoints == null) return false;
        var subsets = endpoints.getSubsets();
        if (subsets == null || subsets.isEmpty()) return false;
        return subsets.stream()
                .anyMatch(s -> s.getAddresses() != null && !s.getAddresses().isEmpty());
    }

    public List<IngressInfo> listIngresses(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {
            var items = isAllNamespaces(namespace)
                    ? client.network().v1().ingresses().inAnyNamespace().list().getItems()
                    : client.network().v1().ingresses().inNamespace(namespace).list().getItems();

            return items.stream()
                    .map(ing -> {
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

                        String address = Optional.ofNullable(ing.getStatus())
                                .map(s -> s.getLoadBalancer())
                                .map(lb -> lb.getIngress())
                                .filter(lbi -> lbi != null && !lbi.isEmpty())
                                .map(lbi -> lbi.stream()
                                        .map(e -> e.getIp() != null ? e.getIp() : e.getHostname())
                                        .filter(v -> v != null && !v.isBlank())
                                        .collect(Collectors.joining(", ")))
                                .filter(v -> !v.isBlank())
                                .orElse("—");

                        return new IngressInfo(
                                ing.getMetadata().getName(),
                                ing.getMetadata().getNamespace(),
                                ingressClass,
                                hosts,
                                hasTls,
                                address,
                                NamespaceService.age(ing.getMetadata().getCreationTimestamp())
                        );
                    })
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list ingresses for cluster {}: {}", cluster.getName(), e.getMessage());
            throw KubernetesOperationException.from("Failed to list ingresses", e);
        }
    }

    public List<String> listIngressClassNames(Cluster cluster) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {
            return client.network().v1().ingressClasses().list().getItems().stream()
                    .map(ic -> ic.getMetadata().getName())
                    .sorted()
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list IngressClasses for cluster {}: {}", cluster.getName(), e.getMessage());
            throw KubernetesOperationException.from("Failed to list IngressClasses", e);
        }
    }

    public void deleteService(Cluster cluster, String namespace, String name) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {
            client.services().inNamespace(namespace).withName(name).delete();
            log.info("Deleted service {}/{}", namespace, name);
        } catch (Exception e) {
            log.error("Failed to delete service {}/{}: {}", namespace, name, e.getMessage());
            throw KubernetesOperationException.from("Failed to delete Service", e);
        }
    }

    public void deleteIngress(Cluster cluster, String namespace, String name) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {
            client.network().v1().ingresses().inNamespace(namespace).withName(name).delete();
            log.info("Deleted ingress {}/{}", namespace, name);
        } catch (Exception e) {
            log.error("Failed to delete ingress {}/{}: {}", namespace, name, e.getMessage());
            throw KubernetesOperationException.from("Failed to delete Ingress", e);
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
