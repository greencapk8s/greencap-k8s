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
import io.greencap.k8s.kubernetes.dto.CronJobInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;

@Route(value = "workloads/cronjobs", layout = MainLayout.class)
@PageTitle("CronJobs — GreenCap K8s")
@PermitAll
public class CronJobsView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private final WorkloadService workloadService;
    private final ClusterContext clusterContext;

    private final Grid<CronJobInfo> grid = new Grid<>(CronJobInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<CronJobInfo> allItems = new ArrayList<>();
    private final ListDataProvider<CronJobInfo> dataProvider = new ListDataProvider<>(allItems);

    public CronJobsView(WorkloadService workloadService, ClusterContext clusterContext) {
        this.workloadService = workloadService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildGrid();

        add(UiConstants.buildSectionHeader("CronJobs", this::loadCronJobs), noClusterMessage, grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.WORKLOADS_CRONJOBS_VIEW)) {
            event.forwardTo("");
            return;
        }
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        grid.setVisible(hasCluster);
        if (hasCluster) {
            loadCronJobs();
        }
    }

    private void buildGrid() {
        var nameCol = grid.addColumn(CronJobInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        grid.addColumn(CronJobInfo::schedule).setHeader("Schedule").setWidth("150px").setResizable(true);
        grid.addComponentColumn(cj -> suspendBadge(cj.suspended()))
                .setHeader("Suspend").setWidth("100px").setResizable(true);
        grid.addColumn(CronJobInfo::active).setHeader("Active").setWidth("80px").setResizable(true);
        grid.addColumn(CronJobInfo::lastScheduleTime).setHeader("Last Schedule").setWidth("120px").setResizable(true);
        grid.addColumn(CronJobInfo::age).setHeader("Age").setWidth("80px").setResizable(true);
        grid.addComponentColumn(cj -> {
            var icon = VaadinIcon.CODE.create();
            icon.setSize(UiConstants.ICON_SIZE);
            Button btn = new Button(icon);
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            btn.getElement().setAttribute("title", "View Manifest");
            btn.addClickListener(e -> UI.getCurrent().navigate(
                    "yaml/cronjob/" + cj.namespace() + "/" + cj.name()));
            return btn;
        }).setHeader("").setWidth("60px").setFlexGrow(0);

        grid.setDataProvider(dataProvider);

        TextField nameFilter = buildFilterField();
        dataProvider.setFilter(item -> matches(item.name(), nameFilter.getValue()));
        nameFilter.addValueChangeListener(e -> dataProvider.refreshAll());

        HeaderRow filterRow = grid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);

        grid.setSizeFull();
        grid.setVisible(false);
    }

    private boolean loadCronJobs() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        String namespace = clusterContext.getNamespace();
        try {
            List<CronJobInfo> items = workloadService.listCronJobs(cluster, namespace);
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

    private Span suspendBadge(boolean suspended) {
        if (!suspended) return new Span();
        Span badge = new Span("Suspended");
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add("contrast");
        return badge;
    }

    @Override
    public void refresh() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        try {
            List<CronJobInfo> items = workloadService.listCronJobs(cluster, clusterContext.getNamespace());
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
