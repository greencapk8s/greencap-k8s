package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.user.Permission;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.HelmOperationException;
import io.greencap.k8s.kubernetes.HelmService;
import io.greencap.k8s.kubernetes.dto.HelmReleaseDetails;
import io.greencap.k8s.kubernetes.dto.HelmReleaseDetails;
import io.greencap.k8s.kubernetes.dto.HelmReleaseInfo;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Route(value = "helm/releases", layout = MainLayout.class)
@PageTitle("Helm Releases — GreenCap K8s")
@PermitAll
public class HelmReleasesView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "Helm Releases";
    private static final String HELP_TEXT  = "Lists all Helm releases installed in the active Namespace. " +
            "Select a release to view its Notes, Values and rendered Manifest. " +
            "Use Uninstall to remove a release and all Kubernetes resources it created.";

    private final HelmService helmService;
    private final ClusterContext clusterContext;
    private final GridSelectionMemory selectionMemory;

    private final VerticalLayout noClusterMessage;
    private final VerticalLayout clusterErrorMessage;

    private final Grid<HelmReleaseInfo> grid = new Grid<>(HelmReleaseInfo.class, false);
    private final List<HelmReleaseInfo> allItems = new ArrayList<>();
    private final ListDataProvider<HelmReleaseInfo> dataProvider = new ListDataProvider<>(allItems);

    public HelmReleasesView(HelmService helmService, ClusterContext clusterContext,
                            GridSelectionMemory selectionMemory) {
        this.helmService     = helmService;
        this.clusterContext  = clusterContext;
        this.selectionMemory = selectionMemory;

        setSizeFull();
        setPadding(true);

        noClusterMessage    = UiConstants.buildNoClusterMessage();
        clusterErrorMessage = UiConstants.buildClusterUnreachableMessage();

        buildGrid();
        UiConstants.configureSingleSelection(grid, selectionMemory, getClass().getSimpleName(),
                HelmReleaseInfo::name);

        boolean canUninstall = SecurityUtils.hasPermission(Permission.PROJECT_HELM_UNINSTALL);
        boolean canUpgrade   = SecurityUtils.hasPermission(Permission.PROJECT_HELM_UPGRADE);
        List<UiConstants.SelectionAction<HelmReleaseInfo>> selectionActions = List.of(
                UiConstants.SelectionAction.of(VaadinIcon.INFO_CIRCLE_O, "Details",
                        this::openDetailsDialog),
                UiConstants.SelectionAction.of(VaadinIcon.UPLOAD, "Upgrade", canUpgrade,
                        this::openUpgradeDialog),
                UiConstants.SelectionAction.destructive(VaadinIcon.TRASH, "Uninstall", canUninstall,
                        this::openUninstallDialog)
        );

        add(UiConstants.buildSectionHeader("Helm Releases", this::loadReleases,
                        HELP_TITLE, HELP_TEXT, grid, selectionActions),
                noClusterMessage, clusterErrorMessage, grid);
        expand(grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.PROJECT_HELM_VIEW)) {
            event.forwardTo("");
            return;
        }
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        clusterErrorMessage.setVisible(false);
        grid.setVisible(hasCluster);
        if (hasCluster) loadReleasesAsync(UI.getCurrent());
    }

    @Override
    public void refresh() {
        if (clusterContext.getCluster() == null) return;
        if (clusterErrorMessage.isVisible()) return;
        loadReleasesAsync(UI.getCurrent());
    }

    private boolean loadReleases() {
        Cluster cluster = clusterContext.getCluster();
        String namespace = clusterContext.getNamespace();
        if (cluster == null || namespace == null) return false;
        try {
            List<HelmReleaseInfo> items = helmService.listReleases(cluster, namespace);
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, HelmReleaseInfo::name);
            clusterErrorMessage.setVisible(false);
            return true;
        } catch (HelmOperationException e) {
            log.debug("Failed to load Helm releases: {}", e.getMessage());
            allItems.clear();
            dataProvider.refreshAll();
            clusterErrorMessage.setVisible(true);
            grid.setVisible(false);
            return false;
        }
    }

    private void loadReleasesAsync(UI ui) {
        Cluster cluster = clusterContext.getCluster();
        String namespace = clusterContext.getNamespace();
        if (cluster == null || namespace == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                List<HelmReleaseInfo> items = helmService.listReleases(cluster, namespace);
                ui.access(() -> {
                    clusterErrorMessage.setVisible(false);
                    allItems.clear();
                    allItems.addAll(items);
                    dataProvider.refreshAll();
                    UiConstants.selectFirstOrPreserve(grid, dataProvider, HelmReleaseInfo::name);
                    grid.setVisible(true);
                });
            } catch (HelmOperationException e) {
                log.debug("Failed to load Helm releases: {}", e.getMessage());
                ui.access(() -> {
                    allItems.clear();
                    dataProvider.refreshAll();
                    clusterErrorMessage.setVisible(true);
                    grid.setVisible(false);
                });
            }
        }, UiConstants.VIRTUAL_THREADS);
    }

    private void buildGrid() {
        TextField nameFilter = new TextField();
        nameFilter.setPlaceholder("Filter...");
        nameFilter.setClearButtonVisible(true);
        nameFilter.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        nameFilter.setWidthFull();

        dataProvider.setFilter(r -> matches(r.name(), nameFilter.getValue()));
        nameFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, HelmReleaseInfo::name);
        });

        var nameCol = grid.addColumn(HelmReleaseInfo::name)
                .setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        grid.addColumn(HelmReleaseInfo::chart)
                .setHeader("Chart").setFlexGrow(2).setResizable(true);
        grid.addColumn(HelmReleaseInfo::appVersion)
                .setHeader("App Version").setWidth("120px").setResizable(true);
        grid.addColumn(HelmReleaseInfo::revision)
                .setHeader("Revision").setWidth("90px").setResizable(true);
        grid.addComponentColumn(r -> statusBadge(r.status()))
                .setHeader("Status").setWidth("120px").setResizable(true);
        grid.addColumn(r -> r.updated() != null && r.updated().length() >= 16
                        ? r.updated().substring(0, 16) : r.updated())
                .setHeader("Updated").setFlexGrow(1).setResizable(true);

        grid.setDataProvider(dataProvider);
        grid.setSizeFull();
        grid.setVisible(false);

        HeaderRow filterRow = grid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);
    }

    private void openDetailsDialog(HelmReleaseInfo release) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(release.name() + "  —  " + release.chart());
        dialog.setWidth("860px");
        dialog.setMaxHeight("80vh");

        Tab notesTab    = new Tab("Notes");
        Tab valuesTab   = new Tab("Values");
        Tab manifestTab = new Tab("Manifest");
        Tabs tabs = new Tabs(notesTab, valuesTab, manifestTab);

        Pre notesContent    = buildPreContent("Loading...");
        Pre valuesContent   = buildPreContent("");
        Pre manifestContent = buildPreContent("");

        VerticalLayout notesPane    = pane(notesContent);
        VerticalLayout valuesPane   = pane(valuesContent);
        VerticalLayout manifestPane = pane(manifestContent);

        valuesPane.setVisible(false);
        manifestPane.setVisible(false);

        tabs.addSelectedChangeListener(e -> {
            notesPane.setVisible(tabs.getSelectedTab() == notesTab);
            valuesPane.setVisible(tabs.getSelectedTab() == valuesTab);
            manifestPane.setVisible(tabs.getSelectedTab() == manifestTab);
        });

        VerticalLayout content = new VerticalLayout(tabs, notesPane, valuesPane, manifestPane);
        content.setPadding(false);
        content.setSpacing(false);
        content.setSizeFull();
        dialog.add(content);

        Button closeBtn = new Button("Close", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        dialog.getFooter().add(closeBtn);

        dialog.open();

        // Load details async after dialog is open
        Cluster cluster = clusterContext.getCluster();
        String namespace = clusterContext.getNamespace();
        if (cluster == null || namespace == null) return;

        UI ui = UI.getCurrent();
        CompletableFuture.runAsync(() -> {
            try {
                HelmReleaseDetails details = helmService.getReleaseDetails(cluster, namespace, release.name());
                ui.access(() -> {
                    notesContent.setText(details.notes().isBlank() ? "(no notes)" : details.notes());
                    valuesContent.setText(details.values().isBlank() ? "(no custom values)" : details.values());
                    manifestContent.setText(details.manifest().isBlank() ? "(no manifest)" : details.manifest());
                });
            } catch (HelmOperationException e) {
                ui.access(() -> notesContent.setText("Failed to load details: " + e.getMessage()));
            }
        }, UiConstants.VIRTUAL_THREADS);
    }

    private void openUpgradeDialog(HelmReleaseInfo release) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Upgrade — " + release.name());
        dialog.setWidth("720px");
        dialog.setHeight("520px");
        dialog.setResizable(true);

        CodeMirrorEditor valuesArea = new CodeMirrorEditor();
        valuesArea.setSizeFull();
        valuesArea.setValue("# Loading current values...");
        valuesArea.setReadOnly(true);

        TextField versionField = new TextField("New version (optional)");
        versionField.setWidthFull();
        versionField.setPlaceholder("Leave empty to keep current version");

        VerticalLayout content = new VerticalLayout(valuesArea, versionField);
        content.setSizeFull();
        content.setPadding(false);
        content.setFlexGrow(1, valuesArea);
        dialog.add(content);

        Button confirmBtn = new Button("Upgrade", e -> {
            Cluster cluster = clusterContext.getCluster();
            String namespace = clusterContext.getNamespace();
            if (cluster == null || namespace == null) return;
            try {
                helmService.upgrade(cluster, namespace, release.name(), release.chart(),
                        versionField.getValue().trim().isBlank() ? null : versionField.getValue().trim(),
                        valuesArea.getValue());
                dialog.close();
                loadReleases();
                Notification.show("Release \"" + release.name() + "\" upgraded.",
                        3000, Notification.Position.BOTTOM_END);
            } catch (HelmOperationException ex) {
                Notification n = Notification.show("Failed to upgrade: " + ex.getMessage(),
                        UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        confirmBtn.setEnabled(false);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();

        // Load current values async
        Cluster cluster = clusterContext.getCluster();
        String namespace = clusterContext.getNamespace();
        if (cluster == null || namespace == null) return;
        UI ui = UI.getCurrent();
        CompletableFuture.runAsync(() -> {
            try {
                HelmReleaseDetails details = helmService.getReleaseDetails(cluster, namespace, release.name());
                ui.access(() -> {
                    valuesArea.setValue(details.values().isBlank() ? "# No custom values" : details.values());
                    valuesArea.setReadOnly(false);
                    confirmBtn.setEnabled(true);
                });
            } catch (HelmOperationException e) {
                ui.access(() -> {
                    valuesArea.setValue("# Failed to load values: " + e.getMessage());
                    valuesArea.setReadOnly(false);
                    confirmBtn.setEnabled(true);
                });
            }
        }, UiConstants.VIRTUAL_THREADS);
    }

    private void openUninstallDialog(HelmReleaseInfo release) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Uninstall Helm Release");
        dialog.setWidth("460px");

        com.vaadin.flow.component.html.Paragraph warning = new com.vaadin.flow.component.html.Paragraph(
                "This will remove all Kubernetes resources created by \"" + release.name() +
                "\" (" + release.chart() + "). PersistentVolumeClaims are NOT deleted by default.");
        warning.getStyle().set("font-size", "var(--lumo-font-size-s)");

        TextField confirmField = new TextField("Type the release name to confirm");
        confirmField.setPlaceholder(release.name());
        confirmField.setWidthFull();

        Button confirmBtn = new Button("Uninstall", e -> {
            Cluster cluster = clusterContext.getCluster();
            String namespace = clusterContext.getNamespace();
            if (cluster == null || namespace == null) return;
            try {
                helmService.uninstall(cluster, namespace, release.name());
                dialog.close();
                loadReleases();
                Notification.show("Release \"" + release.name() + "\" uninstalled.",
                        3000, Notification.Position.BOTTOM_END);
            } catch (HelmOperationException ex) {
                Notification n = Notification.show("Failed to uninstall: " + ex.getMessage(),
                        UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        confirmBtn.setEnabled(false);
        confirmField.addValueChangeListener(e ->
                confirmBtn.setEnabled(release.name().equals(e.getValue())));

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        dialog.add(new VerticalLayout(warning, confirmField) {{ setPadding(false); }});
        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    private Span statusBadge(String status) {
        Span badge = new Span(status);
        badge.getElement().getThemeList().add("badge");
        if ("deployed".equalsIgnoreCase(status)) {
            badge.getElement().getThemeList().add("success");
        } else if ("failed".equalsIgnoreCase(status)) {
            badge.getElement().getThemeList().add("error");
        } else {
            badge.getElement().getThemeList().add("contrast");
        }
        return badge;
    }

    private Pre buildPreContent(String text) {
        Pre pre = new Pre(text);
        pre.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("overflow", "auto")
                .set("white-space", "pre-wrap")
                .set("word-break", "break-all")
                .set("margin", "0");
        return pre;
    }

    private VerticalLayout pane(Pre content) {
        VerticalLayout pane = new VerticalLayout(content);
        pane.setPadding(true);
        pane.setSpacing(false);
        pane.setSizeFull();
        pane.getStyle().set("overflow", "auto");
        return pane;
    }

    private boolean matches(String value, String filter) {
        if (filter == null || filter.isBlank()) return true;
        return value != null && value.toLowerCase().contains(filter.toLowerCase());
    }
}
