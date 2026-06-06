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
import io.greencap.k8s.kubernetes.WorkloadService;
import io.greencap.k8s.kubernetes.dto.JobInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;

@Route(value = "workloads/jobs", layout = MainLayout.class)
@PageTitle("Jobs — GreenCap K8s")
@PermitAll
public class JobsView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private final WorkloadService workloadService;
    private final ClusterContext clusterContext;

    private final Grid<JobInfo> grid = new Grid<>(JobInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<JobInfo> allItems = new ArrayList<>();
    private final ListDataProvider<JobInfo> dataProvider = new ListDataProvider<>(allItems);

    public JobsView(WorkloadService workloadService, ClusterContext clusterContext) {
        this.workloadService = workloadService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildGrid();

        add(UiConstants.buildSectionHeader("Jobs", this::loadJobs), noClusterMessage, grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.WORKLOADS_JOBS_VIEW)) {
            event.forwardTo("");
            return;
        }
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        grid.setVisible(hasCluster);
        if (hasCluster) {
            loadJobs();
        }
    }

    private void buildGrid() {
        var nameCol   = grid.addColumn(JobInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        var statusCol = grid.addComponentColumn(job -> statusBadge(job.status()))
                .setHeader("Status").setWidth("120px").setResizable(true);
        grid.addColumn(JobInfo::completions).setHeader("Completions").setWidth("110px").setResizable(true);
        grid.addColumn(JobInfo::duration).setHeader("Duration").setWidth("110px").setResizable(true);
        grid.addColumn(JobInfo::age).setHeader("Age").setWidth("80px").setResizable(true);
        var ownerCol  = grid.addColumn(JobInfo::owner).setHeader("Owner").setFlexGrow(1).setResizable(true);
        grid.addComponentColumn(job -> {
            var icon = VaadinIcon.CODE.create();
            icon.setSize(UiConstants.ICON_SIZE);
            Button btn = new Button(icon);
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            btn.getElement().setAttribute("title", "View Manifest");
            btn.addClickListener(e -> UI.getCurrent().navigate(
                    "yaml/job/" + job.namespace() + "/" + job.name()));
            return btn;
        }).setHeader("").setWidth("60px").setFlexGrow(0);

        grid.setDataProvider(dataProvider);

        TextField nameFilter  = buildFilterField();
        TextField ownerFilter = buildFilterField();

        dataProvider.setFilter(item ->
            matches(item.name(), nameFilter.getValue()) &&
            matches(item.owner(), ownerFilter.getValue()));

        nameFilter.addValueChangeListener(e -> dataProvider.refreshAll());
        ownerFilter.addValueChangeListener(e -> dataProvider.refreshAll());

        HeaderRow filterRow = grid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);
        filterRow.getCell(ownerCol).setComponent(ownerFilter);

        grid.setSizeFull();
        grid.setVisible(false);
    }

    private boolean loadJobs() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        String namespace = clusterContext.getNamespace();
        try {
            List<JobInfo> items = workloadService.listJobs(cluster, namespace);
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

    private Span statusBadge(String status) {
        Span badge = new Span(status);
        badge.getElement().getThemeList().add("badge");
        switch (status) {
            case "Complete"  -> badge.getElement().getThemeList().add("success");
            case "Failed"    -> badge.getElement().getThemeList().add("error");
            case "Suspended" -> badge.getElement().getThemeList().add("contrast");
            default -> {}
        }
        return badge;
    }

    @Override
    public void refresh() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        try {
            List<JobInfo> items = workloadService.listJobs(cluster, clusterContext.getNamespace());
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
        } catch (KubernetesOperationException ignored) {}
    }

    private TextField buildFilterField() {
        TextField field = new TextField();
        field.setPlaceholder("Filter...");
        field.setClearButtonVisible(true);
        field.setWidth("100%");
        field.getElement().getThemeList().add("small");
        return field;
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
