package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.user.Permission;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.ObservabilityService;
import io.greencap.k8s.kubernetes.WorkloadService;
import io.greencap.k8s.kubernetes.dto.PodInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;

@Route(value = "workloads/pods", layout = MainLayout.class)
@PageTitle("Pods — GreenCap K8s")
@PermitAll
public class PodsView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private final WorkloadService workloadService;
    private final ObservabilityService observabilityService;
    private final ClusterContext clusterContext;

    private final Grid<PodInfo> podGrid = new Grid<>(PodInfo.class, false);
    private final VerticalLayout noClusterMessage;
    private final HorizontalLayout jobFilterBanner = new HorizontalLayout();

    private final List<PodInfo> allItems = new ArrayList<>();
    private final ListDataProvider<PodInfo> dataProvider = new ListDataProvider<>(allItems);

    private String jobFilter = "";
    private TextField nameFilter;
    private TextField statusFilter;

    public PodsView(WorkloadService workloadService, ObservabilityService observabilityService, ClusterContext clusterContext) {
        this.workloadService = workloadService;
        this.observabilityService = observabilityService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildJobFilterBanner();
        buildPodGrid();

        add(UiConstants.buildSectionHeader("Pods", this::loadPods), jobFilterBanner, noClusterMessage, podGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.WORKLOADS_PODS_VIEW)) {
            event.forwardTo("");
            return;
        }

        String jobParam = event.getLocation().getQueryParameters()
                .getParameters().getOrDefault("job", List.of()).stream().findFirst().orElse("");
        applyJobFilter(jobParam);

        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        podGrid.setVisible(hasCluster);
        if (hasCluster) {
            loadPods();
        }
    }

    private void buildJobFilterBanner() {
        jobFilterBanner.setVisible(false);
        jobFilterBanner.setAlignItems(Alignment.CENTER);
        jobFilterBanner.getStyle().set("padding", "4px 0");
    }

    private void applyJobFilter(String jobName) {
        jobFilter = jobName == null ? "" : jobName.trim();
        jobFilterBanner.removeAll();
        jobFilterBanner.setVisible(!jobFilter.isBlank());
        if (!jobFilter.isBlank()) {
            Span label = new Span("Showing pods for Job: ");
            Span jobBadge = new Span(jobFilter);
            jobBadge.getElement().getThemeList().add("badge");
            jobBadge.getElement().getThemeList().add("contrast");

            var closeIcon = VaadinIcon.CLOSE_SMALL.create();
            Button dismissBtn = new Button(closeIcon);
            dismissBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
            dismissBtn.getElement().setAttribute("title", "Clear filter");
            dismissBtn.addClickListener(e -> {
                applyJobFilter("");
                dataProvider.refreshAll();
            });

            jobFilterBanner.add(label, jobBadge, dismissBtn);
        }
        if (dataProvider != null) {
            dataProvider.refreshAll();
        }
    }

    private void buildPodGrid() {
        nameFilter   = buildFilterField();
        statusFilter = buildFilterField();

        var nameCol   = podGrid.addColumn(PodInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        var statusCol = podGrid.addComponentColumn(p -> phaseBadge(p.phase())).setHeader("Status").setWidth("120px").setResizable(true);
        podGrid.addColumn(PodInfo::node).setHeader("Node").setFlexGrow(1).setResizable(true);
        podGrid.addColumn(PodInfo::restarts).setHeader("Restarts").setWidth("90px").setResizable(true);
        podGrid.addColumn(PodInfo::age).setHeader("Age").setWidth("80px").setResizable(true);
        podGrid.addComponentColumn(p -> {
            var manifestIcon = VaadinIcon.CODE.create();
            manifestIcon.setSize(UiConstants.ICON_SIZE);
            Button manifestBtn = new Button(manifestIcon);
            manifestBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            manifestBtn.getElement().setAttribute("title", "View Manifest");
            manifestBtn.addClickListener(e -> UI.getCurrent().navigate("yaml/pod/" + p.namespace() + "/" + p.name()));

            var eventsIcon = VaadinIcon.RECORDS.create();
            eventsIcon.setSize(UiConstants.ICON_SIZE);
            Button eventsBtn = new Button(eventsIcon);
            eventsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            eventsBtn.getElement().setAttribute("title", "Events");
            eventsBtn.addClickListener(e -> EventsDialog.open(observabilityService, clusterContext, "Pod", p.name(), p.namespace()));

            var logsIcon = VaadinIcon.TERMINAL.create();
            logsIcon.setSize(UiConstants.ICON_SIZE);
            Button logsBtn = new Button(logsIcon);
            logsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            logsBtn.getElement().setAttribute("title", "Logs");
            logsBtn.addClickListener(e -> UI.getCurrent().navigate("logs/pod/" + p.namespace() + "/" + p.name()));

            HorizontalLayout actions = new HorizontalLayout(manifestBtn, eventsBtn, logsBtn);
            actions.setSpacing(false);
            return actions;
        }).setHeader("").setWidth("160px").setFlexGrow(0);

        dataProvider.setFilter(item ->
            matches(item.name(), nameFilter.getValue()) &&
            matches(item.phase(), statusFilter.getValue()) &&
            (jobFilter.isBlank() || jobFilter.equals(item.jobName())));

        nameFilter.addValueChangeListener(e -> dataProvider.refreshAll());
        statusFilter.addValueChangeListener(e -> dataProvider.refreshAll());

        podGrid.setDataProvider(dataProvider);

        HeaderRow filterRow = podGrid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);
        filterRow.getCell(statusCol).setComponent(statusFilter);

        podGrid.setSizeFull();
        podGrid.setVisible(false);
    }

    private boolean loadPods() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        String namespace = clusterContext.getNamespace();
        try {
            List<PodInfo> items = workloadService.listPods(cluster, namespace);
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            return true;
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            allItems.clear();
            dataProvider.refreshAll();
            return false;
        }
    }

    private Span phaseBadge(String phase) {
        Span badge = new Span(phase);
        badge.getElement().getThemeList().add("badge");
        switch (phase) {
            case "Running", "Active" -> badge.getElement().getThemeList().add("success");
            case "Pending"           -> badge.getElement().getThemeList().add("contrast");
            case "Failed"            -> badge.getElement().getThemeList().add("error");
            default                  -> {}
        }
        return badge;
    }

    private TextField buildFilterField() {
        TextField field = new TextField();
        field.setPlaceholder("Filter...");
        field.setClearButtonVisible(true);
        field.setWidth("100%");
        field.getElement().getThemeList().add("small");
        return field;
    }

    @Override
    public void refresh() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        try {
            List<PodInfo> items = workloadService.listPods(cluster, clusterContext.getNamespace());
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
        } catch (KubernetesOperationException ignored) {}
    }

    private boolean matches(String value, String filter) {
        return filter == null || filter.isBlank() ||
               (value != null && value.toLowerCase().contains(filter.toLowerCase().trim()));
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
