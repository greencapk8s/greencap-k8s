package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.greencap.k8s.kubernetes.dto.BuildRequest;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistryServiceTest {

    private final RegistryService registryService = new RegistryService(null, null);

    @Test
    void buildGitContextAddsGitSuffixAndDefaultsBranch() {
        String context = registryService.buildGitContext("https://github.com/usuario/repo", null);

        assertThat(context).isEqualTo("git://github.com/usuario/repo.git#refs/heads/main");
    }

    @Test
    void buildGitContextUsesGivenBranchAndKeepsExistingGitSuffix() {
        String context = registryService.buildGitContext("https://github.com/usuario/repo.git/", "develop");

        assertThat(context).isEqualTo("git://github.com/usuario/repo.git#refs/heads/develop");
    }

    @Test
    void buildDestinationCombinesInternalHostRepositoryAndTag() {
        String destination = registryService.buildDestination("meu-grupo/minha-app", "latest");

        assertThat(destination).isEqualTo("registry.kube-system.svc.cluster.local:80/meu-grupo/minha-app:latest");
    }

    @Test
    void resolveDockerfilePathDefaultsWhenBlank() {
        assertThat(registryService.resolveDockerfilePath(null)).isEqualTo("Dockerfile");
        assertThat(registryService.resolveDockerfilePath("  ")).isEqualTo("Dockerfile");
    }

    @Test
    void resolveDockerfilePathTrimsGivenValue() {
        assertThat(registryService.resolveDockerfilePath(" docker/Dockerfile.prod ")).isEqualTo("docker/Dockerfile.prod");
    }

    @Test
    void resolveContextSubPathIsEmptyWhenBlank() {
        assertThat(registryService.resolveContextSubPath(null)).isEmpty();
        assertThat(registryService.resolveContextSubPath("  ")).isEmpty();
        assertThat(registryService.resolveContextSubPath("/")).isEmpty();
    }

    @Test
    void resolveContextSubPathTrimsSlashesAndWhitespace() {
        assertThat(registryService.resolveContextSubPath(" /backend/ ")).isEqualTo(Optional.of("backend"));
    }

    @Test
    void gitSourceBuildsJobWithoutInitContainerOrVolumes() {
        PodSpec podSpec = podSpecOf(registryService.buildKanikoJob("job-1", BuildRequest.fromGitRepository(
                "https://github.com/usuario/repo", "develop", "backend", "docker/Dockerfile", "grupo/app", "latest")));

        assertThat(podSpec.getInitContainers()).isEmpty();
        assertThat(podSpec.getVolumes()).isEmpty();

        Container kaniko = podSpec.getContainers().get(0);
        assertThat(kaniko.getVolumeMounts()).isEmpty();
        assertThat(kaniko.getArgs()).contains(
                "--context=git://github.com/usuario/repo.git#refs/heads/develop",
                "--dockerfile=docker/Dockerfile",
                "--destination=registry.kube-system.svc.cluster.local:80/grupo/app:latest",
                "--context-sub-path=backend");
    }

    @Test
    void localFolderSourceBuildsJobWithInitContainerSharingTheContextVolume() {
        PodSpec podSpec = podSpecOf(registryService.buildKanikoJob("job-2", BuildRequest.fromLocalFolder(
                Path.of("/tmp/context.tar.gz"), "minha-app", "", "", "grupo/app", "latest")));

        assertThat(podSpec.getVolumes()).singleElement()
                .satisfies(volume -> assertThat(volume.getEmptyDir()).isNotNull());
        String contextVolume = podSpec.getVolumes().get(0).getName();

        Container initContainer = podSpec.getInitContainers().get(0);
        assertThat(initContainer.getName()).isEqualTo("context-init");
        assertThat(mountedVolumeNames(initContainer)).containsExactly(contextVolume);

        Container kaniko = podSpec.getContainers().get(0);
        assertThat(mountedVolumeNames(kaniko)).containsExactly(contextVolume);
        assertThat(kaniko.getArgs()).contains("--context=tar:///workspace/context.tar.gz");
        assertThat(kaniko.getArgs()).noneMatch(arg -> arg.startsWith("--context=git://"));
    }

    @Test
    void localFolderInitContainerWaitsForTheSentinelWrittenAfterTheUpload() {
        PodSpec podSpec = podSpecOf(registryService.buildKanikoJob("job-3", BuildRequest.fromLocalFolder(
                Path.of("/tmp/context.tar.gz"), "minha-app", "", "", "grupo/app", "latest")));

        String initScript = String.join(" ", podSpec.getInitContainers().get(0).getCommand());

        assertThat(initScript).contains("/workspace/.context-ready");
        assertThat(initScript).contains("exit 1");
    }

    @Test
    void malformedBuildRequestIsRejectedAtConstruction() {
        assertThatThrownBy(() -> new BuildRequest(null, "", "", "grupo/app", "latest"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BuildRequest.fromGitRepository("https://github.com/u/r", "main", "", "", " ", "latest"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BuildRequest.fromGitRepository(" ", "main", "", "", "grupo/app", "latest"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BuildRequest.fromLocalFolder(null, "minha-app", "", "", "grupo/app", "latest"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private PodSpec podSpecOf(Job job) {
        return job.getSpec().getTemplate().getSpec();
    }

    private List<String> mountedVolumeNames(Container container) {
        return container.getVolumeMounts().stream().map(mount -> mount.getName()).toList();
    }
}
