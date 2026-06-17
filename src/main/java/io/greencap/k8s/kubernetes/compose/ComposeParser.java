package io.greencap.k8s.kubernetes.compose;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class ComposeParser {

    private static final Set<String> SENSITIVE_KEY_FRAGMENTS =
            Set.of("password", "secret", "token", "key", "credential");
    private static final Set<String> KNOWN_TOP_LEVEL_KEYS =
            Set.of("version", "services", "volumes", "networks");
    private static final Set<String> IGNORED_SERVICE_KEYS =
            Set.of("restart", "healthcheck", "deploy", "logging", "ulimits",
                    "profiles", "secrets", "configs", "networks", "network_mode",
                    "extra_hosts", "dns", "cap_add", "cap_drop", "privileged", "depends_on");
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();

    public ComposeDocument fetch(String gitRepositoryUrl, String branch, String composePath) {
        String rawUrl = buildRawUrl(gitRepositoryUrl.trim(), branch.trim(), composePath.trim());
        log.info("Fetching docker-compose from: {}", rawUrl);
        String content = fetchContent(rawUrl);
        return parse(content);
    }

    private String buildRawUrl(String gitUrl, String branch, String path) {
        String cleaned = gitUrl.replaceAll("\\.git$", "");
        if (cleaned.contains("github.com")) {
            String repoPath = cleaned.replaceFirst("https://github\\.com/", "");
            return "https://raw.githubusercontent.com/" + repoPath + "/" + branch + "/" + path;
        }
        if (cleaned.contains("gitlab.com")) {
            return cleaned + "/-/raw/" + branch + "/" + path;
        }
        throw new ComposeParseException(
                "Unsupported Git host. Supported providers: GitHub (github.com), GitLab (gitlab.com).");
    }

    private String fetchContent(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                throw new ComposeParseException(
                        "File not found in the repository. Check the branch and path.");
            }
            if (response.statusCode() != 200) {
                throw new ComposeParseException(
                        "Failed to fetch docker-compose.yml (HTTP " + response.statusCode() + ").");
            }
            return response.body();
        } catch (IOException e) {
            throw new ComposeParseException("Network error fetching docker-compose.yml: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ComposeParseException("Interrupted while fetching docker-compose.yml.");
        }
    }

    @SuppressWarnings("unchecked")
    ComposeDocument parse(String yamlContent) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<String, Object> root;
        try {
            root = yaml.load(yamlContent);
        } catch (Exception e) {
            throw new ComposeParseException("Invalid YAML: " + e.getMessage());
        }
        if (root == null || !root.containsKey("services")) {
            throw new ComposeParseException("No 'services' section found in docker-compose.yml.");
        }

        List<String> ignoredDirectives = new ArrayList<>();
        for (String key : root.keySet()) {
            if (!KNOWN_TOP_LEVEL_KEYS.contains(key)) {
                ignoredDirectives.add(key);
            }
        }
        if (root.containsKey("networks")) {
            ignoredDirectives.add("networks");
        }

        Map<String, Object> servicesMap = (Map<String, Object>) root.get("services");
        List<ComposeDocument.ParsedService> services = new ArrayList<>();
        for (Map.Entry<String, Object> entry : servicesMap.entrySet()) {
            services.add(parseService(entry.getKey(),
                    (Map<String, Object>) entry.getValue(), ignoredDirectives));
        }

        return new ComposeDocument(services, ignoredDirectives.stream().distinct().toList());
    }

    @SuppressWarnings("unchecked")
    private ComposeDocument.ParsedService parseService(String name,
                                                       Map<String, Object> serviceMap,
                                                       List<String> ignoredDirectives) {
        for (String key : serviceMap.keySet()) {
            if (IGNORED_SERVICE_KEYS.contains(key)) {
                ignoredDirectives.add(key + " (service: " + name + ")");
            }
        }

        return new ComposeDocument.ParsedService(
                name,
                (String) serviceMap.get("image"),
                parseBuild(serviceMap.get("build")),
                parsePorts(serviceMap),
                parseEnvironment(serviceMap),
                parseVolumes(serviceMap),
                parseDependsOn(serviceMap)
        );
    }

    @SuppressWarnings("unchecked")
    private ComposeDocument.BuildSpec parseBuild(Object buildValue) {
        if (buildValue == null) return null;
        if (buildValue instanceof String contextStr) {
            return new ComposeDocument.BuildSpec(contextStr, "Dockerfile");
        }
        if (buildValue instanceof Map<?, ?> buildMap) {
            Map<String, Object> typed = (Map<String, Object>) buildMap;
            return new ComposeDocument.BuildSpec(
                    (String) typed.getOrDefault("context", "."),
                    (String) typed.getOrDefault("dockerfile", "Dockerfile")
            );
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Integer> parsePorts(Map<String, Object> serviceMap) {
        Object portsValue = serviceMap.get("ports");
        if (portsValue == null) return List.of();
        List<Integer> result = new ArrayList<>();
        for (Object portEntry : (List<?>) portsValue) {
            if (portEntry instanceof String portStr) {
                String containerPart = portStr.contains(":") ? portStr.split(":")[1] : portStr;
                containerPart = containerPart.contains("/") ? containerPart.split("/")[0] : containerPart;
                tryParsePort(containerPart.trim()).ifPresent(result::add);
            } else if (portEntry instanceof Map<?, ?> portMap) {
                Object target = ((Map<String, Object>) portMap).get("target");
                if (target instanceof Integer i) result.add(i);
            } else if (portEntry instanceof Integer i) {
                result.add(i);
            }
        }
        return result;
    }

    private java.util.Optional<Integer> tryParsePort(String value) {
        try { return java.util.Optional.of(Integer.parseInt(value)); }
        catch (NumberFormatException e) { return java.util.Optional.empty(); }
    }

    @SuppressWarnings("unchecked")
    private List<ComposeDocument.EnvEntry> parseEnvironment(Map<String, Object> serviceMap) {
        Object envValue = serviceMap.get("environment");
        if (envValue == null) return List.of();
        List<ComposeDocument.EnvEntry> result = new ArrayList<>();
        if (envValue instanceof List<?> envList) {
            for (Object item : envList) {
                String entry = String.valueOf(item);
                int eqIdx = entry.indexOf('=');
                if (eqIdx > 0) {
                    String key = entry.substring(0, eqIdx);
                    String value = entry.substring(eqIdx + 1);
                    result.add(new ComposeDocument.EnvEntry(key, value, isSensitive(key)));
                } else {
                    result.add(new ComposeDocument.EnvEntry(entry, null, isSensitive(entry)));
                }
            }
        } else if (envValue instanceof Map<?, ?> envMap) {
            for (Map.Entry<?, ?> entry : envMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String value = entry.getValue() != null ? String.valueOf(entry.getValue()) : null;
                result.add(new ComposeDocument.EnvEntry(key, value, isSensitive(key)));
            }
        }
        return result;
    }

    private boolean isSensitive(String key) {
        String lower = key.toLowerCase();
        return SENSITIVE_KEY_FRAGMENTS.stream().anyMatch(lower::contains);
    }

    @SuppressWarnings("unchecked")
    private List<ComposeDocument.VolumeEntry> parseVolumes(Map<String, Object> serviceMap) {
        Object volumesValue = serviceMap.get("volumes");
        if (volumesValue == null) return List.of();
        List<ComposeDocument.VolumeEntry> result = new ArrayList<>();
        for (Object item : (List<?>) volumesValue) {
            if (item instanceof String volumeStr) {
                String[] parts = volumeStr.split(":", 2);
                String source = parts[0];
                String target = parts.length > 1 ? parts[1] : source;
                boolean isBindMount = source.startsWith("./") || source.startsWith("/") || source.startsWith("../");
                result.add(new ComposeDocument.VolumeEntry(source, target, isBindMount));
            } else if (item instanceof Map<?, ?> volumeMap) {
                Map<String, Object> typed = (Map<String, Object>) volumeMap;
                String type = (String) typed.getOrDefault("type", "volume");
                String source = (String) typed.getOrDefault("source", "");
                String target = (String) typed.getOrDefault("target", "");
                boolean isBindMount = "bind".equals(type);
                result.add(new ComposeDocument.VolumeEntry(source, target, isBindMount));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseDependsOn(Map<String, Object> serviceMap) {
        Object value = serviceMap.get("depends_on");
        if (value == null) return List.of();
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (value instanceof Map<?, ?> map) {
            return new ArrayList<>(((Map<String, Object>) map).keySet());
        }
        return List.of();
    }
}
