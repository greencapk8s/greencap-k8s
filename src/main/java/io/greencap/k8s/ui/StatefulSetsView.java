package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.AutoScalingService;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.WorkloadService;
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.user.Permission;
import io.greencap.k8s.kubernetes.dto.StatefulSetInfo;
import jakarta.annotation.security.PermitAll;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Route(value = "workloads/statefulsets", layout = MainLayout.class)
@PageTitle("StatefulSets — GreenCap K8s")
@PermitAll
public class StatefulSetsView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "StatefulSets";
    private static final String HELP_TEXT = "A StatefulSet is a Workload that manages a set of replica Pods with stable, unique network identities and stable storage. Pods follow an ordinal naming scheme (<name>-0, <name>-1, ...) and are created, scaled, and deleted in order. It is associated with a headless Service via spec.serviceName, which provides per-pod DNS resolution.\n\nOn this screen you can: Scale — change the desired replica count — Restart — perform a rolling restart, replacing pods one by one in reverse ordinal order — and Rollback — revert to the previous revision.";

    private final WorkloadService workloadService;
    private final AutoScalingService autoScalingService;
    private final ClusterContext clusterContext;
    private final GridSelectionMemory selectionMemory;

    private final Grid<StatefulSetInfo> grid = new Grid<>(StatefulSetInfo.class, false);
    private final VerticalLayout noClusterMessage;
    private final VerticalLayout clusterErrorMessage;

    private final List<StatefulSetInfo> allItems = new ArrayList<>();
    private final ListDataProvider<StatefulSetInfo> dataProvider = new ListDataProvider<>(allItems);

    public StatefulSetsView(WorkloadService workloadService, AutoScalingService autoScalingService,
                             ClusterContext clusterContext, GridSelectionMemory selectionMemory) {
        this.workloadService = workloadService;
        this.autoScalingService = autoScalingService;
        this.clusterContext = clusterContext;
        this.selectionMemory = selectionMemory;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        clusterErrorMessage = UiConstants.buildClusterUnreachableMessage();
        buildGrid();
        UiConstants.configureSingleSelection(grid, selectionMemory, getClass().getSimpleName(), StatefulSetInfo::name);

        boolean canDelete = SecurityUtils.hasPermission(Permission.WORKLOADS_STATEFULSETS_DELETE);
        List<UiConstants.SelectionAction<StatefulSetInfo>> selectionActions = List.of(
                UiConstants.SelectionAction.destructive(VaadinIcon.TRASH, "Delete", canDelete, this::openDeleteDialog),
                UiConstants.SelectionAction.of(VaadinIcon.CODE, "View Manifest",
                        sts -> UI.getCurrent().navigate("yaml/statefulset/" + sts.namespace() + "/" + sts.name()))
        );

        add(UiConstants.buildSectionHeader("StatefulSets", this::loadStatefulSets, HELP_TITLE, HELP_TEXT, grid, selectionActions),
                noClusterMessage, clusterErrorMessage, grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.WORKLOADS_STATEFULSETS_VIEW)) {
            event.forwardTo("");
            return;
        }
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        clusterErrorMessage.setVisible(false);
        grid.setVisible(hasCluster);
        if (hasCluster) {
            loadStatefulSetsAsync(UI.getCurrent());
        }
    }

    private void buildGrid() {
        var nameCol = grid.addColumn(StatefulSetInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        grid.addComponentColumn(sts -> UiConstants.replicasBadge(sts.ready(), sts.desired()))
                .setHeader("Replicas").setWidth("100px").setResizable(true);
        grid.addColumn(StatefulSetInfo::available).setHeader("Available").setWidth("110px").setResizable(true);
        grid.addColumn(StatefulSetInfo::serviceName).setHeader("Service").setFlexGrow(1).setResizable(true);
        var nodesCol = grid.addColumn(StatefulSetInfo::nodes).setHeader("Nodes").setFlexGrow(1).setResizable(true);
        grid.addColumn(StatefulSetInfo::age).setHeader("Age").setWidth("80px").setResizable(true);

        boolean canScale = SecurityUtils.hasPermission(Permission.WORKLOADS_STATEFULSETS_SCALE);
        boolean canRestart = SecurityUtils.hasPermission(Permission.WORKLOADS_STATEFULSETS_RESTART);
        boolean canRollback = SecurityUtils.hasPermission(Permission.WORKLOADS_STATEFULSETS_ROLLBACK);
        UiConstants.addActionsColumn(grid, 3, sts -> {
            Button scaleBtn = buildActionButton(VaadinIcon.EXPAND, "Scale", e -> openScaleDialog(sts));
            scaleBtn.setEnabled(canScale);
            Button restartBtn = buildActionButton(VaadinIcon.ROTATE_RIGHT, "Restart", e -> openRestartDialog(sts));
            restartBtn.setEnabled(canRestart);
            Button rollbackBtn = buildActionButton(VaadinIcon.REPLY, "Rollout Undo", e -> openRollbackDialog(sts));
            rollbackBtn.setEnabled(canRollback);

            return List.of(scaleBtn, restartBtn, rollbackBtn);
        });

        grid.setDataProvider(dataProvider);

        TextField nameFilter = buildFilterField();
        TextField nodesFilter = buildFilterField();

        dataProvider.setFilter(item ->
            matches(item.name(), nameFilter.getValue()) &&
            matches(item.nodes(), nodesFilter.getValue()));

        nameFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, StatefulSetInfo::name);
        });
        nodesFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, StatefulSetInfo::name);
        });

        HeaderRow filterRow = grid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);
        filterRow.getCell(nodesCol).setComponent(nodesFilter);

        grid.setSizeFull();
        grid.setVisible(false);
    }

    private boolean loadStatefulSets() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        String namespace = clusterContext.getNamespace();
        try {
            List<StatefulSetInfo> items = workloadService.listStatefulSets(cluster, namespace);
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, StatefulSetInfo::name);
            clusterErrorMessage.setVisible(false);
            grid.setVisible(true);
            return true;
        } catch (KubernetesOperationException e) {
            allItems.clear();
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, StatefulSetInfo::name);
            clusterErrorMessage.setVisible(true);
            grid.setVisible(false);
            return false;
        }
    }

    private void loadStatefulSetsAsync(UI ui) {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        String namespace = clusterContext.getNamespace();
        CompletableFuture.runAsync(() -> {
            try {
                List<StatefulSetInfo> items = workloadService.listStatefulSets(cluster, namespace);
                ui.access(() -> {
                    allItems.clear();
                    allItems.addAll(items);
                    dataProvider.refreshAll();
                    UiConstants.selectFirstOrPreserve(grid, dataProvider, StatefulSetInfo::name);
                    clusterErrorMessage.setVisible(false);
                    grid.setVisible(true);
                });
            } catch (KubernetesOperationException e) {
                log.debug("Failed to load statefulsets for cluster {}: {}", cluster.getName(), e.getMessage());
                ui.access(() -> {
                    allItems.clear();
                    dataProvider.refreshAll();
                    UiConstants.selectFirstOrPreserve(grid, dataProvider, StatefulSetInfo::name);
                    clusterErrorMessage.setVisible(true);
                    grid.setVisible(false);
                });
            }
        }, UiConstants.VIRTUAL_THREADS);
    }

    @Override
    public void refresh() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        try {
            List<StatefulSetInfo> items = workloadService.listStatefulSets(cluster, clusterContext.getNamespace());
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, StatefulSetInfo::name);
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

    private void openScaleDialog(StatefulSetInfo statefulSet) {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;

        try {
            autoScalingService.findHorizontalScalerForTarget(cluster, statefulSet.namespace(), statefulSet.name())
                    .ifPresentOrElse(
                            hpa -> UI.getCurrent().navigate("autoscaling/horizontalscalers",
                                    new QueryParameters(Map.of("edit", List.of(hpa.name())))),
                            () -> openDirectScaleDialog(statefulSet, cluster)
                    );
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void openDirectScaleDialog(StatefulSetInfo statefulSet, Cluster cluster) {
        IntegerField replicasField = new IntegerField("Replicas");
        replicasField.setMin(0);
        replicasField.setMax(50);
        replicasField.setValue(statefulSet.desired());
        replicasField.setStepButtonsVisible(true);

        Button scaleBtn = new Button("Scale");
        scaleBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        scaleBtn.setEnabled(false);
        replicasField.addValueChangeListener(e ->
                scaleBtn.setEnabled(e.getValue() != null && e.getValue() != statefulSet.desired()));

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Scale — " + statefulSet.name());

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        scaleBtn.addClickListener(e -> {
            dialog.close();
            try {
                workloadService.scaleStatefulSet(cluster, statefulSet.namespace(), statefulSet.name(), replicasField.getValue());
                loadStatefulSets();
                notify("StatefulSet " + statefulSet.name() + " scaled to " + replicasField.getValue() + " replicas", NotificationVariant.LUMO_SUCCESS);
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.add(replicasField);
        dialog.getFooter().add(cancelBtn, scaleBtn);
        dialog.open();
    }

    private void openRestartDialog(StatefulSetInfo statefulSet) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Restart StatefulSet");

        Paragraph message = new Paragraph("Restart " + statefulSet.name() + "? Pods will be replaced one by one, in reverse ordinal order.");

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button restartBtn = new Button("Restart");
        restartBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        restartBtn.addClickListener(e -> {
            dialog.close();
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                workloadService.restartStatefulSet(cluster, statefulSet.namespace(), statefulSet.name());
                loadStatefulSets();
                notify("StatefulSet " + statefulSet.name() + " restarted", NotificationVariant.LUMO_SUCCESS);
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.add(message);
        dialog.getFooter().add(cancelBtn, restartBtn);
        dialog.open();
    }

    private void openDeleteDialog(StatefulSetInfo statefulSet) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete StatefulSet");
        dialog.setText("Deleting StatefulSet \"" + statefulSet.name() + "\" will also remove all its Pods, in reverse ordinal order. PersistentVolumeClaims created from volumeClaimTemplates are retained and not deleted automatically. This action cannot be undone.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                workloadService.deleteStatefulSet(cluster, statefulSet.namespace(), statefulSet.name());
                loadStatefulSets();
                notify("StatefulSet " + statefulSet.name() + " deleted", NotificationVariant.LUMO_SUCCESS);
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }

    private void openRollbackDialog(StatefulSetInfo statefulSet) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Rollout Undo");

        Paragraph message = new Paragraph("Roll back " + statefulSet.name() + " to the previous revision?");

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button undoBtn = new Button("Rollback");
        undoBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        undoBtn.addClickListener(e -> {
            dialog.close();
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                workloadService.rolloutUndoStatefulSet(cluster, statefulSet.namespace(), statefulSet.name());
                loadStatefulSets();
                notify("StatefulSet " + statefulSet.name() + " rolled back to previous revision", NotificationVariant.LUMO_SUCCESS);
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.add(message);
        dialog.getFooter().add(cancelBtn, undoBtn);
        dialog.open();
    }

    private Button buildActionButton(VaadinIcon icon, String title, com.vaadin.flow.component.ComponentEventListener<com.vaadin.flow.component.ClickEvent<Button>> listener) {
        var iconEl = icon.create();
        iconEl.setSize(UiConstants.ICON_SIZE);
        Button btn = new Button(iconEl, listener);
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        btn.getElement().setAttribute("title", title);
        return btn;
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
