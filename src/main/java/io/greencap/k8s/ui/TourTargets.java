package io.greencap.k8s.ui;

import java.util.List;

/**
 * Element ids the first-access Tour anchors its steps to. These are a contract between the
 * MainLayout that writes them and the step list that reads them — not view internals — so
 * renaming or dropping one silently skips a Tour step in the browser.
 */
final class TourTargets {

    static final String HEADER_CONTEXT = "tour-header-context";
    static final String NAV_DEVELOPER_EXPERIENCE = "tour-nav-developer-experience";
    static final String NAV_TOPOLOGY = "tour-nav-topology";
    static final String NAV_PROJECT = "tour-nav-project";
    static final String NAV_GLOBAL = "tour-nav-global";
    static final String NAV_SETTINGS = "tour-nav-settings";

    static final List<String> ALL = List.of(
            HEADER_CONTEXT,
            NAV_DEVELOPER_EXPERIENCE,
            NAV_TOPOLOGY,
            NAV_PROJECT,
            NAV_GLOBAL,
            NAV_SETTINGS
    );

    private TourTargets() {}
}
