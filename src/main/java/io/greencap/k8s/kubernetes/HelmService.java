package io.greencap.k8s.kubernetes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.helm.HelmRepository;
import io.greencap.k8s.domain.helm.HelmRepositoryService;
import io.greencap.k8s.kubernetes.dto.HelmReleaseDetails;
import io.greencap.k8s.kubernetes.dto.HelmReleaseInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class HelmService {

    private static final String HELM_BINARY = "helm";
    private static final int TIMEOUT_SECONDS = 30;
    private static final int LIST_TIMEOUT_SECONDS = 8;
    private static final int INSTALL_TIMEOUT_SECONDS = 120;

    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;
    private final HelmRepositoryService helmRepositoryService;

    public List<HelmReleaseInfo> listReleases(Cluster cluster, String namespace) {
        Path kubeconfig = writeKubeconfig(cluster);
        try {
            String output = execWithTimeout(kubeconfig, LIST_TIMEOUT_SECONDS, "list", "--namespace", namespace, "--output", "json");
            return parseReleases(output);
        } catch (HelmOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to list Helm releases in {}: {}", namespace, e.getMessage());
            throw new HelmOperationException("Failed to list Helm releases: " + e.getMessage(), e);
        } finally {
            deleteQuietly(kubeconfig);
        }
    }

    public HelmReleaseDetails getReleaseDetails(Cluster cluster, String namespace, String name) {
        Path kubeconfig = writeKubeconfig(cluster);
        try {
            String notes    = execQuiet(kubeconfig, "get", "notes",    name, "--namespace", namespace);
            String rawValues = execQuiet(kubeconfig, "get", "values",  name, "--namespace", namespace);
            String values   = rawValues.replaceFirst("^USER-SUPPLIED VALUES:\\r?\\n?", "");
            String manifest = execQuiet(kubeconfig, "get", "manifest", name, "--namespace", namespace);
            return new HelmReleaseDetails(notes, values, manifest);
        } finally {
            deleteQuietly(kubeconfig);
        }
    }

    public String install(Cluster cluster, String namespace, String repoName, String chart,
                          String version, String releaseName, String values) {
        Path kubeconfig = writeKubeconfig(cluster);
        Path valuesFile = values != null && !values.isBlank() ? writeValuesFile(values) : null;
        try {
            ensureRepos(kubeconfig, helmRepositoryService.listRepositories(cluster));

            List<String> args = new ArrayList<>(List.of(
                    "install", releaseName, repoName + "/" + chart,
                    "--namespace", namespace,
                    "--create-namespace"
            ));
            if (version != null && !version.isBlank()) {
                args.addAll(List.of("--version", version));
            }
            if (valuesFile != null) {
                args.addAll(List.of("-f", valuesFile.toAbsolutePath().toString()));
            }

            String output = execWithTimeout(kubeconfig, INSTALL_TIMEOUT_SECONDS, args.toArray(String[]::new));
            log.info("Helm release {} installed in namespace {}", releaseName, namespace);
            return output;
        } catch (HelmOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to install Helm chart {}/{}: {}", repoName, chart, e.getMessage());
            throw new HelmOperationException("Failed to install chart: " + e.getMessage(), e);
        } finally {
            deleteQuietly(kubeconfig);
            deleteQuietly(valuesFile);
        }
    }

    public void upgrade(Cluster cluster, String namespace, String releaseName, String chart,
                        String version, String values) {
        Path kubeconfig = writeKubeconfig(cluster);
        Path valuesFile = values != null && !values.isBlank() ? writeValuesFile(values) : null;
        try {
            ensureRepos(kubeconfig, helmRepositoryService.listRepositories(cluster));

            List<String> args = new ArrayList<>(List.of(
                    "upgrade", releaseName, chart,
                    "--namespace", namespace,
                    "--reuse-values"
            ));
            if (version != null && !version.isBlank()) {
                args.addAll(List.of("--version", version));
            }
            if (valuesFile != null) {
                args.addAll(List.of("-f", valuesFile.toAbsolutePath().toString()));
            }

            execWithTimeout(kubeconfig, INSTALL_TIMEOUT_SECONDS, args.toArray(String[]::new));
            log.info("Helm release {} upgraded in namespace {}", releaseName, namespace);
        } catch (HelmOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upgrade Helm release {}: {}", releaseName, e.getMessage());
            throw new HelmOperationException("Failed to upgrade release: " + e.getMessage(), e);
        } finally {
            deleteQuietly(kubeconfig);
            deleteQuietly(valuesFile);
        }
    }

    public void uninstall(Cluster cluster, String namespace, String name) {
        Path kubeconfig = writeKubeconfig(cluster);
        try {
            exec(kubeconfig, "uninstall", name, "--namespace", namespace);
            log.info("Helm release {} uninstalled from namespace {}", name, namespace);
        } catch (HelmOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to uninstall Helm release {}: {}", name, e.getMessage());
            throw new HelmOperationException("Failed to uninstall release: " + e.getMessage(), e);
        } finally {
            deleteQuietly(kubeconfig);
        }
    }

    private void ensureRepos(Path kubeconfig, List<HelmRepository> repos) {
        if (repos.isEmpty()) return;
        for (HelmRepository repo : repos) {
            try {
                exec(kubeconfig, "repo", "add", repo.getName(), repo.getUrl(), "--force-update");
            } catch (HelmOperationException e) {
                log.warn("Failed to add repo {}: {}", repo.getName(), e.getMessage());
            }
        }
        execQuiet(kubeconfig, "repo", "update");
    }

    private Path writeValuesFile(String values) {
        try {
            Path path = Files.createTempFile("greencap-helm-values-", ".yaml");
            Files.writeString(path, values);
            return path;
        } catch (Exception e) {
            throw new HelmOperationException("Failed to write values file: " + e.getMessage(), e);
        }
    }

    private String execWithTimeout(Path kubeconfig, int timeoutSeconds, String... args) {
        try {
            List<String> cmd = buildCommand(kubeconfig, args);
            Process process = new ProcessBuilder(cmd)
                    .redirectErrorStream(false)
                    .start();

            String stdout = new String(process.getInputStream().readAllBytes());
            String stderr = new String(process.getErrorStream().readAllBytes());

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new HelmOperationException("Helm command timed out after " + timeoutSeconds + "s");
            }
            if (process.exitValue() != 0) {
                String message = stderr.isBlank() ? stdout : stderr;
                throw new HelmOperationException("Helm command failed: " + message.trim());
            }
            return stdout;
        } catch (HelmOperationException e) {
            throw e;
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("No such file")) {
                throw new HelmOperationException("Helm CLI not found — ensure 'helm' is installed and available in PATH");
            }
            throw new HelmOperationException("Failed to execute Helm command: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HelmOperationException("Helm command interrupted", e);
        }
    }

    private String exec(Path kubeconfig, String... args) {
        try {
            List<String> cmd = buildCommand(kubeconfig, args);
            Process process = new ProcessBuilder(cmd)
                    .redirectErrorStream(false)
                    .start();

            String stdout = new String(process.getInputStream().readAllBytes());
            String stderr = new String(process.getErrorStream().readAllBytes());

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new HelmOperationException("Helm command timed out after " + TIMEOUT_SECONDS + "s");
            }

            if (process.exitValue() != 0) {
                String message = stderr.isBlank() ? stdout : stderr;
                throw new HelmOperationException("Helm command failed: " + message.trim());
            }

            return stdout;
        } catch (HelmOperationException e) {
            throw e;
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("No such file")) {
                throw new HelmOperationException("Helm CLI not found — ensure 'helm' is installed and available in PATH");
            }
            throw new HelmOperationException("Failed to execute Helm command: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HelmOperationException("Helm command interrupted", e);
        }
    }

    // Returns empty string on non-zero exit instead of throwing (some get subcommands return non-zero for empty)
    private String execQuiet(Path kubeconfig, String... args) {
        try {
            return exec(kubeconfig, args);
        } catch (HelmOperationException e) {
            log.debug("Helm get returned non-zero (may be empty): {}", e.getMessage());
            return "";
        }
    }

    private List<String> buildCommand(Path kubeconfig, String[] args) {
        List<String> cmd = new ArrayList<>();
        cmd.add(HELM_BINARY);
        cmd.addAll(List.of(args));
        cmd.add("--kubeconfig");
        cmd.add(kubeconfig.toAbsolutePath().toString());
        return cmd;
    }

    private List<HelmReleaseInfo> parseReleases(String json) {
        try {
            List<HelmReleaseInfo> releases = new ArrayList<>();
            JsonNode array = objectMapper.readTree(json);
            if (array == null || !array.isArray()) return releases;
            for (JsonNode node : array) {
                releases.add(new HelmReleaseInfo(
                        text(node, "name"),
                        text(node, "namespace"),
                        text(node, "chart"),
                        text(node, "app_version"),
                        text(node, "revision"),
                        text(node, "status"),
                        text(node, "updated")
                ));
            }
            return releases;
        } catch (Exception e) {
            log.error("Failed to parse helm list output: {}", e.getMessage());
            throw new HelmOperationException("Failed to parse Helm releases: " + e.getMessage(), e);
        }
    }

    private Path writeKubeconfig(Cluster cluster) {
        try {
            String content = encryptionService.decrypt(cluster.getKubeconfigContent());
            Path path = Files.createTempFile("greencap-helm-", ".kubeconfig");
            Files.writeString(path, content);
            Files.setPosixFilePermissions(path, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
            ));
            return path;
        } catch (Exception e) {
            throw new HelmOperationException("Failed to prepare kubeconfig: " + e.getMessage(), e);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            if (path != null) Files.deleteIfExists(path);
        } catch (Exception e) {
            log.warn("Failed to delete temp kubeconfig {}: {}", path, e.getMessage());
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText() : "";
    }
}
