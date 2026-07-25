package io.greencap.k8s.kubernetes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientTimeoutException;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.BuildContextSource;
import io.greencap.k8s.kubernetes.dto.BuildProgress;
import io.greencap.k8s.kubernetes.dto.BuildRequest;
import io.greencap.k8s.kubernetes.dto.RepositoryInfo;
import io.greencap.k8s.kubernetes.dto.TagInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistryService {

    private static final String REGISTRY_NAMESPACE = "kube-system";
    private static final String REGISTRY_SERVICE_NAME = "registry";
    // Fabric8's ServiceResource#portForward forwards directly to this port on the matching Pod
    // (it does not resolve the Service's targetPort) — 5000 is the registry container's listening port.
    private static final int REGISTRY_CONTAINER_PORT = 5000;
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);

    // Build (Kaniko) — runs as a Job inside the cluster, so it reaches the Registry via cluster-internal
    // DNS directly, unlike the read path above which needs a port-forward from outside the cluster.
    // Port 80 is the Service port (mapped to the registry container's 5000), not 5000 itself —
    // the Service does not expose port 5000 directly.
    private static final String BUILD_NAMESPACE = "greencap-system";
    private static final String REGISTRY_INTERNAL_HOST = "registry.kube-system.svc.cluster.local:80";
    private static final String KANIKO_IMAGE = "gcr.io/kaniko-project/executor:v1.23.2";
    private static final String KANIKO_CONTAINER = "kaniko";
    private static final long BUILD_JOB_TTL_SECONDS = 600;
    private static final String DEFAULT_BRANCH = "main";
    private static final String DEFAULT_DOCKERFILE_PATH = "Dockerfile";

    // Local Build Context — the archive packed in the browser reaches the Kaniko container through an
    // initContainer that shares an emptyDir with it and blocks until the sentinel file appears (ADR 0020).
    private static final String CONTEXT_INIT_CONTAINER = "context-init";
    private static final String CONTEXT_INIT_IMAGE = "busybox:1.36";
    private static final String CONTEXT_VOLUME = "build-context";
    private static final String CONTEXT_MOUNT_PATH = "/workspace";
    private static final String CONTEXT_ARCHIVE_PATH = CONTEXT_MOUNT_PATH + "/context.tar.gz";
    private static final String CONTEXT_SENTINEL_PATH = CONTEXT_MOUNT_PATH + "/.context-ready";
    private static final long CONTEXT_INIT_MAX_WAIT_SECONDS = 300;
    private static final Duration BUILD_POD_START_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration CONTEXT_EXEC_TIMEOUT = Duration.ofSeconds(60);
    private static final long POD_LOOKUP_INTERVAL_MILLIS = 500;

    private final KubernetesClientFactory clientFactory;
    private final ObjectMapper objectMapper;

    public List<RepositoryInfo> listRepositories(Cluster cluster) {
        try (KubernetesClient client = clientFactory.buildClient(cluster);
             LocalPortForward portForward = client.services()
                     .inNamespace(REGISTRY_NAMESPACE)
                     .withName(REGISTRY_SERVICE_NAME)
                     .portForward(REGISTRY_CONTAINER_PORT)) {

            HttpClient httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
            String baseUrl = "http://localhost:" + portForward.getLocalPort();

            return fetchCatalog(httpClient, baseUrl).stream()
                    .map(name -> new RepositoryInfo(name, fetchTags(httpClient, baseUrl, name).size()))
                    .toList();
        } catch (Exception e) {
            // Absence of the registry Service (addon not enabled) is an expected state, not a cluster
            // operation failure — return an empty catalog instead of KubernetesOperationException.
            log.warn("Registry not available for cluster {}: {}", cluster.getName(), e.getMessage());
            return List.of();
        }
    }

    public List<TagInfo> listTags(Cluster cluster, String repository) {
        try (KubernetesClient client = clientFactory.buildClient(cluster);
             LocalPortForward portForward = client.services()
                     .inNamespace(REGISTRY_NAMESPACE)
                     .withName(REGISTRY_SERVICE_NAME)
                     .portForward(REGISTRY_CONTAINER_PORT)) {

            HttpClient httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
            String baseUrl = "http://localhost:" + portForward.getLocalPort();

            return fetchTags(httpClient, baseUrl, repository).stream()
                    .map(tag -> fetchTagInfo(httpClient, baseUrl, repository, tag))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to list tags for repository {} on cluster {}: {}", repository, cluster.getName(), e.getMessage());
            return List.of();
        }
    }

    public String startBuild(Cluster cluster, BuildRequest request) {
        return startBuild(cluster, request, phase -> { });
    }

    /**
     * @param progressListener receives the phases a local Build Context goes through before Kaniko
     *                         produces its first log line — the caller has nothing else to show meanwhile.
     */
    public String startBuild(Cluster cluster, BuildRequest request, Consumer<String> progressListener) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {

            ensureBuildNamespaceExists(client);

            String jobName = "kaniko-build-" + (System.currentTimeMillis() / 1000);
            Job job = buildKanikoJob(jobName, request);

            client.batch().v1().jobs().inNamespace(BUILD_NAMESPACE).resource(job).create();

            if (request.source() instanceof BuildContextSource.LocalFolder localFolder) {
                injectLocalContext(client, jobName, localFolder, progressListener);
            }

            log.info("Started build job {} for cluster {}: {} -> {}", jobName, cluster.getName(),
                    describeSource(request.source()), buildDestination(request.repository(), request.tag()));
            return jobName;
        } catch (KubernetesOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to start build for cluster {}: {}", cluster.getName(), e.getMessage());
            throw KubernetesOperationException.from("Failed to start Build", e);
        }
    }

    Job buildKanikoJob(String jobName, BuildRequest request) {
        boolean hasLocalContext = request.source() instanceof BuildContextSource.LocalFolder;

        List<String> args = new ArrayList<>(List.of(
                "--dockerfile=" + resolveDockerfilePath(request.dockerfilePath()),
                "--context=" + resolveKanikoContext(request.source()),
                "--destination=" + buildDestination(request.repository(), request.tag()),
                "--insecure"));
        resolveContextSubPath(request.contextPath())
                .ifPresent(contextSubPath -> args.add("--context-sub-path=" + contextSubPath));

        Container kaniko = new ContainerBuilder()
                .withName(KANIKO_CONTAINER)
                .withImage(KANIKO_IMAGE)
                .withArgs(args)
                .withVolumeMounts(hasLocalContext ? List.of(contextVolumeMount()) : List.<VolumeMount>of())
                .withResources(containerResources("250m", "256Mi", "1", "1Gi"))
                .build();

        PodSpecBuilder podSpec = new PodSpecBuilder()
                .withRestartPolicy("Never")
                .withContainers(kaniko);

        if (hasLocalContext) {
            podSpec.withInitContainers(contextInitContainer())
                    .withVolumes(new VolumeBuilder()
                            .withName(CONTEXT_VOLUME)
                            .withNewEmptyDir()
                            .endEmptyDir()
                            .build());
        }

        return new JobBuilder()
                .withNewMetadata()
                    .withName(jobName)
                    .withNamespace(BUILD_NAMESPACE)
                .endMetadata()
                .withNewSpec()
                    .withBackoffLimit(0)
                    .withTtlSecondsAfterFinished((int) BUILD_JOB_TTL_SECONDS)
                    .withNewTemplate()
                        .withSpec(podSpec.build())
                    .endTemplate()
                .endSpec()
                .build();
    }

    private String resolveKanikoContext(BuildContextSource source) {
        return switch (source) {
            case BuildContextSource.GitRepository git -> buildGitContext(git.url(), git.branch());
            case BuildContextSource.LocalFolder ignored -> "tar://" + CONTEXT_ARCHIVE_PATH;
        };
    }

    private String describeSource(BuildContextSource source) {
        return switch (source) {
            case BuildContextSource.GitRepository git -> buildGitContext(git.url(), git.branch());
            case BuildContextSource.LocalFolder local -> "local folder " + local.folderName();
        };
    }

    private Container contextInitContainer() {
        return new ContainerBuilder()
                .withName(CONTEXT_INIT_CONTAINER)
                .withImage(CONTEXT_INIT_IMAGE)
                .withCommand("sh", "-c", waitForContextScript())
                .withVolumeMounts(contextVolumeMount())
                .withResources(containerResources("50m", "32Mi", "200m", "64Mi"))
                .build();
    }

    // The build context is pushed into this container through the exec API while it blocks here; the
    // bounded wait keeps the Pod from sitting idle until the Job TTL if the upload never arrives.
    private String waitForContextScript() {
        return "waited=0; while [ ! -f " + CONTEXT_SENTINEL_PATH + " ]; do "
                + "waited=$((waited+1)); "
                + "if [ $waited -gt " + CONTEXT_INIT_MAX_WAIT_SECONDS + " ]; then "
                + "echo 'Timed out waiting for the build context upload'; exit 1; fi; "
                + "sleep 1; done";
    }

    private VolumeMount contextVolumeMount() {
        return new VolumeMountBuilder()
                .withName(CONTEXT_VOLUME)
                .withMountPath(CONTEXT_MOUNT_PATH)
                .build();
    }

    private ResourceRequirements containerResources(String cpuRequest, String memoryRequest,
                                                    String cpuLimit, String memoryLimit) {
        return new ResourceRequirementsBuilder()
                .addToRequests("cpu", new Quantity(cpuRequest))
                .addToRequests("memory", new Quantity(memoryRequest))
                .addToLimits("cpu", new Quantity(cpuLimit))
                .addToLimits("memory", new Quantity(memoryLimit))
                .build();
    }

    private void injectLocalContext(KubernetesClient client, String jobName,
                                    BuildContextSource.LocalFolder localFolder, Consumer<String> progressListener) {
        try {
            progressListener.accept("Waiting for the Build Pod to start...");
            PodResource pod = awaitContextInitContainer(client, jobName);

            progressListener.accept("Uploading the build context (" + describeArchiveSize(localFolder) + ")...");
            uploadContextArchive(pod, localFolder);

            progressListener.accept("Build context uploaded. Starting Kaniko...");
            releaseContextInitContainer(pod);
            log.info("Injected local build context ({}) into job {}", localFolder.folderName(), jobName);
        } catch (RuntimeException e) {
            // Leaving the Job alive would keep the initContainer blocked until its own timeout,
            // showing the user a Pod that never builds anything.
            deleteJobQuietly(client, jobName);
            throw e;
        }
    }

    private PodResource awaitContextInitContainer(KubernetesClient client, String jobName) {
        long deadline = System.currentTimeMillis() + BUILD_POD_START_TIMEOUT.toMillis();
        while (System.currentTimeMillis() < deadline) {
            Optional<String> podName = findBuildPodName(client, jobName);
            if (podName.isPresent()) {
                PodResource pod = client.pods().inNamespace(BUILD_NAMESPACE).withName(podName.get());
                awaitInitContainerRunning(pod, deadline - System.currentTimeMillis());
                return pod;
            }
            sleepQuietly(POD_LOOKUP_INTERVAL_MILLIS);
        }
        throw buildPodStartTimeout(null);
    }

    private void awaitInitContainerRunning(PodResource pod, long remainingMillis) {
        try {
            pod.waitUntilCondition(RegistryService::isContextInitContainerRunning,
                    Math.max(remainingMillis, 0), TimeUnit.MILLISECONDS);
        } catch (KubernetesClientTimeoutException e) {
            throw buildPodStartTimeout(e);
        }
    }

    private KubernetesOperationException buildPodStartTimeout(Exception cause) {
        return new KubernetesOperationException(
                "The Build Pod did not start within " + BUILD_POD_START_TIMEOUT.toSeconds()
                        + "s — the cluster may not have resources to schedule it, or the image could not be pulled.",
                cause);
    }

    private static boolean isContextInitContainerRunning(Pod pod) {
        if (pod == null || pod.getStatus() == null || pod.getStatus().getInitContainerStatuses() == null) {
            return false;
        }
        return pod.getStatus().getInitContainerStatuses().stream()
                .filter(status -> CONTEXT_INIT_CONTAINER.equals(status.getName()))
                .anyMatch(status -> status.getState() != null && status.getState().getRunning() != null);
    }

    private void uploadContextArchive(PodResource pod, BuildContextSource.LocalFolder localFolder) {
        try (InputStream archive = Files.newInputStream(localFolder.archive())) {
            boolean uploaded = pod.inContainer(CONTEXT_INIT_CONTAINER)
                    .file(CONTEXT_ARCHIVE_PATH)
                    .upload(archive);
            if (!uploaded) {
                throw new KubernetesOperationException(
                        "Failed to upload the build context into the Build Pod — the transfer did not complete.");
            }
        } catch (IOException e) {
            throw new KubernetesOperationException("Failed to read the packed build context: " + e.getMessage(), e);
        } catch (KubernetesOperationException e) {
            throw e;
        } catch (Exception e) {
            throw KubernetesOperationException.from("Failed to upload the build context into the Build Pod", e);
        }
    }

    private void releaseContextInitContainer(PodResource pod) {
        ByteArrayOutputStream execOutput = new ByteArrayOutputStream();
        try (ExecWatch exec = pod.inContainer(CONTEXT_INIT_CONTAINER)
                .writingOutput(execOutput)
                .writingError(execOutput)
                .exec("sh", "-c", "touch " + CONTEXT_SENTINEL_PATH)) {

            Integer exitCode = exec.exitCode().get(CONTEXT_EXEC_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            if (exitCode == null || exitCode != 0) {
                throw new KubernetesOperationException("Failed to release the Build Pod: "
                        + execOutput.toString(StandardCharsets.UTF_8));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KubernetesOperationException("Interrupted while releasing the Build Pod", e);
        } catch (KubernetesOperationException e) {
            throw e;
        } catch (Exception e) {
            throw KubernetesOperationException.from("Failed to release the Build Pod", e);
        }
    }

    private String describeArchiveSize(BuildContextSource.LocalFolder localFolder) {
        try {
            return formatSize(Files.size(localFolder.archive()));
        } catch (IOException e) {
            return "unknown size";
        }
    }

    private Optional<String> findBuildPodName(KubernetesClient client, String jobName) {
        return client.pods().inNamespace(BUILD_NAMESPACE)
                .withLabel("job-name", jobName)
                .list().getItems().stream()
                .findFirst()
                .map(pod -> pod.getMetadata().getName());
    }

    private void deleteJobQuietly(KubernetesClient client, String jobName) {
        try {
            client.batch().v1().jobs().inNamespace(BUILD_NAMESPACE).withName(jobName).delete();
        } catch (Exception e) {
            log.warn("Could not delete build job {} after a failed context injection: {}", jobName, e.getMessage());
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KubernetesOperationException("Interrupted while waiting for the Build Pod to start", e);
        }
    }

    public BuildProgress getBuildProgress(Cluster cluster, String jobName) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {

            Job job = client.batch().v1().jobs().inNamespace(BUILD_NAMESPACE).withName(jobName).get();
            if (job == null) {
                throw new KubernetesOperationException("Build job not found: " + jobName, null);
            }

            return new BuildProgress(findBuildPodName(client, jobName).orElse(null), deriveBuildStatus(job));
        } catch (KubernetesOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get build progress for job {}: {}", jobName, e.getMessage());
            throw KubernetesOperationException.from("Failed to get Build progress", e);
        }
    }

    private void ensureBuildNamespaceExists(KubernetesClient client) {
        if (client.namespaces().withName(BUILD_NAMESPACE).get() == null) {
            client.namespaces().resource(new NamespaceBuilder()
                            .withNewMetadata()
                                .withName(BUILD_NAMESPACE)
                            .endMetadata()
                            .build())
                    .create();
            log.info("Created namespace {}", BUILD_NAMESPACE);
        }
    }

    private String deriveBuildStatus(Job job) {
        var conditions = Optional.ofNullable(job.getStatus())
                .map(status -> status.getConditions()).orElse(List.of());
        for (var condition : conditions) {
            if ("Complete".equals(condition.getType()) && "True".equals(condition.getStatus())) return "Complete";
            if ("Failed".equals(condition.getType()) && "True".equals(condition.getStatus())) return "Failed";
        }
        return "Running";
    }

    String buildGitContext(String repositoryUrl, String branch) {
        String url = repositoryUrl.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (!url.endsWith(".git")) {
            url = url + ".git";
        }
        // Kaniko's "git://" prefix is followed by the URL without its own scheme;
        // Kaniko re-adds "https://" (or "http://" via GIT_PULL_METHOD) when cloning.
        url = url.replaceFirst("^https?://", "");
        String ref = (branch == null || branch.isBlank()) ? DEFAULT_BRANCH : branch.trim();
        return "git://" + url + "#refs/heads/" + ref;
    }

    String buildDestination(String repository, String tag) {
        return REGISTRY_INTERNAL_HOST + "/" + repository.trim() + ":" + tag.trim();
    }

    String resolveDockerfilePath(String dockerfilePath) {
        return (dockerfilePath == null || dockerfilePath.isBlank()) ? DEFAULT_DOCKERFILE_PATH : dockerfilePath.trim();
    }

    Optional<String> resolveContextSubPath(String contextPath) {
        if (contextPath == null || contextPath.isBlank()) {
            return Optional.empty();
        }
        String trimmed = contextPath.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
    }

    private TagInfo fetchTagInfo(HttpClient httpClient, String baseUrl, String repository, String tag) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v2/" + repository + "/manifests/" + tag))
                    .timeout(HTTP_TIMEOUT)
                    // Kaniko pushes OCI manifests; older images pushed via Docker use the Docker v2 schema —
                    // both have the same "config"/"layers" shape, so ManifestResponse handles either.
                    .header("Accept", "application/vnd.docker.distribution.manifest.v2+json, application/vnd.oci.image.manifest.v1+json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Unexpected status " + response.statusCode() + " for manifest " + repository + ":" + tag);
            }
            String digest = response.headers().firstValue("Docker-Content-Digest").orElse("-");
            ManifestResponse manifest = objectMapper.readValue(response.body(), ManifestResponse.class);
            long totalSize = manifest.config().size()
                    + manifest.layers().stream().mapToLong(LayerInfo::size).sum();

            ConfigBlob configBlob = get(httpClient, baseUrl + "/v2/" + repository + "/blobs/" + manifest.config().digest(), ConfigBlob.class);

            return new TagInfo(tag, digest, formatSize(totalSize), NamespaceService.age(configBlob.created()));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch details for tag " + repository + ":" + tag, e);
        }
    }

    private String formatSize(long bytes) {
        double kilobytes = bytes / 1024.0;
        if (kilobytes < 1024) {
            return String.format("%.1f KB", kilobytes);
        }
        double megabytes = kilobytes / 1024.0;
        if (megabytes < 1024) {
            return String.format("%.1f MB", megabytes);
        }
        return String.format("%.1f GB", megabytes / 1024.0);
    }

    private List<String> fetchCatalog(HttpClient httpClient, String baseUrl) throws Exception {
        CatalogResponse response = get(httpClient, baseUrl + "/v2/_catalog", CatalogResponse.class);
        return response.repositories() != null ? response.repositories() : List.of();
    }

    private List<String> fetchTags(HttpClient httpClient, String baseUrl, String repository) {
        try {
            TagsResponse response = get(httpClient, baseUrl + "/v2/" + repository + "/tags/list", TagsResponse.class);
            return response.tags() != null ? response.tags() : List.of();
        } catch (Exception e) {
            log.warn("Failed to list tags for repository {}: {}", repository, e.getMessage());
            return List.of();
        }
    }

    private <T> T get(HttpClient httpClient, String url, Class<T> type) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Unexpected status " + response.statusCode() + " from " + url);
        }
        return objectMapper.readValue(response.body(), type);
    }

    private record CatalogResponse(List<String> repositories) {
    }

    private record TagsResponse(String name, List<String> tags) {
    }

    private record ManifestResponse(ConfigRef config, List<LayerInfo> layers) {
    }

    private record ConfigRef(String digest, long size) {
    }

    private record LayerInfo(long size) {
    }

    private record ConfigBlob(String created) {
    }
}
