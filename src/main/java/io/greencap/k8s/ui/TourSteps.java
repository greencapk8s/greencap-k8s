package io.greencap.k8s.ui;

import java.util.List;

/**
 * The first-access Tour, written out in full on the server. Order follows the way the eye
 * crosses the screen: the header context first, then the drawer from top to bottom.
 */
final class TourSteps {

    private static final String USERS_SENTENCE =
            " Users is where you invite people to the platform and decide who gets in.";

    private static final String CLOSING_SENTENCE =
            " One last thing: everything you have just seen is GreenCap itself, running as an"
            + " application on the very cluster you are managing.";

    static List<TourStep> forUser(boolean isPlatformAdmin) {
        return List.of(
                new TourStep(TourTargets.HEADER_CONTEXT,
                        "Where you are working",
                        "Everything you do happens inside a cluster and a namespace. This bar shows"
                        + " which ones are active right now, and the selector switches the namespace"
                        + " every screen below is looking at."),

                new TourStep(TourTargets.NAV_DEVELOPER_EXPERIENCE,
                        "Start here",
                        "Templates Catalog installs a ready-made application in a few clicks, and New"
                        + " Application deploys your own code straight from a Dockerfile. This is the"
                        + " shortest path from an empty cluster to something actually running."),

                new TourStep(TourTargets.NAV_TOPOLOGY,
                        "See how it all connects",
                        "Topology draws your namespace as a live map — workloads, services and volumes,"
                        + " with the links between them. It is the fastest way to understand what is"
                        + " running and what depends on what."),

                new TourStep(TourTargets.NAV_PROJECT,
                        "Your day-to-day",
                        "Project holds everything scoped to the selected namespace: Workloads,"
                        + " Networking, Storage, Parameters and Auto Scaling. This is where you inspect,"
                        + " scale and troubleshoot what you deployed."),

                new TourStep(TourTargets.NAV_GLOBAL,
                        "The whole cluster",
                        "Global reaches past a single namespace: register Clusters, create Namespaces,"
                        + " review Infrastructure, and push your images to the Container Registry."),

                new TourStep(TourTargets.NAV_SETTINGS,
                        "Settings — and one last thing",
                        "Settings keeps what belongs to you: theme, refresh interval, and this tour"
                        + " whenever you want to see it again."
                        + (isPlatformAdmin ? USERS_SENTENCE : "")
                        + CLOSING_SENTENCE)
        );
    }

    private TourSteps() {}
}
