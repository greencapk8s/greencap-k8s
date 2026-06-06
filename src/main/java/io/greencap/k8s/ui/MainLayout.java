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
import io.greencap.k8s.domain.cluster.ClusterService;
import io.greencap.k8s.domain.cluster.ConnectionStatus;
import io.greencap.k8s.domain.user.UserService;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.NamespaceService;
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

    private final ClusterContext clusterContext;
    private final UserService userService;
    private final NamespaceService namespaceService;
    private final ClusterService clusterService;
    private final HorizontalLayout clusterInfoLayout = new HorizontalLayout();
    private final HorizontalLayout namespaceLayout = new HorizontalLayout();
    private final com.vaadin.flow.component.combobox.ComboBox<String> namespaceCombo = new com.vaadin.flow.component.combobox.ComboBox<>();
    private RefreshInterval currentRefreshInterval = RefreshInterval.NONE;
    private final Div clusterWarningBanner = new Div();
    private final List<SideNavItem> clusterDependentNavItems = new ArrayList<>();

    private final ScheduledExecutorService refreshExecutor =
            Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    private ScheduledFuture<?> refreshTask;

    private Cluster lastLoadedCluster = null;
    private String currentPath = "";
    private boolean suppressNavigation = false;

    public MainLayout(ClusterContext clusterContext, UserService userService, NamespaceService namespaceService, ClusterService clusterService) {
        this.clusterContext = clusterContext;
        this.userService = userService;
        this.namespaceService = namespaceService;
        this.clusterService = clusterService;
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

        userService.findRefreshInterval(username).ifPresent(seconds -> {
            currentRefreshInterval = RefreshInterval.fromSeconds(seconds);
            applyRefreshInterval(currentRefreshInterval);
        });

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

                    suppressNavigation = true;
                    namespaceCombo.setItems(names);
                    namespaceCombo.setPlaceholder("Select...");
                    namespaceCombo.setEnabled(true);
                    if (preferred != null) {
                        namespaceCombo.setValue(preferred);
                        clusterContext.setNamespace(preferred);
                    }
                    suppressNavigation = false;

                    lastLoadedCluster = cluster;
                    setClusterReachable(true);
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

        Span text = new Span("Cluster unreachable — check your connection settings in Settings › Clusters");
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
        namespaceCombo.setWidth("180px");
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
        VerticalLayout drawer = new VerticalLayout();
        drawer.setSizeUndefined();
        drawer.setWidthFull();
        drawer.setPadding(false);
        drawer.setSpacing(false);

        drawer.add(buildLogoSection());
        drawer.add(buildNavSection("PROJECT", buildVisaoGeralNav()));
        drawer.add(buildNavSection("OBSERVABILITY", buildObservabilidadeNav()));
        drawer.add(buildNavSection("SETTINGS", buildConfiguracaoNav()));

        return drawer;
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
        Span sectionLabel = new Span(label);
        sectionLabel.addClassNames(
                LumoUtility.FontSize.XXSMALL,
                LumoUtility.FontWeight.BOLD,
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Padding.Top.MEDIUM,
                LumoUtility.Padding.Bottom.XSMALL
        );

        VerticalLayout section = new VerticalLayout(sectionLabel, nav);
        section.setPadding(false);
        section.setSpacing(false);
        section.setWidthFull();
        return section;
    }

    private SideNav buildVisaoGeralNav() {
        SideNav nav = new SideNav();
        nav.setWidthFull();

        SideNavItem workloads   = buildWorkloadsNavItem();
        SideNavItem networking  = buildRedeNavItem();
        SideNavItem parameters  = buildConfigNavItem();
        SideNavItem autoScaling = buildAutoScalingNavItem();
        SideNavItem storage     = buildStorageNavItem();
        SideNavItem topologia = new SideNavItem("Topology", TopologiaView.class, VaadinIcon.CLUSTER.create());
        clusterDependentNavItems.addAll(List.of(workloads, networking, parameters, autoScaling, storage, topologia));

        nav.addItem(topologia, workloads, autoScaling, networking, parameters, storage);
        return nav;
    }

    private SideNavItem buildWorkloadsNavItem() {
        SideNavItem workloads = new SideNavItem("Workloads", DeploymentsView.class, VaadinIcon.CUBES.create());
        workloads.addItem(new SideNavItem("Deployments", DeploymentsView.class, VaadinIcon.ROCKET.create()));
        workloads.addItem(new SideNavItem("ReplicaSets", ReplicaSetView.class, VaadinIcon.COPY.create()));
        workloads.addItem(new SideNavItem("Pods", PodsView.class, VaadinIcon.CUBE.create()));
        return workloads;
    }

    private SideNavItem buildRedeNavItem() {
        SideNavItem networking = new SideNavItem("Networking", ServicesView.class, VaadinIcon.CONNECT.create());
        networking.addItem(new SideNavItem("Services", ServicesView.class, VaadinIcon.SHARE.create()));
        return networking;
    }

    private SideNavItem buildConfigNavItem() {
        SideNavItem parameters = new SideNavItem("Parameters", ConfigMapsView.class, VaadinIcon.SLIDERS.create());
        parameters.addItem(new SideNavItem("ConfigMaps", ConfigMapsView.class, VaadinIcon.FILE_TEXT.create()));
        parameters.addItem(new SideNavItem("Secrets", SecretsView.class, VaadinIcon.LOCK.create()));
        return parameters;
    }

    private SideNavItem buildAutoScalingNavItem() {
        SideNavItem autoScaling = new SideNavItem("Auto Scaling", HorizontalScalerView.class, VaadinIcon.SCALE.create());
        autoScaling.addItem(new SideNavItem("Horizontal Scaler", HorizontalScalerView.class, VaadinIcon.RESIZE_H.create()));
        return autoScaling;
    }

    private SideNavItem buildStorageNavItem() {
        SideNavItem storage = new SideNavItem("Storage", PersistentVolumeClaimsView.class, VaadinIcon.STORAGE.create());
        storage.addItem(new SideNavItem("Volume Claims (PVC)", PersistentVolumeClaimsView.class, VaadinIcon.DATABASE.create()));
        return storage;
    }

    private SideNav buildObservabilidadeNav() {
        SideNav nav = new SideNav();
        nav.setWidthFull();

        SideNavItem dashboard = new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create());
        SideNavItem events  = new SideNavItem("Events", EventsView.class, VaadinIcon.RECORDS.create());
        SideNavItem metrics = new SideNavItem("Metrics", MetricsView.class, VaadinIcon.CHART.create());
        clusterDependentNavItems.addAll(List.of(dashboard, events, metrics));

        nav.addItem(dashboard, events, metrics);
        return nav;
    }

    private SideNav buildConfiguracaoNav() {
        SideNav nav = new SideNav();
        nav.setWidthFull();
        nav.addItem(new SideNavItem("Clusters", ClustersView.class, VaadinIcon.SERVER.create()));
        nav.addItem(buildInfrastructureNavItem());
        if (SecurityUtils.isAdmin()) {
            nav.addItem(new SideNavItem("Users", UserManagementView.class, VaadinIcon.USERS.create()));
        }
        nav.addItem(new SideNavItem("Settings", PlatformSettingsView.class, VaadinIcon.COG.create()));
        return nav;
    }

    private SideNavItem buildInfrastructureNavItem() {
        SideNavItem infrastructure = new SideNavItem("Infrastructure", PersistentVolumesView.class, VaadinIcon.CLOUD.create());
        infrastructure.addItem(new SideNavItem("Persistent Volumes (PV)", PersistentVolumesView.class, VaadinIcon.HARDDRIVE.create()));
        infrastructure.addItem(new SideNavItem("Storage Classes", StorageClassesView.class, VaadinIcon.STORAGE.create()));
        clusterDependentNavItems.add(infrastructure);
        return infrastructure;
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
                    if (drawerPart)  drawerPart.style.width = w + 'px';
                    if (navbarPart)  navbarPart.style.left  = w + 'px';
                    if (contentPart) contentPart.style.marginInlineStart = w + 'px';
                }

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
}
