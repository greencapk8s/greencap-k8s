package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
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
import io.greencap.k8s.domain.cluster.Cluster;
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

    private static final String HELP_TITLE = "CronJobs";
    private static final String HELP_TEXT = "A CronJob automatically creates Jobs on a recurring schedule, defined by a cron expression. It can own zero or more active Jobs at any given time.\n\nOn this screen you can: Trigger — create a Job immediately, bypassing the schedule —, Suspend/Resume — pause or resume the creation of new Jobs without affecting those already running — and Delete.";

    private final WorkloadService workloadService;
    private final ClusterContext clusterContext;
    private final GridSelectionMemory selectionMemory;

    private final Grid<CronJobInfo> grid = new Grid<>(CronJobInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<CronJobInfo> allItems = new ArrayList<>();
    private final ListDataProvider<CronJobInfo> dataProvider = new ListDataProvider<>(allItems);

    private boolean canRunNow;
    private boolean canSuspend;
    private boolean canDelete;

    public CronJobsView(WorkloadService workloadService, ClusterContext clusterContext, GridSelectionMemory selectionMemory) {
        this.workloadService = workloadService;
        this.clusterContext = clusterContext;
        this.selectionMemory = selectionMemory;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildGrid();
        UiConstants.configureSingleSelection(grid, selectionMemory, getClass().getSimpleName(), CronJobInfo::name);

        List<UiConstants.SelectionAction<CronJobInfo>> selectionActions = List.of(
                UiConstants.SelectionAction.destructive(VaadinIcon.TRASH, "Delete", canDelete, this::openDeleteDialog),
                UiConstants.SelectionAction.of(VaadinIcon.CODE, "View Manifest",
                        cj -> UI.getCurrent().navigate("yaml/cronjob/" + cj.namespace() + "/" + cj.name()))
        );

        add(UiConstants.buildSectionHeader("CronJobs", this::loadCronJobs, HELP_TITLE, HELP_TEXT, grid, selectionActions), noClusterMessage, grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        grid.setVisible(hasCluster);
        if (hasCluster) {
            loadCronJobs();
        }
    }

    private void buildGrid() {
        canRunNow = true;
        canSuspend = true;
        canDelete  = true;

        var nameCol = grid.addColumn(CronJobInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        grid.addColumn(CronJobInfo::schedule).setHeader("Schedule").setWidth("150px").setResizable(true);
        grid.addComponentColumn(cj -> suspendBadge(cj.suspended()))
                .setHeader("Suspend").setWidth("100px").setResizable(true);
        grid.addColumn(CronJobInfo::active).setHeader("Active").setWidth("80px").setResizable(true);
        grid.addColumn(CronJobInfo::lastScheduleTime).setHeader("Last Schedule").setWidth("120px").setResizable(true);
        grid.addColumn(CronJobInfo::age).setHeader("Age").setWidth("80px").setResizable(true);
        UiConstants.addActionsColumn(grid, 3, this::buildActionButtons);

        grid.setDataProvider(dataProvider);

        TextField nameFilter = buildFilterField();
        dataProvider.setFilter(item -> matches(item.name(), nameFilter.getValue()));
        nameFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, CronJobInfo::name);
        });

        HeaderRow filterRow = grid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);

        grid.setSizeFull();
        grid.setVisible(false);
    }

    private List<Button> buildActionButtons(CronJobInfo cj) {
        Button triggerBtn = buildIconButton(VaadinIcon.FAST_FORWARD, "Trigger Job", canRunNow);
        triggerBtn.addClickListener(e -> openTriggerDialog(cj));

        Icon suspendIcon = cj.suspended() ? VaadinIcon.PLAY.create() : VaadinIcon.PAUSE.create();
        suspendIcon.setSize(UiConstants.ICON_SIZE);
        Button suspendBtn = new Button(suspendIcon);
        suspendBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        suspendBtn.getElement().setAttribute("title", cj.suspended() ? "Resume" : "Suspend");
        suspendBtn.setEnabled(canSuspend);
        suspendBtn.addClickListener(e -> toggleSuspend(cj));

        Button jobsBtn = buildIconButton(VaadinIcon.LIST, "View Jobs", true);
        jobsBtn.addClickListener(e -> UI.getCurrent().navigate("workloads/jobs?cronjob=" + cj.name()));

        return List.of(triggerBtn, suspendBtn, jobsBtn);
    }

    private Button buildIconButton(VaadinIcon icon, String title, boolean enabled) {
        Icon iconElement = icon.create();
        iconElement.setSize(UiConstants.ICON_SIZE);
        Button btn = new Button(iconElement);
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        btn.getElement().setAttribute("title", title);
        btn.setEnabled(enabled);
        return btn;
    }

    private void openTriggerDialog(CronJobInfo cj) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Trigger Job");
        dialog.setText("Trigger a new Job run from CronJob " + cj.name() + "?");
        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");
        dialog.setConfirmText("Trigger");
        dialog.setConfirmButtonTheme("primary");
        dialog.addConfirmListener(e -> {
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                String jobName = workloadService.triggerCronJob(cluster, cj.namespace(), cj.name());
                notify("Job " + jobName + " triggered", NotificationVariant.LUMO_SUCCESS);
                UI.getCurrent().navigate("workloads/jobs?cronjob=" + cj.name());
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }

    private void toggleSuspend(CronJobInfo cj) {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        boolean newSuspendState = !cj.suspended();
        try {
            workloadService.suspendCronJob(cluster, cj.namespace(), cj.name(), newSuspendState);
            notify("CronJob " + cj.name() + (newSuspendState ? " suspended" : " resumed"), NotificationVariant.LUMO_SUCCESS);
            loadCronJobs();
        } catch (KubernetesOperationException ex) {
            notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void openDeleteDialog(CronJobInfo cj) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete CronJob");
        if (cj.active() > 0) {
            dialog.setText("CronJob \"" + cj.name() + "\" has " + cj.active() + " active Job(s). Deleting it will also delete all associated Jobs and Pods. This action cannot be undone.");
        } else {
            dialog.setText("Deleting CronJob \"" + cj.name() + "\" will also delete all associated Jobs and Pods. This action cannot be undone.");
        }
        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                workloadService.deleteCronJob(cluster, cj.namespace(), cj.name());
                notify("CronJob " + cj.name() + " deleted", NotificationVariant.LUMO_SUCCESS);
                loadCronJobs();
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
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
            UiConstants.selectFirstOrPreserve(grid, dataProvider, CronJobInfo::name);
            return true;
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            allItems.clear();
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, CronJobInfo::name);
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
            UiConstants.selectFirstOrPreserve(grid, dataProvider, CronJobInfo::name);
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
