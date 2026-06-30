package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.KubernetesOperatorService;
import io.greencap.k8s.kubernetes.dto.OperatorChannel;
import io.greencap.k8s.kubernetes.dto.OperatorPackage;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Route(value = "developer-experience/operators/catalog", layout = MainLayout.class)
@PageTitle("Operator Catalog — GreenCap K8s")
@PermitAll
public class OperatorCatalogView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "Operator Catalog";
    private static final String HELP_TEXT  = "Browse all Kubernetes Operators available across the cluster's OLM CatalogSources. " +
            "Select an operator and click Install to deploy it via a Subscription. " +
            "The catalog is loaded once per visit — use the Refresh button to reload it.";

    private final KubernetesOperatorService operatorService;
    private final ClusterContext clusterContext;

    private final VerticalLayout noClusterMessage;
    private final VerticalLayout clusterErrorMessage;
    private final VerticalLayout olmMissingMessage;

    private final Grid<OperatorPackage> grid = new Grid<>(OperatorPackage.class, false);
    private final List<OperatorPackage> allItems   = new ArrayList<>();
    private final List<OperatorPackage> shownItems = new ArrayList<>();
    private final ListDataProvider<OperatorPackage> dataProvider = new ListDataProvider<>(shownItems);

    private TextField    nameFilter;
    private ComboBox<String> sourceFilter;
    private final ProgressBar progress = new ProgressBar();

    private boolean loading = false;

    public OperatorCatalogView(KubernetesOperatorService operatorService, ClusterContext clusterContext) {
        this.operatorService = operatorService;
        this.clusterContext  = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage    = UiConstants.buildNoClusterMessage();
        clusterErrorMessage = UiConstants.buildClusterUnreachableMessage();
        olmMissingMessage   = buildOlmMissingMessage();

        buildGrid();

        boolean canInstall = true;
        List<UiConstants.SelectionAction<OperatorPackage>> selectionActions = List.of(
                UiConstants.SelectionAction.of(VaadinIcon.DOWNLOAD, "Install", canInstall,
                        this::openInstallDialog)
        );

        progress.setIndeterminate(true);
        progress.setVisible(false);
        progress.setWidthFull();

        // Manual refresh forces a full reload; auto-refresh timer is a no-op (refresh() below)
        HorizontalLayout header = UiConstants.buildSectionHeader(
                "Operator Catalog", this::forceReload,
                HELP_TITLE, HELP_TEXT, grid, selectionActions);

        add(header, noClusterMessage, clusterErrorMessage, olmMissingMessage, progress, grid);
        expand(grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        clusterErrorMessage.setVisible(false);
        olmMissingMessage.setVisible(false);
        grid.setVisible(hasCluster);

        if (hasCluster && allItems.isEmpty()) {
            loadAsync(UI.getCurrent());
        } else if (hasCluster) {
            applyFilters();
        }
    }

    // Called by the auto-refresh timer in MainLayout — no-op to avoid reloading the full catalog automatically
    @Override
    public void refresh() {}

    private boolean forceReload() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        allItems.clear();
        loadAsync(UI.getCurrent());
        return true;
    }

    private void buildGrid() {
        nameFilter = new TextField();
        nameFilter.setPlaceholder("Filter...");
        nameFilter.setClearButtonVisible(true);
        nameFilter.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        nameFilter.setWidthFull();
        nameFilter.addValueChangeListener(e -> applyFilters());

        sourceFilter = new ComboBox<>();
        sourceFilter.setPlaceholder("All catalogs");
        sourceFilter.setClearButtonVisible(true);
        sourceFilter.setWidth("200px");
        sourceFilter.addValueChangeListener(e -> applyFilters());

        var nameCol   = grid.addColumn(OperatorPackage::displayName)
                .setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        grid.addColumn(OperatorPackage::provider)
                .setHeader("Provider").setWidth("160px").setResizable(true);
        var catalogCol = grid.addColumn(OperatorPackage::catalogSource)
                .setHeader("Catalog").setWidth("200px").setResizable(true);

        grid.setDataProvider(dataProvider);
        grid.setSizeFull();
        grid.setVisible(false);

        HeaderRow filterRow = grid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);
        filterRow.getCell(catalogCol).setComponent(sourceFilter);
    }

    private void loadAsync(UI ui) {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null || loading) return;
        loading = true;

        ui.access(() -> {
            progress.setVisible(true);
            grid.setVisible(false);
        });

        CompletableFuture.runAsync(() -> {
            try {
                List<OperatorPackage> items = operatorService.listCatalog(cluster);
                ui.access(() -> {
                    loading = false;
                    clusterErrorMessage.setVisible(false);
                    olmMissingMessage.setVisible(false);
                    allItems.clear();
                    allItems.addAll(items);

                    List<String> sources = items.stream()
                            .map(OperatorPackage::catalogSource)
                            .distinct()
                            .sorted()
                            .toList();
                    sourceFilter.setItems(sources);

                    applyFilters();
                    progress.setVisible(false);
                    grid.setVisible(true);
                });
            } catch (KubernetesOperationException e) {
                log.debug("Failed to load operator catalog for cluster {}: {}", cluster.getName(), e.getMessage());
                ui.access(() -> {
                    loading = false;
                    allItems.clear();
                    shownItems.clear();
                    dataProvider.refreshAll();
                    progress.setVisible(false);
                    clusterErrorMessage.setVisible(true);
                    grid.setVisible(false);
                });
            }
        }, UiConstants.VIRTUAL_THREADS);
    }

    private void applyFilters() {
        String name   = nameFilter.getValue();
        String source = sourceFilter.getValue();
        shownItems.clear();
        allItems.stream()
                .filter(p -> source == null || source.isBlank() || source.equals(p.catalogSource()))
                .filter(p -> matches(p.displayName(), name) || matches(p.name(), name))
                .forEach(shownItems::add);
        dataProvider.refreshAll();
    }

    private void openInstallDialog(OperatorPackage pkg) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Install " + pkg.displayName());
        dialog.setWidth("420px");

        ComboBox<String> channelSelect = new ComboBox<>("Channel");
        channelSelect.setItems(pkg.channels().stream().map(OperatorChannel::name).toList());
        channelSelect.setValue(pkg.defaultChannel().isBlank() ? null : pkg.defaultChannel());
        channelSelect.setRequired(true);
        channelSelect.setWidthFull();

        Span modeNote = new Span("Install mode: All Namespaces");
        modeNote.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        dialog.add(new VerticalLayout(channelSelect, modeNote) {{ setPadding(false); setSpacing(true); }});

        com.vaadin.flow.component.button.Button confirmBtn = new com.vaadin.flow.component.button.Button("Install", e -> {
            if (channelSelect.getValue() == null) {
                channelSelect.setInvalid(true);
                return;
            }
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                operatorService.install(cluster, pkg.name(), channelSelect.getValue(), pkg.catalogSource());
                dialog.close();
                UI.getCurrent().navigate(InstalledOperatorsView.class);
                Notification.show("Installing " + pkg.displayName() + "...",
                        3000, Notification.Position.BOTTOM_END);
            } catch (KubernetesOperationException ex) {
                Notification n = Notification.show("Failed to install: " + ex.getMessage(),
                        5000, Notification.Position.BOTTOM_END);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        com.vaadin.flow.component.button.Button cancelBtn = new com.vaadin.flow.component.button.Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    private VerticalLayout buildOlmMissingMessage() {
        H3 title = new H3("OLM not installed");
        title.addClassNames(LumoUtility.Margin.Bottom.XSMALL);
        Paragraph text = new Paragraph(
                "Operator Lifecycle Manager (OLM) is not installed in the active cluster.");
        text.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        Paragraph hint = new Paragraph("For minikube: minikube addons enable olm");
        hint.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.MEDIUM);
        VerticalLayout layout = new VerticalLayout(title, text, hint);
        layout.setPadding(true);
        layout.setSpacing(false);
        layout.setVisible(false);
        return layout;
    }

    private boolean matches(String value, String filter) {
        if (filter == null || filter.isBlank()) return true;
        return value != null && value.toLowerCase().contains(filter.toLowerCase());
    }
}
