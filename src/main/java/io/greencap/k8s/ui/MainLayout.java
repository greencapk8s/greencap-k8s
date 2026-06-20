package io.greencap.k8s.ui;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.user.Permission;
import io.greencap.k8s.domain.cluster.ClusterService;
import io.greencap.k8s.domain.cluster.ConnectionStatus;
import io.greencap.k8s.domain.user.UserService;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.NamespaceService;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@JsModule("@vaadin/vaadin-lumo-styles/badge-global.js")
@JsModule("@vaadin/vaadin-lumo-styles/utility-global.js")
public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private static final String NAMESPACE_CONTEXT_TOOLTIP =
            "Namespace-scoped — depends on the namespace selected above";
    private static final String CLUSTER_CONTEXT_TOOLTIP =
            "Cluster-scoped — independent of the selected namespace";

    private final ClusterContext clusterContext;
    private final UserService userService;
    private final NamespaceService namespaceService;
    private final ClusterService clusterService;
    private final BuildProperties buildProperties;
    private final HorizontalLayout clusterInfoLayout = new HorizontalLayout();
    private final HorizontalLayout namespaceLayout = new HorizontalLayout();
    private final com.vaadin.flow.component.combobox.ComboBox<String> namespaceCombo = new com.vaadin.flow.component.combobox.ComboBox<>();
    private RefreshInterval currentRefreshInterval = RefreshInterval.THREE_SECONDS;
    private final Div clusterWarningBanner = new Div();
    private final List<SideNavItem> clusterDependentNavItems = new ArrayList<>();

    private final ScheduledExecutorService refreshExecutor =
            Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    private ScheduledFuture<?> refreshTask;

    private Cluster lastLoadedCluster = null;
    private String currentPath = "";
    private boolean suppressNavigation = false;

    public MainLayout(ClusterContext clusterContext, UserService userService, NamespaceService namespaceService, ClusterService clusterService, BuildProperties buildProperties) {
        this.clusterContext = clusterContext;
        this.userService = userService;
        this.namespaceService = namespaceService;
        this.clusterService = clusterService;
        this.buildProperties = buildProperties;
        setPrimarySection(Section.DRAWER);

        clusterInfoLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        clusterInfoLayout.setSpacing(true);
        clusterInfoLayout.setPadding(false);

        buildNamespaceLayout();
        buildClusterWarningBanner();

        addToNavbar(buildNavbar());
        addToNavbar(true, clusterWarningBanner);
        addToDrawer(buildDrawer());

        if (clusterContext.getCluster() == null) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            userService.findActiveCluster(username).ifPresent(clusterContext::setCluster);
            userService.findActiveNamespace(username).ifPresent(clusterContext::setNamespace);
        }

        addDetachListener(e -> {
            stopRefreshTimer();
            refreshExecutor.shutdown();
        });
    }

    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        int refreshSeconds = userService.findRefreshInterval(username)
                .orElse(RefreshInterval.THREE_SECONDS.getSeconds());
        currentRefreshInterval = RefreshInterval.fromSeconds(refreshSeconds);
        applyRefreshInterval(currentRefreshInterval);

        String theme = userService.findTheme(username).orElse("DARK");
        applyTheme(theme);

        int drawerWidth = userService.findDrawerWidth(username).orElse(240);
        initResizableDrawer(drawerWidth);
    }

    public void applyTheme(String theme) {
        if ("LIGHT".equals(theme)) {
            getElement().removeAttribute("theme");
        } else {
            getElement().setAttribute("theme", Lumo.DARK);
        }
    }

    @ClientCallable
    public void saveDrawerWidth(int width) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.updateDrawerWidth(username, width);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        currentPath = event.getLocation().getPath();
        updateClusterInfo();
        updateNamespaceSelector();
        restartRefreshTimer();
    }

    public void updateClusterInfo() {
        clusterInfoLayout.removeAll();
        Cluster cluster = clusterContext.getCluster();
        if (cluster != null) {
            Span label = new Span("Cluster:");
            label.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

            Span name = new Span(cluster.getName());
            name.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.MEDIUM);

            Span badge = new Span(cluster.getConnectionStatus().name());
            badge.getElement().getThemeList().add("badge");
            badge.getElement().getThemeList().add("small");
            applyStatusTheme(badge, cluster.getConnectionStatus());

            clusterInfoLayout.add(label, name, badge);
        } else {
            Span noCluster = new Span("No active cluster");
            noCluster.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            clusterInfoLayout.add(noCluster);
        }
    }

    private void updateNamespaceSelector() {
        Cluster cluster = clusterContext.getCluster();
        namespaceLayout.setVisible(cluster != null);

        if (cluster == null) {
            lastLoadedCluster = null;
            setClusterReachable(false);
            return;
        }

        if (!cluster.equals(lastLoadedCluster)) {
            loadNamespacesForCluster(cluster);
        } else {
            String contextNamespace = clusterContext.getNamespace();
            if (contextNamespace != null && !contextNamespace.equals(namespaceCombo.getValue())) {
                suppressNavigation = true;
                namespaceCombo.setValue(contextNamespace);
                suppressNavigation = false;
            }
        }
    }

    private void loadNamespacesForCluster(Cluster cluster) {
        namespaceCombo.setItems(List.of());
        namespaceCombo.setPlaceholder("Loading...");
        namespaceCombo.setEnabled(false);

        UI ui = UI.getCurrent();
        Thread.ofVirtual().start(() -> {
            try {
                List<String> names = namespaceService.listNamespaceNames(cluster);
                ui.access(() -> {
                    String current = clusterContext.getNamespace();
                    String preferred;
                    if (current != null && names.contains(current)) {
                        preferred = current;
                    } else if (names.contains("default")) {
                        preferred = "default";
                    } else {
                        preferred = names.isEmpty() ? null : names.get(0);
                    }

                    // Push 1: send items so the client populates its item cache
                    namespaceCombo.setItems(names);
                    namespaceCombo.setPlaceholder("Select...");
                    namespaceCombo.setEnabled(true);
                    if (preferred != null) {
                        clusterContext.setNamespace(preferred);
                    }
                    lastLoadedCluster = cluster;
                    setClusterReachable(true);

                    // Push 2: set value in a separate push cycle so items are already
                    // available on the client when the selection is applied.
                    // Calling ui.access() from a new thread (not the current session thread)
                    // guarantees a distinct push batch.
                    if (preferred != null) {
                        final String finalPreferred = preferred;
                        Thread.ofVirtual().start(() -> ui.access(() -> {
                            suppressNavigation = true;
                            namespaceCombo.setValue(finalPreferred);
                            suppressNavigation = false;
                        }));
                    }
                });
            } catch (KubernetesOperationException e) {
                ui.access(() -> {
                    clusterService.markAsDisconnectedIfConnected(cluster);
                    updateClusterInfo();
                    namespaceLayout.setVisible(false);
                    setClusterReachable(false);
                    Notification notification = Notification.show(
                            "Cluster unreachable: " + cluster.getName(), UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                });
            }
        });
    }

    private void applyRefreshInterval(RefreshInterval interval) {
        stopRefreshTimer();
        if (!interval.isActive()) return;
        UI ui = UI.getCurrent();
        refreshTask = refreshExecutor.scheduleAtFixedRate(() ->
                ui.access(() -> {
                    com.vaadin.flow.component.Component content = getContent();
                    if (content instanceof Refreshable refreshable) {
                        refreshable.refresh();
                    }
                }),
                interval.getSeconds(), interval.getSeconds(), TimeUnit.SECONDS);
    }

    private void stopRefreshTimer() {
        if (refreshTask != null && !refreshTask.isDone()) {
            refreshTask.cancel(false);
            refreshTask = null;
        }
    }

    public void applyAndPersistRefreshInterval(RefreshInterval interval) {
        currentRefreshInterval = interval;
        applyRefreshInterval(interval);
    }

    private void restartRefreshTimer() {
        if (currentRefreshInterval != null && currentRefreshInterval.isActive()) {
            applyRefreshInterval(currentRefreshInterval);
        }
    }

    private void buildClusterWarningBanner() {
        Icon warningIcon = VaadinIcon.WARNING.create();
        warningIcon.getStyle().set("flex-shrink", "0");

        Span text = new Span("Cluster unreachable — check your connection settings in Global › Clusters");
        text.addClassNames(LumoUtility.FontSize.SMALL);

        Button retryButton = new Button("Retry", VaadinIcon.REFRESH.create(), e -> retryClusterConnection());
        retryButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        retryButton.getStyle().set("flex-shrink", "0");

        HorizontalLayout content = new HorizontalLayout(warningIcon, text, retryButton);
        content.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        content.setSpacing(true);
        content.setPadding(false);
        content.setWidthFull();
        content.addClassNames(LumoUtility.JustifyContent.CENTER);

        clusterWarningBanner.add(content);
        clusterWarningBanner.getStyle()
                .set("background", "var(--lumo-warning-color-10pct)")
                .set("color", "var(--lumo-warning-text-color)")
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-m)")
                .set("width", "100%");
        clusterWarningBanner.setVisible(false);
    }

    public void refreshClusterState() {
        lastLoadedCluster = null;
        updateClusterInfo();
        updateNamespaceSelector();
    }

    private void retryClusterConnection() {
        refreshClusterState();
    }

    private void setClusterReachable(boolean reachable) {
        clusterWarningBanner.setVisible(!reachable);
        for (SideNavItem item : clusterDependentNavItems) {
            if (reachable) {
                item.getStyle().remove("opacity").remove("pointer-events");
            } else {
                item.getStyle().set("opacity", "0.4").set("pointer-events", "none");
            }
        }
    }

    private void applyStatusTheme(Span badge, ConnectionStatus status) {
        switch (status) {
            case CONNECTED    -> badge.getElement().getThemeList().add("success");
            case ERROR        -> badge.getElement().getThemeList().add("error");
            case DISCONNECTED -> badge.getElement().getThemeList().add("contrast");
            default           -> {}
        }
    }

    private void buildNamespaceLayout() {
        Span label = new Span("Namespace:");
        label.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        namespaceCombo.setPlaceholder("Select...");
        namespaceCombo.setWidth("220px");
        namespaceCombo.getElement().getThemeList().add("small");
        namespaceCombo.addValueChangeListener(e -> {
            if (e.getValue() != null && !suppressNavigation) {
                clusterContext.setNamespace(e.getValue());
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                userService.updateActiveNamespace(username, e.getValue());
                final int PREVIOUS_PAGE = -1;
                if (currentPath.startsWith("yaml/")) {
                    UI.getCurrent().getPage().getHistory().go(PREVIOUS_PAGE);
                } else {
                    UI.getCurrent().navigate(currentPath);
                }
            }
        });

        namespaceLayout.add(label, namespaceCombo);
        namespaceLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        namespaceLayout.setSpacing(true);
        namespaceLayout.setPadding(false);
        namespaceLayout.setVisible(false);
    }

    private HorizontalLayout buildNavbar() {
        DrawerToggle toggle = new DrawerToggle();

        Div spacer = new Div();

        Button logout = new Button(VaadinIcon.SIGN_OUT.create(), e -> {
            VaadinSession.getCurrent().getSession().invalidate();
            UI.getCurrent().getPage().executeJs("window.location.href='/login'");
        });
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        logout.getElement().setAttribute("title", "Logout");

        HorizontalLayout navbar = new HorizontalLayout(toggle, namespaceLayout, spacer, clusterInfoLayout, logout);
        navbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        navbar.expand(spacer);
        navbar.setWidthFull();
        return navbar;
    }

    private VerticalLayout buildDrawer() {
        VerticalLayout navContent = new VerticalLayout();
        navContent.setSizeUndefined();
        navContent.setWidthFull();
        navContent.setPadding(false);
        navContent.setSpacing(false);
        navContent.add(buildLogoSection());
        navContent.add(buildNewApplicationNav());
        navContent.add(buildNavSection("PROJECT", buildVisaoGeralNav(), NAMESPACE_CONTEXT_TOOLTIP));
        navContent.add(buildNavSection("GLOBAL", buildGlobalNav(), CLUSTER_CONTEXT_TOOLTIP));
        navContent.add(buildNavSection("DEVELOPER EXPERIENCE", buildDeveloperExperienceNav(), CLUSTER_CONTEXT_TOOLTIP));
        navContent.add(buildNavSection("SETTINGS", buildConfiguracaoNav()));

        Scroller scroller = new Scroller(navContent);
        scroller.setWidthFull();

        VerticalLayout drawer = new VerticalLayout(scroller, buildVersionFooter());
        drawer.setSizeFull();
        drawer.setPadding(false);
        drawer.setSpacing(false);
        drawer.expand(scroller);
        return drawer;
    }

    private Div buildVersionFooter() {
        String version = "v" + buildProperties.getVersion();
        Span versionLabel = new Span(version);
        versionLabel.addClassNames(
                LumoUtility.FontSize.XXSMALL,
                LumoUtility.TextColor.TERTIARY
        );

        Div footer = new Div(versionLabel);
        footer.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.JustifyContent.CENTER,
                LumoUtility.Padding.Vertical.SMALL
        );
        footer.setWidthFull();
        return footer;
    }

    private HorizontalLayout buildLogoSection() {
        Image logo = new Image("greencap.png", "GreenCap K8s");
        logo.setHeight("36px");

        Span appName = new Span("GreenCap K8s");
        appName.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.MEDIUM);

        HorizontalLayout logoRow = new HorizontalLayout(logo, appName);
        logoRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        logoRow.setSpacing(true);
        logoRow.addClassNames(
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Padding.Vertical.MEDIUM
        );
        return logoRow;
    }

    private VerticalLayout buildNavSection(String label, SideNav nav) {
        return buildNavSection(label, nav, null);
    }

    private VerticalLayout buildNavSection(String label, SideNav nav, String contextTooltip) {
        Span sectionLabel = new Span(label);
        sectionLabel.addClassNames(
                LumoUtility.FontWeight.BOLD,
                LumoUtility.TextColor.SECONDARY
        );

        HorizontalLayout sectionLabelRow = new HorizontalLayout(sectionLabel);
        sectionLabelRow.addClassNames(
                LumoUtility.FontSize.XXSMALL,
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Padding.Top.MEDIUM,
                LumoUtility.Padding.Bottom.XSMALL
        );
        sectionLabelRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        sectionLabelRow.setSpacing(false);
        sectionLabelRow.setPadding(false);

        if (contextTooltip != null) {
            Icon contextIcon = VaadinIcon.INFO_CIRCLE_O.create();
            contextIcon.setSize("14px");
            contextIcon.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Left.XSMALL);
            contextIcon.getElement().setAttribute("title", contextTooltip);
            sectionLabelRow.add(contextIcon);
        }

        VerticalLayout section = new VerticalLayout(sectionLabelRow, nav);
        section.setPadding(false);
        section.setSpacing(false);
        section.setWidthFull();
        return section;
    }

    private SideNav buildNewApplicationNav() {
        SideNav nav = new SideNav();
        nav.setWidthFull();

        boolean canDeployApp = SecurityUtils.hasPermission(Permission.PROJECT_DEPLOY_APPLICATION);
        SideNavItem newApp = navItem("New Application", DeployApplicationView.class, VaadinIcon.PLUS_CIRCLE, canDeployApp);

        if (canDeployApp) {
            clusterDependentNavItems.add(newApp);
        }

        nav.addItem(newApp);
        return nav;
    }

    private SideNav buildVisaoGeralNav() {
        SideNav nav = new SideNav();
        nav.setWidthFull();

        boolean canTopology   = SecurityUtils.hasPermission(Permission.TOPOLOGY_VIEW);
        SideNavItem topologia = navItem("Topology", TopologiaView.class, VaadinIcon.CLUSTER, canTopology);

        SideNavItem observability = buildObservabilidadeNavItem();
        SideNavItem workloads   = buildWorkloadsNavItem();
        SideNavItem networking  = buildRedeNavItem();
        SideNavItem parameters  = buildConfigNavItem();
        SideNavItem autoScaling = buildAutoScalingNavItem();
        SideNavItem storage     = buildStorageNavItem();
        SideNavItem helm        = buildHelmNavItem();

        addIfEnabled(topologia, observability, workloads, networking, parameters, autoScaling, storage, helm);

        nav.addItem(topologia, observability, workloads, autoScaling, networking, parameters, storage, helm);
        return nav;
    }

    private SideNavItem buildWorkloadsNavItem() {
        boolean canDeploy     = SecurityUtils.hasPermission(Permission.WORKLOADS_DEPLOYMENTS_VIEW);
        boolean canStatefulSet = SecurityUtils.hasPermission(Permission.WORKLOADS_STATEFULSETS_VIEW);
        boolean canReplica    = SecurityUtils.hasPermission(Permission.WORKLOADS_REPLICASETS_VIEW);
        boolean canPods       = SecurityUtils.hasPermission(Permission.WORKLOADS_PODS_VIEW);
        boolean canJobs       = SecurityUtils.hasPermission(Permission.WORKLOADS_JOBS_VIEW);
        boolean canCronJobs   = SecurityUtils.hasPermission(Permission.WORKLOADS_CRONJOBS_VIEW);
        boolean anyChild      = canDeploy || canStatefulSet || canReplica || canPods || canJobs || canCronJobs;

        SideNavItem workloads = navItem("Workloads", DeploymentsView.class, VaadinIcon.CUBES, anyChild);
        workloads.addItem(navItem("Deployments", DeploymentsView.class, VaadinIcon.ROCKET, canDeploy));
        workloads.addItem(navItem("StatefulSets", StatefulSetsView.class, VaadinIcon.DATABASE, canStatefulSet));
        workloads.addItem(navItem("ReplicaSets", ReplicaSetView.class, VaadinIcon.COPY, canReplica));
        workloads.addItem(navItem("Pods", PodsView.class, VaadinIcon.CUBE, canPods));
        workloads.addItem(navItem("Jobs", JobsView.class, VaadinIcon.TASKS, canJobs));
        workloads.addItem(navItem("CronJobs", CronJobsView.class, VaadinIcon.CLOCK, canCronJobs));
        return workloads;
    }

    private SideNavItem buildRedeNavItem() {
        boolean canServices  = SecurityUtils.hasPermission(Permission.NETWORKING_SERVICES_VIEW);
        boolean canIngresses = SecurityUtils.hasPermission(Permission.NETWORKING_INGRESS_VIEW);
        boolean anyChild     = canServices || canIngresses;

        SideNavItem networking = navItem("Networking", ServicesView.class, VaadinIcon.CONNECT, anyChild);
        networking.addItem(navItem("Services", ServicesView.class, VaadinIcon.SHARE, canServices));
        networking.addItem(navItem("Ingresses", IngressView.class, VaadinIcon.ARROWS_LONG_RIGHT, canIngresses));
        return networking;
    }

    private SideNavItem buildConfigNavItem() {
        boolean canConfigMaps = SecurityUtils.hasPermission(Permission.PARAMETERS_CONFIGMAPS_VIEW);
        boolean canSecrets    = SecurityUtils.hasPermission(Permission.PARAMETERS_SECRETS_VIEW);
        boolean anyChild      = canConfigMaps || canSecrets;

        SideNavItem parameters = navItem("Parameters", ConfigMapsView.class, VaadinIcon.SLIDERS, anyChild);
        parameters.addItem(navItem("ConfigMaps", ConfigMapsView.class, VaadinIcon.FILE_TEXT, canConfigMaps));
        parameters.addItem(navItem("Secrets", SecretsView.class, VaadinIcon.LOCK, canSecrets));
        return parameters;
    }

    private SideNavItem buildAutoScalingNavItem() {
        boolean canHpa = SecurityUtils.hasPermission(Permission.AUTOSCALING_HORIZONTALSCALER_VIEW);

        SideNavItem autoScaling = navItem("Auto Scaling", HorizontalScalerView.class, VaadinIcon.SCALE, canHpa);
        autoScaling.addItem(navItem("Horizontal Scaler", HorizontalScalerView.class, VaadinIcon.RESIZE_H, canHpa));
        return autoScaling;
    }

    private SideNavItem buildHelmNavItem() {
        boolean canHelm = SecurityUtils.hasPermission(Permission.PROJECT_HELM_VIEW);

        SideNavItem helm = navItem("Helm", HelmReleasesView.class, VaadinIcon.PACKAGE, canHelm);
        helm.addItem(navItem("Releases", HelmReleasesView.class, VaadinIcon.LIST, canHelm));
        return helm;
    }

    private SideNavItem buildStorageNavItem() {
        boolean canPvc = SecurityUtils.hasPermission(Permission.STORAGE_PVC_VIEW);

        SideNavItem storage = navItem("Storage", PersistentVolumeClaimsView.class, VaadinIcon.STORAGE, canPvc);
        storage.addItem(navItem("Volume Claims (PVC)", PersistentVolumeClaimsView.class, VaadinIcon.DATABASE, canPvc));
        return storage;
    }

    private SideNavItem buildObservabilidadeNavItem() {
        boolean canDashboard = SecurityUtils.hasPermission(Permission.OBSERVABILITY_DASHBOARD_VIEW);
        boolean canEvents    = SecurityUtils.hasPermission(Permission.OBSERVABILITY_EVENTS_VIEW);
        boolean canMetrics   = SecurityUtils.hasPermission(Permission.OBSERVABILITY_METRICS_VIEW);
        boolean anyChild     = canDashboard || canEvents || canMetrics;

        SideNavItem observability = navItem("Observability", DashboardView.class, VaadinIcon.EYE, anyChild);
        observability.addItem(navItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD, canDashboard));
        observability.addItem(navItem("Events", EventsView.class, VaadinIcon.RECORDS, canEvents));
        observability.addItem(navItem("Metrics", MetricsView.class, VaadinIcon.CHART, canMetrics));
        return observability;
    }

    private SideNav buildGlobalNav() {
        SideNav nav = new SideNav();
        nav.setWidthFull();

        SideNavItem clustersItem = navItem("Clusters", ClustersView.class, VaadinIcon.SERVER,
                SecurityUtils.hasPermission(Permission.GLOBAL_CLUSTERS_VIEW));

        SideNavItem namespacesItem = navItem("Namespaces", NamespacesView.class, VaadinIcon.FOLDER_O,
                SecurityUtils.hasPermission(Permission.GLOBAL_NAMESPACES_VIEW));

        if (SecurityUtils.hasPermission(Permission.GLOBAL_NAMESPACES_VIEW)) {
            clusterDependentNavItems.add(namespacesItem);
        }

        nav.addItem(clustersItem, namespacesItem, buildInfrastructureNavItem(), buildRegistryNavItem());
        return nav;
    }

    private SideNav buildDeveloperExperienceNav() {
        SideNav nav = new SideNav();
        nav.setWidthFull();

        boolean canOperators = SecurityUtils.hasPermission(Permission.DEVELOPER_EXPERIENCE_OPERATORS_VIEW);

        SideNavItem operatorsItem = navItem("Operators", InstalledOperatorsView.class, VaadinIcon.GRID_BIG_O, canOperators);
        if (canOperators) {
            Span betaBadge = new Span("beta");
            betaBadge.getStyle()
                    .set("font-size", "var(--lumo-font-size-xxs)")
                    .set("color", "var(--lumo-primary-color)")
                    .set("font-weight", "600")
                    .set("letter-spacing", "0.05em")
                    .set("margin-left", "var(--lumo-space-xs)");
            operatorsItem.setSuffixComponent(betaBadge);
        }
        operatorsItem.addItem(navItem("Installed", InstalledOperatorsView.class, VaadinIcon.CHECK_CIRCLE_O, canOperators));
        operatorsItem.addItem(navItem("Catalog", OperatorCatalogView.class, VaadinIcon.SEARCH, canOperators));

        if (canOperators) {
            clusterDependentNavItems.add(operatorsItem);
        }

        nav.addItem(operatorsItem);
        return nav;
    }

    private SideNav buildConfiguracaoNav() {
        SideNav nav = new SideNav();
        nav.setWidthFull();

        SideNavItem usersItem = navItem("Users", UserManagementView.class, VaadinIcon.USERS,
                SecurityUtils.hasPermission(Permission.SETTINGS_USERS_VIEW));
        SideNavItem settingsItem = navItem("Settings", PlatformSettingsView.class, VaadinIcon.COG,
                SecurityUtils.hasPermission(Permission.SETTINGS_PLATFORM_VIEW));

        nav.addItem(usersItem, settingsItem);
        return nav;
    }

    private SideNavItem buildInfrastructureNavItem() {
        boolean canInfra = SecurityUtils.hasPermission(Permission.GLOBAL_INFRASTRUCTURE_VIEW);

        SideNavItem infrastructure = navItem("Infrastructure", PersistentVolumesView.class, VaadinIcon.CLOUD, canInfra);
        infrastructure.addItem(navItem("Persistent Volumes (PV)", PersistentVolumesView.class, VaadinIcon.HARDDRIVE, canInfra));
        infrastructure.addItem(navItem("Storage Classes", StorageClassesView.class, VaadinIcon.STORAGE, canInfra));
        infrastructure.addItem(navItem("Nodes", NodesView.class, VaadinIcon.SERVER, canInfra));

        if (canInfra) {
            clusterDependentNavItems.add(infrastructure);
        }
        return infrastructure;
    }

    private SideNavItem buildRegistryNavItem() {
        boolean canRegistry = SecurityUtils.hasPermission(Permission.GLOBAL_REGISTRY_VIEW);

        SideNavItem registry = navItem("Container Registry", RegistryView.class, VaadinIcon.ARCHIVE, canRegistry);

        if (canRegistry) {
            clusterDependentNavItems.add(registry);
        }
        return registry;
    }

    private void addIfEnabled(SideNavItem... items) {
        for (SideNavItem item : items) {
            if (!isPermissionDisabled(item)) {
                clusterDependentNavItems.add(item);
            }
        }
    }

    private boolean isPermissionDisabled(SideNavItem item) {
        return "0.4".equals(item.getStyle().get("opacity"));
    }

    private void initResizableDrawer(int initialWidth) {
        getElement().executeJs("""
            (function(initial) {
                const MIN_WIDTH = 180;
                const MAX_WIDTH = 400;

                const appLayout = $0;
                const shadow = appLayout.shadowRoot;
                if (!shadow) return;

                const drawerPart  = shadow.querySelector('[part="drawer"]');
                const navbarPart  = shadow.querySelector('[part="navbar"]');
                const contentPart = shadow.querySelector('#content') || shadow.querySelector('[part="content"]');

                function applyWidth(w) {
                    appLayout.style.setProperty('--vaadin-app-layout-drawer-width', w + 'px');
                    if (drawerPart) drawerPart.style.width = w + 'px';
                    const isOpen = appLayout.hasAttribute('drawer-opened');
                    if (navbarPart)  navbarPart.style.left  = isOpen ? w + 'px' : '0px';
                    if (contentPart) contentPart.style.marginInlineStart = isOpen ? w + 'px' : '0px';
                }

                const drawerObserver = new MutationObserver(() => {
                    const w = parseInt(drawerPart ? drawerPart.style.width : initial) || initial;
                    const isOpen = appLayout.hasAttribute('drawer-opened');
                    if (navbarPart)  navbarPart.style.left  = isOpen ? w + 'px' : '0px';
                    if (contentPart) contentPart.style.marginInlineStart = isOpen ? w + 'px' : '0px';
                });
                drawerObserver.observe(appLayout, { attributes: true, attributeFilter: ['drawer-opened'] });

                applyWidth(initial);

                const handle = document.createElement('div');
                handle.style.cssText = 'position:fixed;top:0;left:' + initial + 'px;width:5px;height:100vh;cursor:col-resize;z-index:1000;background:transparent;';
                document.body.appendChild(handle);

                handle.addEventListener('mouseenter', () => { handle.style.background = 'rgba(255,255,255,0.15)'; });
                handle.addEventListener('mouseleave', () => { if (!dragging) handle.style.background = 'transparent'; });

                let dragging = false;
                let startX = 0;
                let startWidth = initial;

                handle.addEventListener('mousedown', function(e) {
                    dragging = true;
                    startX = e.clientX;
                    startWidth = parseInt(drawerPart ? drawerPart.style.width : initial) || initial;
                    document.body.style.userSelect = 'none';
                    e.preventDefault();
                });

                document.addEventListener('mousemove', function(e) {
                    if (!dragging) return;
                    const newWidth = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, startWidth + (e.clientX - startX)));
                    applyWidth(newWidth);
                    handle.style.left = newWidth + 'px';
                });

                document.addEventListener('mouseup', function() {
                    if (!dragging) return;
                    dragging = false;
                    document.body.style.userSelect = '';
                    handle.style.background = 'transparent';
                    const finalWidth = parseInt(drawerPart ? drawerPart.style.width : initial);
                    appLayout.$server.saveDrawerWidth(finalWidth);
                });
            })($1);
        """, getElement(), initialWidth);
    }

    private SideNavItem disabledNavItem(String label, VaadinIcon icon) {
        SideNavItem item = new SideNavItem(label);
        item.setPrefixComponent(icon.create());
        item.getStyle()
                .set("opacity", "0.4")
                .set("pointer-events", "none");
        return item;
    }

    private <T extends com.vaadin.flow.component.Component> SideNavItem navItem(
            String label, Class<T> view, VaadinIcon icon, boolean enabled) {
        SideNavItem item = new SideNavItem(label, view, icon.create());
        if (!enabled) {
            item.getStyle()
                    .set("opacity", "0.4")
                    .set("pointer-events", "none");
        }
        return item;
    }
}
