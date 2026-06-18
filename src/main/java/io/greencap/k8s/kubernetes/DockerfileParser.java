package io.greencap.k8s.kubernetes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DockerfileParser {

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);
    private static final Pattern EXPOSE_PATTERN = Pattern.compile(
            "^EXPOSE\\s+(\\d+)(?:[/\\s].*)?$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();

    public Optional<Integer> fetchExposePort(String gitUrl, String branch, String dockerfilePath) {
        String resolvedPath = (dockerfilePath == null || dockerfilePath.isBlank()) ? "Dockerfile" : dockerfilePath.trim();
        String rawUrl;
        try {
            rawUrl = buildRawUrl(gitUrl.trim(), branch.trim(), resolvedPath);
        } catch (IllegalArgumentException e) {
            log.debug("Cannot build raw URL for Dockerfile: {}", e.getMessage());
            return Optional.empty();
        }

        log.debug("Fetching Dockerfile from: {}", rawUrl);
        try {
            String content = fetchContent(rawUrl);
            return parseFirstExposePort(content);
        } catch (Exception e) {
            log.debug("Failed to fetch or parse Dockerfile from {}: {}", rawUrl, e.getMessage());
            return Optional.empty();
        }
    }

    Optional<Integer> parseFirstExposePort(String content) {
        Matcher matcher = EXPOSE_PATTERN.matcher(content);
        if (matcher.find()) {
            try {
                return Optional.of(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
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
        throw new IllegalArgumentException(
                "Unsupported Git host. Supported providers: GitHub (github.com), GitLab (gitlab.com).");
    }

    private String fetchContent(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " fetching Dockerfile");
        }
        return response.body();
    }
}
