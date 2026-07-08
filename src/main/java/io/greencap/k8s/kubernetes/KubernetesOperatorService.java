package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.operatorhub.lifecyclemanager.v1.PackageManifest;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroup;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroupBuilder;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.ClusterServiceVersion;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.SubscriptionBuilder;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.OperatorChannel;
import io.greencap.k8s.kubernetes.dto.OperatorInfo;
import io.greencap.k8s.kubernetes.dto.OperatorPackage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KubernetesOperatorService {

    private static final String OPERATORS_NAMESPACE = "operators";
    private static final String OLM_NAMESPACE = "olm";

    private final KubernetesClientFactory clientFactory;

    public boolean isOlmInstalled(Cluster cluster) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {
            client.resources(ClusterServiceVersion.class).inNamespace(OPERATORS_NAMESPACE).list();
            return true;
        } catch (Exception e) {
            log.debug("OLM not detected in cluster {}: {}", cluster.getName(), e.getMessage());
            return false;
        }
    }

    public List<OperatorInfo> listInstalled(Cluster cluster) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {

            Map<String, ClusterServiceVersion> csvByName = client.resources(ClusterServiceVersion.class)
                    .inNamespace(OPERATORS_NAMESPACE)
                    .list()
                    .getItems()
                    .stream()
                    .collect(Collectors.toMap(csv -> csv.getMetadata().getName(), csv -> csv));

            return client.resources(Subscription.class)
                    .inNamespace(OPERATORS_NAMESPACE)
                    .list()
                    .getItems()
                    .stream()
                    .map(sub -> toOperatorInfo(sub, csvByName))
                    .sorted(Comparator.comparing(OperatorInfo::name))
                    .toList();

        } catch (Exception e) {
            log.error("Failed to list installed operators for cluster {}: {}", cluster.getName(), e.getMessage());
            throw KubernetesOperationException.from("Failed to list installed operators", e);
        }
    }

    public List<OperatorPackage> listCatalog(Cluster cluster) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {

            return client.resources(PackageManifest.class)
                    .inAnyNamespace()
                    .list()
                    .getItems()
                    .stream()
                    .map(this::toOperatorPackage)
                    .sorted(Comparator.comparing(OperatorPackage::name))
                    .toList();

        } catch (Exception e) {
            log.error("Failed to list operator catalog for cluster {}: {}", cluster.getName(), e.getMessage());
            throw KubernetesOperationException.from("Failed to list operator catalog", e);
        }
    }

    public void install(Cluster cluster, String packageName, String channel, String catalogSource) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {

            ensureOperatorGroup(client);

            Subscription subscription = new SubscriptionBuilder()
                    .withNewMetadata()
                        .withName(packageName)
                        .withNamespace(OPERATORS_NAMESPACE)
                    .endMetadata()
                    .withNewSpec()
                        .withName(packageName)
                        .withSource(catalogSource)
                        .withSourceNamespace(OLM_NAMESPACE)
                        .withChannel(channel)
                        .withInstallPlanApproval("Automatic")
                    .endSpec()
                    .build();

            client.resources(Subscription.class)
                    .inNamespace(OPERATORS_NAMESPACE)
                    .resource(subscription)
                    .create();

        } catch (KubernetesOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to install operator {} in cluster {}: {}", packageName, cluster.getName(), e.getMessage());
            throw KubernetesOperationException.from("Failed to install operator", e);
        }
    }

    public void uninstall(Cluster cluster, String packageName) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {

            client.resources(Subscription.class)
                    .inNamespace(OPERATORS_NAMESPACE)
                    .list()
                    .getItems()
                    .stream()
                    .filter(sub -> packageName.equals(Optional.ofNullable(sub.getSpec())
                            .map(s -> s.getName()).orElse("")))
                    .forEach(sub -> client.resources(Subscription.class)
                            .inNamespace(OPERATORS_NAMESPACE)
                            .withName(sub.getMetadata().getName())
                            .delete());

            client.resources(ClusterServiceVersion.class)
                    .inNamespace(OPERATORS_NAMESPACE)
                    .list()
                    .getItems()
                    .stream()
                    .filter(csv -> csv.getMetadata().getName().startsWith(packageName + "."))
                    .forEach(csv -> client.resources(ClusterServiceVersion.class)
                            .inNamespace(OPERATORS_NAMESPACE)
                            .withName(csv.getMetadata().getName())
                            .delete());

        } catch (KubernetesOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to uninstall operator {} from cluster {}: {}", packageName, cluster.getName(), e.getMessage());
            throw KubernetesOperationException.from("Failed to uninstall operator", e);
        }
    }

    private void ensureOperatorGroup(KubernetesClient client) {
        List<OperatorGroup> existing = client.resources(OperatorGroup.class)
                .inNamespace(OPERATORS_NAMESPACE)
                .list()
                .getItems();

        if (existing.isEmpty()) {
            OperatorGroup group = new OperatorGroupBuilder()
                    .withNewMetadata()
                        .withName("global-operators")
                        .withNamespace(OPERATORS_NAMESPACE)
                    .endMetadata()
                    .withNewSpec()
                        .withTargetNamespaces(List.of())
                    .endSpec()
                    .build();
            client.resources(OperatorGroup.class)
                    .inNamespace(OPERATORS_NAMESPACE)
                    .resource(group)
                    .create();
        }
    }

    private OperatorInfo toOperatorInfo(Subscription sub, Map<String, ClusterServiceVersion> csvByName) {
        String packageName = Optional.ofNullable(sub.getSpec()).map(s -> s.getName()).orElse(sub.getMetadata().getName());
        String channel = Optional.ofNullable(sub.getSpec()).map(s -> s.getChannel()).orElse("");
        String catalogSource = Optional.ofNullable(sub.getSpec()).map(s -> s.getSource()).orElse("");

        String currentCsvName = Optional.ofNullable(sub.getStatus()).map(s -> s.getCurrentCSV()).orElse(null);
        ClusterServiceVersion csv = currentCsvName != null ? csvByName.get(currentCsvName) : null;

        String displayName = Optional.ofNullable(csv)
                .map(c -> c.getSpec())
                .map(s -> s.getDisplayName())
                .orElse(packageName);
        String version = Optional.ofNullable(csv)
                .map(c -> c.getSpec())
                .map(s -> s.getVersion())
                .orElse("");
        String subState = Optional.ofNullable(sub.getStatus()).map(s -> s.getState()).orElse(null);
        String subReason = Optional.ofNullable(sub.getStatus()).map(s -> s.getReason()).orElse("");

        String phase;
        String statusMessage;

        if (csv != null) {
            phase = Optional.ofNullable(csv.getStatus()).map(s -> s.getPhase()).orElse("Installing");
            statusMessage = Optional.ofNullable(csv.getStatus()).map(s -> s.getMessage()).orElse("");
        } else if (isFailedState(subState)) {
            phase = "Failed";
            statusMessage = subReason.isBlank() ? "Installation failed — check Events in the operators namespace" : subReason;
        } else {
            phase = "Installing";
            statusMessage = "";
        }

        return new OperatorInfo(packageName, displayName, version, channel, catalogSource, phase, statusMessage);
    }

    private static boolean isFailedState(String state) {
        return state != null && Set.of("Failed", "ResolutionFailed", "UpgradeFailed").contains(state);
    }

    private OperatorPackage toOperatorPackage(PackageManifest manifest) {
        var status = manifest.getStatus();
        String name = manifest.getMetadata().getName();

        String displayName = Optional.ofNullable(status)
                .map(s -> s.getChannels())
                .filter(c -> !c.isEmpty())
                .map(c -> c.get(0).getCurrentCSVDesc())
                .map(d -> d.getDisplayName())
                .orElse(name);
        String provider = Optional.ofNullable(status)
                .map(s -> s.getProvider())
                .map(p -> p.getName())
                .orElse("");
        String catalogSource = Optional.ofNullable(status).map(s -> s.getCatalogSource()).orElse("");
        String defaultChannel = Optional.ofNullable(status).map(s -> s.getDefaultChannel()).orElse("");

        List<OperatorChannel> channels = Optional.ofNullable(status)
                .map(s -> s.getChannels())
                .orElse(List.of())
                .stream()
                .map(c -> new OperatorChannel(c.getName(), c.getCurrentCSV()))
                .toList();

        return new OperatorPackage(name, displayName, "", provider, catalogSource, channels, defaultChannel);
    }
}
