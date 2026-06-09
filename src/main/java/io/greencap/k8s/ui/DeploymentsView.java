package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
import io.greencap.k8s.kubernetes.ObservabilityService;
import io.greencap.k8s.kubernetes.WorkloadService;
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.user.Permission;
import io.greencap.k8s.kubernetes.dto.DeploymentInfo;
import jakarta.annotation.security.PermitAll;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Route(value = "workloads/deployments", layout = MainLayout.class)
@PageTitle("Deployments — GreenCap K8s")
@PermitAll
public class DeploymentsView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "Deployments";
    private static final String HELP_TEXT = "A Deployment is a Workload that manages a set of replica Pods through one or more ReplicaSets. It ensures that the desired number of replicas is always running.\n\nOn this screen you can: Scale — change the desired replica count — and Restart — perform a rolling restart, replacing pods one by one without service interruption.";

    private final WorkloadService workloadService;
    private final AutoScalingService autoScalingService;
    private final ObservabilityService observabilityService;
    private final ClusterContext clusterContext;

    private final Grid<DeploymentInfo> deployGrid = new Grid<>(DeploymentInfo.class, false);
    private final VerticalLayout noClusterMessage;
    private final VerticalLayout clusterErrorMessage;

    private final List<DeploymentInfo> allItems = new ArrayList<>();
    private final ListDataProvider<DeploymentInfo> dataProvider = new ListDataProvider<>(allItems);

    public DeploymentsView(WorkloadService workloadService, AutoScalingService autoScalingService,
                           ObservabilityService observabilityService, ClusterContext clusterContext) {
        this.workloadService = workloadService;
        this.autoScalingService = autoScalingService;
        this.observabilityService = observabilityService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        clusterErrorMessage = UiConstants.buildClusterUnreachableMessage();
        buildDeployGrid();

        add(UiConstants.buildSectionHeader("Deployments", this::loadDeployments, HELP_TITLE, HELP_TEXT), noClusterMessage, clusterErrorMessage, deployGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.WORKLOADS_DEPLOYMENTS_VIEW)) {
            event.forwardTo("");
            return;
        }
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        clusterErrorMessage.setVisible(false);
        deployGrid.setVisible(hasCluster);
        if (hasCluster) {
            loadDeploymentsAsync(UI.getCurrent());
        }
    }

    private void buildDeployGrid() {
        var nameCol = deployGrid.addColumn(DeploymentInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        deployGrid.addComponentColumn(d -> replicasBadge(d.ready(), d.desired()))
                .setHeader("Replicas").setWidth("100px").setResizable(true);
        deployGrid.addColumn(DeploymentInfo::available).setHeader("Available").setWidth("110px").setResizable(true);
        deployGrid.addColumn(DeploymentInfo::age).setHeader("Age").setWidth("80px").setResizable(true);
        boolean canScale = SecurityUtils.hasPermission(Permission.WORKLOADS_DEPLOYMENTS_SCALE);
        boolean canRestart = SecurityUtils.hasPermission(Permission.WORKLOADS_DEPLOYMENTS_RESTART);
        boolean canRollback = SecurityUtils.hasPermission(Permission.WORKLOADS_DEPLOYMENTS_ROLLBACK);
        boolean canDelete = SecurityUtils.hasPermission(Permission.WORKLOADS_DEPLOYMENTS_DELETE);
        deployGrid.addComponentColumn(d -> {
            Button manifestBtn = buildActionButton(VaadinIcon.CODE, "View Manifest",
                    e -> UI.getCurrent().navigate("yaml/deployment/" + d.namespace() + "/" + d.name()));
            Button eventsBtn = buildActionButton(VaadinIcon.RECORDS, "Events",
                    e -> EventsDialog.open(observabilityService, clusterContext, "Deployment", d.name(), d.namespace()));
            Button scaleBtn = buildActionButton(VaadinIcon.EXPAND, "Scale", e -> openScaleDialog(d));
            scaleBtn.setEnabled(canScale);
            Button restartBtn = buildActionButton(VaadinIcon.ROTATE_RIGHT, "Restart", e -> openRestartDialog(d));
            restartBtn.setEnabled(canRestart);
            Button rollbackBtn = buildActionButton(VaadinIcon.REPLY, "Rollout Undo", e -> openRollbackDialog(d));
            rollbackBtn.setEnabled(canRollback);
            Button deleteBtn = buildActionButton(VaadinIcon.TRASH, "Delete", e -> openDeleteDialog(d));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteBtn.setEnabled(canDelete);

            HorizontalLayout actions = new HorizontalLayout(scaleBtn, restartBtn, rollbackBtn, deleteBtn, manifestBtn, eventsBtn);
            actions.setSpacing(false);
            return actions;
        }).setHeader("").setWidth(UiConstants.actionsColumnWidth(6)).setFlexGrow(0);

        deployGrid.setDataProvider(dataProvider);

        TextField nameFilter = buildFilterField();

        dataProvider.setFilter(item -> matches(item.name(), nameFilter.getValue()));

        nameFilter.addValueChangeListener(e -> dataProvider.refreshAll());

        HeaderRow filterRow = deployGrid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);

        deployGrid.setSizeFull();
        deployGrid.setVisible(false);
    }

    private boolean loadDeployments() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        String namespace = clusterContext.getNamespace();
        try {
            List<DeploymentInfo> items = workloadService.listDeployments(cluster, namespace);
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            clusterErrorMessage.setVisible(false);
            deployGrid.setVisible(true);
            return true;
        } catch (KubernetesOperationException e) {
            allItems.clear();
            dataProvider.refreshAll();
            clusterErrorMessage.setVisible(true);
            deployGrid.setVisible(false);
            return false;
        }
    }

    private void loadDeploymentsAsync(UI ui) {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        String namespace = clusterContext.getNamespace();
        CompletableFuture.runAsync(() -> {
            try {
                List<DeploymentInfo> items = workloadService.listDeployments(cluster, namespace);
                ui.access(() -> {
                    allItems.clear();
                    allItems.addAll(items);
                    dataProvider.refreshAll();
                    clusterErrorMessage.setVisible(false);
                    deployGrid.setVisible(true);
                });
            } catch (KubernetesOperationException e) {
                log.debug("Failed to load deployments for cluster {}: {}", cluster.getName(), e.getMessage());
                ui.access(() -> {
                    allItems.clear();
                    dataProvider.refreshAll();
                    clusterErrorMessage.setVisible(true);
                    deployGrid.setVisible(false);
                });
            }
        }, UiConstants.VIRTUAL_THREADS);
    }

    private Span replicasBadge(int ready, int desired) {
        Span badge = new Span(ready + "/" + desired);
        badge.getElement().getThemeList().add("badge");
        if (ready >= desired && desired > 0) {
            badge.getElement().getThemeList().add("success");
        } else if (ready == 0) {
            badge.getElement().getThemeList().add("error");
        } else {
            badge.getElement().getThemeList().add("contrast");
        }
        return badge;
    }

    @Override
    public void refresh() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        try {
            List<DeploymentInfo> items = workloadService.listDeployments(cluster, clusterContext.getNamespace());
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

    private void openScaleDialog(DeploymentInfo deployment) {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;

        try {
            autoScalingService.findHorizontalScalerForDeployment(cluster, deployment.namespace(), deployment.name())
                    .ifPresentOrElse(
                            hpa -> UI.getCurrent().navigate("autoscaling/horizontalscalers",
                                    new QueryParameters(Map.of("edit", List.of(hpa.name())))),
                            () -> openDirectScaleDialog(deployment, cluster)
                    );
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void openDirectScaleDialog(DeploymentInfo deployment, Cluster cluster) {
        IntegerField replicasField = new IntegerField("Replicas");
        replicasField.setMin(0);
        replicasField.setMax(50);
        replicasField.setValue(deployment.desired());
        replicasField.setStepButtonsVisible(true);

        Button scaleBtn = new Button("Scale");
        scaleBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        scaleBtn.setEnabled(false);
        replicasField.addValueChangeListener(e ->
                scaleBtn.setEnabled(e.getValue() != null && e.getValue() != deployment.desired()));

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Scale — " + deployment.name());

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        scaleBtn.addClickListener(e -> {
            dialog.close();
            try {
                workloadService.scaleDeployment(cluster, deployment.namespace(), deployment.name(), replicasField.getValue());
                loadDeployments();
                notify("Deployment " + deployment.name() + " scaled to " + replicasField.getValue() + " replicas", NotificationVariant.LUMO_SUCCESS);
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.add(replicasField);
        dialog.getFooter().add(cancelBtn, scaleBtn);
        dialog.open();
    }

    private void openRestartDialog(DeploymentInfo deployment) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Restart Deployment");

        Paragraph message = new Paragraph("Restart " + deployment.name() + "? Pods will be replaced one by one.");

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button restartBtn = new Button("Restart");
        restartBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        restartBtn.addClickListener(e -> {
            dialog.close();
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                workloadService.restartDeployment(cluster, deployment.namespace(), deployment.name());
                loadDeployments();
                notify("Deployment " + deployment.name() + " restarted", NotificationVariant.LUMO_SUCCESS);
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.add(message);
        dialog.getFooter().add(cancelBtn, restartBtn);
        dialog.open();
    }

    private void openDeleteDialog(DeploymentInfo deployment) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Deployment");
        dialog.setText("Deleting this Deployment will also remove all its ReplicaSets and Pods. This action cannot be undone.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                workloadService.deleteDeployment(cluster, deployment.namespace(), deployment.name());
                loadDeployments();
                notify("Deployment " + deployment.name() + " deleted", NotificationVariant.LUMO_SUCCESS);
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }

    private void openRollbackDialog(DeploymentInfo deployment) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Rollout Undo");

        Paragraph message = new Paragraph("Roll back " + deployment.name() + " to the previous revision?");

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button undoBtn = new Button("Rollout Undo");
        undoBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        undoBtn.addClickListener(e -> {
            dialog.close();
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                workloadService.rolloutUndoDeployment(cluster, deployment.namespace(), deployment.name());
                loadDeployments();
                notify("Deployment " + deployment.name() + " rolled back to previous revision", NotificationVariant.LUMO_SUCCESS);
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
