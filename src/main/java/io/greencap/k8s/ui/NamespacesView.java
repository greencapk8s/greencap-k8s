package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
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
import io.greencap.k8s.kubernetes.NamespaceService;
import io.greencap.k8s.kubernetes.dto.NamespaceInfo;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Route(value = "global/namespaces", layout = MainLayout.class)
@PageTitle("Namespaces — GreenCap K8s")
@PermitAll
public class NamespacesView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "Namespaces";
    private static final String HELP_TEXT = "A Namespace is a logical isolation unit within a Cluster that groups Workloads. " +
            "Deleting a Namespace permanently removes all resources inside it — Pods, Deployments, Services, and more.";

    private static final Set<String> PROTECTED_NAMESPACES = Set.of(
            "kube-system", "kube-public", "kube-node-lease", "default"
    );

    private final NamespaceService namespaceService;
    private final ClusterContext clusterContext;
    private final GridSelectionMemory selectionMemory;

    private final Grid<NamespaceInfo> grid = new Grid<>(NamespaceInfo.class, false);
    private final VerticalLayout noClusterMessage;
    private final VerticalLayout clusterErrorMessage;

    private final List<NamespaceInfo> allItems = new ArrayList<>();
    private final ListDataProvider<NamespaceInfo> dataProvider = new ListDataProvider<>(allItems);

    private TextField nameFilter;

    public NamespacesView(NamespaceService namespaceService, ClusterContext clusterContext,
                          GridSelectionMemory selectionMemory) {
        this.namespaceService = namespaceService;
        this.clusterContext = clusterContext;
        this.selectionMemory = selectionMemory;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        clusterErrorMessage = UiConstants.buildClusterUnreachableMessage();
        buildGrid();
        UiConstants.configureSingleSelection(grid, selectionMemory, getClass().getSimpleName(), NamespaceInfo::name);

        boolean canDelete = SecurityUtils.hasPermission(Permission.GLOBAL_NAMESPACES_DELETE);
        boolean canWrite  = SecurityUtils.hasPermission(Permission.GLOBAL_NAMESPACES_WRITE);

        List<UiConstants.SelectionAction<NamespaceInfo>> selectionActions = List.of(
                UiConstants.SelectionAction.destructive(VaadinIcon.TRASH, "Delete", canDelete, this::openDeleteDialog)
        );

        List<Button> extraButtons = new ArrayList<>();
        if (canWrite) {
            Button createBtn = new Button("Create Namespace", VaadinIcon.PLUS.create());
            createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            createBtn.addClickListener(e -> openCreateDialog());
            extraButtons.add(createBtn);
        }

        add(UiConstants.buildSectionHeader("Namespaces", this::loadNamespaces, HELP_TITLE, HELP_TEXT,
                        grid, selectionActions, extraButtons),
                noClusterMessage, clusterErrorMessage, grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.GLOBAL_NAMESPACES_VIEW)) {
            event.forwardTo("");
            return;
        }

        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        clusterErrorMessage.setVisible(false);
        grid.setVisible(hasCluster);
        if (hasCluster) {
            loadNamespacesAsync(UI.getCurrent());
        }
    }

    private void buildGrid() {
        nameFilter = buildFilterField();

        var nameCol   = grid.addColumn(NamespaceInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        var statusCol = grid.addComponentColumn(ns -> phaseBadge(ns.phase())).setHeader("Status").setWidth("120px").setResizable(true);
        grid.addColumn(NamespaceInfo::podCount).setHeader("Pods").setWidth("80px").setResizable(true);
        grid.addColumn(NamespaceInfo::deploymentCount).setHeader("Deployments").setWidth("110px").setResizable(true);
        grid.addColumn(NamespaceInfo::serviceCount).setHeader("Services").setWidth("90px").setResizable(true);
        grid.addColumn(NamespaceInfo::age).setHeader("Age").setWidth("80px").setResizable(true);

        dataProvider.setFilter(item -> matches(item.name(), nameFilter.getValue()));
        nameFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, NamespaceInfo::name);
        });

        grid.setDataProvider(dataProvider);

        HeaderRow filterRow = grid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);

        grid.setSizeFull();
        grid.setVisible(false);
    }

    private boolean loadNamespaces() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        try {
            List<NamespaceInfo> items = namespaceService.listNamespacesWithCounts(cluster);
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, NamespaceInfo::name);
            clusterErrorMessage.setVisible(false);
            grid.setVisible(true);
            return true;
        } catch (KubernetesOperationException e) {
            allItems.clear();
            dataProvider.refreshAll();
            clusterErrorMessage.setVisible(true);
            grid.setVisible(false);
            return false;
        }
    }

    private void loadNamespacesAsync(UI ui) {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                List<NamespaceInfo> items = namespaceService.listNamespacesWithCounts(cluster);
                ui.access(() -> {
                    allItems.clear();
                    allItems.addAll(items);
                    dataProvider.refreshAll();
                    UiConstants.selectFirstOrPreserve(grid, dataProvider, NamespaceInfo::name);
                    clusterErrorMessage.setVisible(false);
                    grid.setVisible(true);
                });
            } catch (KubernetesOperationException e) {
                log.debug("Failed to load namespaces for cluster {}: {}", cluster.getName(), e.getMessage());
                ui.access(() -> {
                    allItems.clear();
                    dataProvider.refreshAll();
                    clusterErrorMessage.setVisible(true);
                    grid.setVisible(false);
                });
            }
        }, UiConstants.VIRTUAL_THREADS);
    }

    private void openCreateDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create Namespace");
        dialog.setWidth("400px");

        TextField nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setWidthFull();
        nameField.setHelperText("Lowercase letters, numbers, and hyphens only (e.g. my-app)");
        nameField.setPattern("[a-z0-9][a-z0-9\\-]*[a-z0-9]|[a-z0-9]");

        FormLayout form = new FormLayout(nameField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        dialog.add(form);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        Button createBtn = new Button("Create", e -> {
            String name = nameField.getValue().trim();
            if (name.isEmpty()) {
                nameField.setErrorMessage("Name is required");
                nameField.setInvalid(true);
                return;
            }
            if (!name.matches("[a-z0-9][a-z0-9\\-]*[a-z0-9]|[a-z0-9]")) {
                nameField.setErrorMessage("Must be lowercase letters, numbers, and hyphens only");
                nameField.setInvalid(true);
                return;
            }
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                namespaceService.createNamespace(cluster, name);
                dialog.close();
                loadNamespaces();
                refreshMainLayoutNamespaces();
                notify("Namespace " + name + " created", NotificationVariant.LUMO_SUCCESS);
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        dialog.getFooter().add(cancelBtn, createBtn);
        dialog.open();
        nameField.focus();
    }

    private void openDeleteDialog(NamespaceInfo namespace) {
        if (PROTECTED_NAMESPACES.contains(namespace.name())) {
            notify("System namespace \"" + namespace.name() + "\" cannot be deleted", NotificationVariant.LUMO_ERROR);
            return;
        }
        if ("Terminating".equals(namespace.phase())) {
            notify("Namespace \"" + namespace.name() + "\" is already being deleted", NotificationVariant.LUMO_CONTRAST);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Delete Namespace");
        dialog.setWidth("480px");

        Span warning = new Span("Deleting namespace \"" + namespace.name() + "\" will permanently remove ALL resources " +
                "inside it — Pods, Deployments, Services, ConfigMaps, Secrets, PersistentVolumeClaims, and more. " +
                "This action cannot be undone.");
        warning.getStyle().set("color", "var(--lumo-error-text-color)");

        Span instruction = new Span("Type the namespace name to confirm:");
        instruction.getStyle().set("margin-top", "var(--lumo-space-m)").set("display", "block");

        TextField confirmField = new TextField();
        confirmField.setWidthFull();
        confirmField.setPlaceholder(namespace.name());

        VerticalLayout content = new VerticalLayout(warning, instruction, confirmField);
        content.setPadding(false);
        content.setSpacing(true);
        dialog.add(content);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        Button deleteBtn = new Button("Delete", e -> {
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                namespaceService.deleteNamespace(cluster, namespace.name());
                dialog.close();
                allItems.remove(namespace);
                dataProvider.refreshAll();
                UiConstants.selectFirstOrPreserve(grid, dataProvider, NamespaceInfo::name);
                if (namespace.name().equals(clusterContext.getNamespace())) {
                    clusterContext.setNamespace(null);
                }
                refreshMainLayoutNamespaces();
                notify("Namespace " + namespace.name() + " deleted", NotificationVariant.LUMO_SUCCESS);
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        deleteBtn.setEnabled(false);

        confirmField.addValueChangeListener(e ->
                deleteBtn.setEnabled(namespace.name().equals(e.getValue().trim())));

        dialog.getFooter().add(cancelBtn, deleteBtn);
        dialog.open();
        confirmField.focus();
    }

    private Span phaseBadge(String phase) {
        Span badge = new Span(phase);
        badge.getElement().getThemeList().add("badge");
        switch (phase) {
            case "Active"      -> badge.getElement().getThemeList().add("success");
            case "Terminating" -> badge.getElement().getThemeList().add("error");
            default            -> badge.getElement().getThemeList().add("contrast");
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
            List<NamespaceInfo> items = namespaceService.listNamespacesWithCounts(cluster);
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, NamespaceInfo::name);
        } catch (KubernetesOperationException ignored) {}
    }

    private void refreshMainLayoutNamespaces() {
        UI.getCurrent().getChildren()
                .filter(c -> c instanceof MainLayout)
                .map(c -> (MainLayout) c)
                .findFirst()
                .ifPresent(MainLayout::refreshClusterState);
    }

    private boolean matches(String value, String filter) {
        return filter == null || filter.isBlank() ||
                (value != null && value.toLowerCase().contains(filter.toLowerCase().trim()));
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS,
                Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
