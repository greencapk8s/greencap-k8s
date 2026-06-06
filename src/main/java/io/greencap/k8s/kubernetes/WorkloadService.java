package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.CronJobInfo;
import io.greencap.k8s.kubernetes.dto.DeploymentInfo;
import io.greencap.k8s.kubernetes.dto.JobInfo;
import io.greencap.k8s.kubernetes.dto.PodInfo;
import io.greencap.k8s.kubernetes.dto.ReplicaSetInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkloadService {

    private final KubernetesClientFactory clientFactory;
    private final EncryptionService encryptionService;

    public List<PodInfo> listPods(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            var items = isAllNamespaces(namespace)
                    ? client.pods().inAnyNamespace().list().getItems()
                    : client.pods().inNamespace(namespace).list().getItems();

            return items.stream()
                    .map(pod -> new PodInfo(
                            pod.getMetadata().getName(),
                            pod.getMetadata().getNamespace(),
                            Optional.ofNullable(pod.getStatus()).map(s -> s.getPhase()).orElse("Unknown"),
                            Optional.ofNullable(pod.getSpec()).map(s -> s.getNodeName()).orElse("-"),
                            Optional.ofNullable(pod.getStatus())
                                    .map(s -> s.getContainerStatuses())
                                    .map(cs -> cs.stream()
                                            .mapToInt(c -> c.getRestartCount() != null ? c.getRestartCount() : 0)
                                            .sum())
                                    .orElse(0),
                            NamespaceService.age(pod.getMetadata().getCreationTimestamp())
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list pods for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list pods: " + e.getMessage(), e);
        }
    }

    public List<DeploymentInfo> listDeployments(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            var items = isAllNamespaces(namespace)
                    ? client.apps().deployments().inAnyNamespace().list().getItems()
                    : client.apps().deployments().inNamespace(namespace).list().getItems();

            return items.stream()
                    .map(d -> new DeploymentInfo(
                            d.getMetadata().getName(),
                            d.getMetadata().getNamespace(),
                            Optional.ofNullable(d.getSpec()).map(s -> s.getReplicas()).orElse(0),
                            Optional.ofNullable(d.getStatus()).map(s -> s.getReadyReplicas()).orElse(0),
                            Optional.ofNullable(d.getStatus()).map(s -> s.getAvailableReplicas()).orElse(0),
                            NamespaceService.age(d.getMetadata().getCreationTimestamp())
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list deployments for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list deployments: " + e.getMessage(), e);
        }
    }

    public List<ReplicaSetInfo> listReplicaSets(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            var items = isAllNamespaces(namespace)
                    ? client.apps().replicaSets().inAnyNamespace().list().getItems()
                    : client.apps().replicaSets().inNamespace(namespace).list().getItems();

            Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);

            return items.stream()
                    .filter(rs -> {
                        int desired = Optional.ofNullable(rs.getSpec()).map(s -> s.getReplicas()).orElse(0);
                        if (desired > 0) return true;
                        String ts = rs.getMetadata().getCreationTimestamp();
                        return ts != null && Instant.parse(ts).isAfter(oneDayAgo);
                    })
                    .map(rs -> {
                        String owner = Optional.ofNullable(rs.getMetadata().getOwnerReferences())
                                .flatMap(refs -> refs.stream()
                                        .filter(ref -> "Deployment".equals(ref.getKind()))
                                        .findFirst())
                                .map(ref -> ref.getName())
                                .orElse("—");

                        int desired = Optional.ofNullable(rs.getSpec())
                                .map(s -> s.getReplicas())
                                .orElse(0);

                        int ready = Optional.ofNullable(rs.getStatus())
                                .map(s -> s.getReadyReplicas())
                                .orElse(0);

                        return new ReplicaSetInfo(
                                rs.getMetadata().getName(),
                                rs.getMetadata().getNamespace(),
                                owner,
                                desired,
                                ready,
                                NamespaceService.age(rs.getMetadata().getCreationTimestamp())
                        );
                    })
                    .sorted(Comparator.comparingInt((ReplicaSetInfo rs) -> rs.desired() > 0 ? 0 : 1))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list replicasets for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list replicasets: " + e.getMessage(), e);
        }
    }

    public void scaleDeployment(Cluster cluster, String namespace, String name, int replicas) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            client.apps().deployments().inNamespace(namespace).withName(name).scale(replicas);
            log.info("Scaled deployment {}/{} to {} replicas", namespace, name, replicas);
        } catch (Exception e) {
            log.error("Failed to scale deployment {}/{}: {}", namespace, name, e.getMessage());
            throw new KubernetesOperationException("Failed to scale deployment: " + e.getMessage(), e);
        }
    }

    public void restartDeployment(Cluster cluster, String namespace, String name) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            client.apps().deployments().inNamespace(namespace).withName(name).rolling().restart();
            log.info("Restarted deployment {}/{}", namespace, name);
        } catch (Exception e) {
            log.error("Failed to restart deployment {}/{}: {}", namespace, name, e.getMessage());
            throw new KubernetesOperationException("Failed to restart deployment: " + e.getMessage(), e);
        }
    }

    public void rolloutUndoDeployment(Cluster cluster, String namespace, String name) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            client.apps().deployments().inNamespace(namespace).withName(name).rolling().undo();
            log.info("Rolled back deployment {}/{}", namespace, name);
        } catch (Exception e) {
            log.error("Failed to roll back deployment {}/{}: {}", namespace, name, e.getMessage());
            throw new KubernetesOperationException("Failed to roll back deployment: " + e.getMessage(), e);
        }
    }

    public List<JobInfo> listJobs(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            var items = isAllNamespaces(namespace)
                    ? client.batch().v1().jobs().inAnyNamespace().list().getItems()
                    : client.batch().v1().jobs().inNamespace(namespace).list().getItems();

            return items.stream()
                    .map(job -> {
                        int desired = Optional.ofNullable(job.getSpec())
                                .map(s -> s.getCompletions()).orElse(1);
                        int succeeded = Optional.ofNullable(job.getStatus())
                                .map(s -> s.getSucceeded()).orElse(0);

                        String status = deriveJobStatus(job);
                        String completions = succeeded + "/" + desired;
                        String duration = deriveJobDuration(job);

                        String owner = Optional.ofNullable(job.getMetadata().getOwnerReferences())
                                .flatMap(refs -> refs.stream()
                                        .filter(ref -> "CronJob".equals(ref.getKind()))
                                        .findFirst())
                                .map(ref -> ref.getName())
                                .orElse("—");

                        return new JobInfo(
                                job.getMetadata().getName(),
                                job.getMetadata().getNamespace(),
                                status,
                                completions,
                                duration,
                                NamespaceService.age(job.getMetadata().getCreationTimestamp()),
                                owner
                        );
                    })
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list jobs for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list jobs: " + e.getMessage(), e);
        }
    }

    public List<CronJobInfo> listCronJobs(Cluster cluster, String namespace) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {
            var items = isAllNamespaces(namespace)
                    ? client.batch().v1().cronjobs().inAnyNamespace().list().getItems()
                    : client.batch().v1().cronjobs().inNamespace(namespace).list().getItems();

            return items.stream()
                    .map(cj -> {
                        boolean suspended = Optional.ofNullable(cj.getSpec())
                                .map(s -> s.getSuspend()).orElse(false);
                        int active = Optional.ofNullable(cj.getStatus())
                                .map(s -> s.getActive()).map(List::size).orElse(0);
                        String lastSchedule = Optional.ofNullable(cj.getStatus())
                                .map(s -> s.getLastScheduleTime())
                                .map(NamespaceService::age)
                                .orElse("—");

                        return new CronJobInfo(
                                cj.getMetadata().getName(),
                                cj.getMetadata().getNamespace(),
                                Optional.ofNullable(cj.getSpec()).map(s -> s.getSchedule()).orElse("—"),
                                suspended,
                                active,
                                lastSchedule,
                                NamespaceService.age(cj.getMetadata().getCreationTimestamp())
                        );
                    })
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list cronjobs for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to list cronjobs: " + e.getMessage(), e);
        }
    }

    private String deriveJobStatus(io.fabric8.kubernetes.api.model.batch.v1.Job job) {
        if (Boolean.TRUE.equals(Optional.ofNullable(job.getSpec()).map(s -> s.getSuspend()).orElse(false))) {
            return "Suspended";
        }
        var conditions = Optional.ofNullable(job.getStatus())
                .map(s -> s.getConditions()).orElse(List.of());
        for (var condition : conditions) {
            if ("Complete".equals(condition.getType()) && "True".equals(condition.getStatus())) return "Complete";
            if ("Failed".equals(condition.getType()) && "True".equals(condition.getStatus())) return "Failed";
        }
        return "Running";
    }

    private String deriveJobDuration(io.fabric8.kubernetes.api.model.batch.v1.Job job) {
        if (job.getStatus() == null) return "—";
        String startTime = job.getStatus().getStartTime();
        String completionTime = job.getStatus().getCompletionTime();
        if (startTime == null) return "—";
        try {
            Instant start = Instant.parse(startTime);
            Instant end = completionTime != null ? Instant.parse(completionTime) : Instant.now();
            return formatDuration(Duration.between(start, end));
        } catch (Exception e) {
            return "—";
        }
    }

    private String formatDuration(Duration d) {
        if (d.toDays() > 0)    return d.toDays() + "d " + (d.toHoursPart()) + "h";
        if (d.toHours() > 0)   return d.toHours() + "h " + d.toMinutesPart() + "m";
        if (d.toMinutes() > 0) return d.toMinutes() + "m " + d.toSecondsPart() + "s";
        return d.toSeconds() + "s";
    }

    private boolean isAllNamespaces(String namespace) {
        return namespace == null || namespace.isBlank() || "all".equalsIgnoreCase(namespace);
    }
}
