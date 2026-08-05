package io.greencap.k8s.ui;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Where the deploy wizards take the Build Context from. Git is the default: it is versioned and reproducible. */
@Getter
@RequiredArgsConstructor
public enum BuildContextOrigin {

    GIT_REPOSITORY("Git repository"),
    LOCAL_FOLDER("Local folder");

    private final String label;
}
