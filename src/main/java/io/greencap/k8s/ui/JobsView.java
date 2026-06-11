package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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

    private static final String HELP_TITLE = "Jobs";
    private static final String HELP_TEXT = "A Job is a Workload that runs a finite task to completion. It tracks how many Pods succeeded (completions) out of the total desired.\n\nThe status shown (Complete, Failed, Running or Suspended) is derived from the resource's conditions in the cluster. This screen allows you to delete a Job.";

    private final WorkloadService workloadService;
    private final ClusterContext clusterContext;
    private final GridSelectionMemory selectionMemory;

    private final Grid<JobInfo> grid = new Grid<>(JobInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<JobInfo> allItems = new ArrayList<>();
    private final ListDataProvider<JobInfo> dataProvider = new ListDataProvider<>(allItems);

    private TextField nameFilterField;
    private TextField ownerFilterField;

    public JobsView(WorkloadService workloadService, ClusterContext clusterContext, GridSelectionMemory selectionMemory) {
        this.workloadService = workloadService;
        this.clusterContext = clusterContext;
        this.selectionMemory = selectionMemory;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildGrid();
        UiConstants.configureSingleSelection(grid, selectionMemory, getClass().getSimpleName(), JobInfo::name);

        boolean canDelete = SecurityUtils.hasPermission(Permission.WORKLOADS_JOBS_DELETE);
        List<UiConstants.SelectionAction<JobInfo>> selectionActions = List.of(
                UiConstants.SelectionAction.destructive(VaadinIcon.TRASH, "Delete", canDelete, this::openDeleteJobDialog),
                UiConstants.SelectionAction.of(VaadinIcon.CODE, "View Manifest",
                        job -> UI.getCurrent().navigate("yaml/job/" + job.namespace() + "/" + job.name()))
        );

        add(UiConstants.buildSectionHeader("Jobs", this::loadJobs, HELP_TITLE, HELP_TEXT, grid, selectionActions), noClusterMessage, grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.WORKLOADS_JOBS_VIEW)) {
            event.forwardTo("");
            return;
        }

        String cronjobParam = event.getLocation().getQueryParameters()
                .getParameters().getOrDefault("cronjob", List.of()).stream().findFirst().orElse("");
        if (!cronjobParam.isBlank()) {
            ownerFilterField.setValue(cronjobParam);
        }

        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        grid.setVisible(hasCluster);
        if (hasCluster) {
            loadJobs();
        }
    }

    private void buildGrid() {
        nameFilterField  = buildFilterField();
        ownerFilterField = buildFilterField();

        var nameCol  = grid.addColumn(JobInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        var statusCol = grid.addComponentColumn(job -> statusBadge(job.status()))
                .setHeader("Status").setWidth("120px").setResizable(true);
        grid.addColumn(JobInfo::completions).setHeader("Completions").setWidth("110px").setResizable(true);
        grid.addColumn(JobInfo::duration).setHeader("Duration").setWidth("110px").setResizable(true);
        grid.addColumn(JobInfo::age).setHeader("Age").setWidth("80px").setResizable(true);
        var ownerCol = grid.addColumn(JobInfo::owner).setHeader("Owner").setFlexGrow(1).setResizable(true);

        UiConstants.addActionsColumn(grid, 1, job -> {
            var podsIcon = VaadinIcon.LIST.create();
            podsIcon.setSize(UiConstants.ICON_SIZE);
            Button podsBtn = new Button(podsIcon);
            podsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            podsBtn.getElement().setAttribute("title", "View Pods");
            podsBtn.addClickListener(e -> UI.getCurrent().navigate(
                    "workloads/pods?job=" + job.name()));
            return List.of(podsBtn);
        });

        dataProvider.setFilter(item ->
            matches(item.name(), nameFilterField.getValue()) &&
            matches(item.owner(), ownerFilterField.getValue()));

        nameFilterField.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, JobInfo::name);
        });
        ownerFilterField.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, JobInfo::name);
        });

        grid.setDataProvider(dataProvider);

        HeaderRow filterRow = grid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilterField);
        filterRow.getCell(ownerCol).setComponent(ownerFilterField);

        grid.setSizeFull();
        grid.setVisible(false);
    }

    private void openDeleteJobDialog(JobInfo job) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Job");
        dialog.setText("Deleting Job \"" + job.name() + "\" will also remove all its Pods and logs. This action cannot be undone.");
        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                workloadService.deleteJob(cluster, job.namespace(), job.name());
                notify("Job " + job.name() + " deleted", NotificationVariant.LUMO_SUCCESS);
                loadJobs();
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
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
            UiConstants.selectFirstOrPreserve(grid, dataProvider, JobInfo::name);
            return true;
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            allItems.clear();
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, JobInfo::name);
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
            UiConstants.selectFirstOrPreserve(grid, dataProvider, JobInfo::name);
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
