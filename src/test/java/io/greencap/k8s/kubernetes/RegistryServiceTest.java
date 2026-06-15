package io.greencap.k8s.kubernetes;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RegistryServiceTest {

    private final RegistryService registryService = new RegistryService(null, null, null);

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
}
