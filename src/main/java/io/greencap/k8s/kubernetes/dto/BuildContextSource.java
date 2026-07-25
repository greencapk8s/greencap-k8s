package io.greencap.k8s.kubernetes.dto;

import java.nio.file.Path;

/**
 * Where the files Kaniko builds from come from. A Build has exactly one origin:
 * either a Git repository the Build Pod clones by itself, or a local folder the
 * platform packed in the browser and uploads into the Pod.
 */
public sealed interface BuildContextSource {

    record GitRepository(String url, String branch) implements BuildContextSource {

        public GitRepository {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("Git repository URL is required");
            }
        }
    }

    record LocalFolder(Path archive, String folderName) implements BuildContextSource {

        public LocalFolder {
            if (archive == null) {
                throw new IllegalArgumentException("Local build context archive is required");
            }
            if (folderName == null || folderName.isBlank()) {
                throw new IllegalArgumentException("Local build context folder name is required");
            }
        }
    }
}
