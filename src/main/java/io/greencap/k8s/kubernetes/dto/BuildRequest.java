package io.greencap.k8s.kubernetes.dto;

import java.nio.file.Path;

public record BuildRequest(BuildContextSource source, String contextPath, String dockerfilePath,
                           String repository, String tag) {

    public BuildRequest {
        if (source == null) {
            throw new IllegalArgumentException("Build context source is required");
        }
        if (repository == null || repository.isBlank()) {
            throw new IllegalArgumentException("Target repository is required");
        }
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("Target tag is required");
        }
    }

    public static BuildRequest fromGitRepository(String gitRepositoryUrl, String branch, String contextPath,
                                                 String dockerfilePath, String repository, String tag) {
        return new BuildRequest(new BuildContextSource.GitRepository(gitRepositoryUrl, branch),
                contextPath, dockerfilePath, repository, tag);
    }

    public static BuildRequest fromLocalFolder(Path archive, String folderName, String contextPath,
                                               String dockerfilePath, String repository, String tag) {
        return new BuildRequest(new BuildContextSource.LocalFolder(archive, folderName),
                contextPath, dockerfilePath, repository, tag);
    }
}
