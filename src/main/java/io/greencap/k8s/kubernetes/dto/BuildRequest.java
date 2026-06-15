package io.greencap.k8s.kubernetes.dto;

public record BuildRequest(String gitRepositoryUrl, String branch, String contextPath, String dockerfilePath, String repository, String tag) {
}
