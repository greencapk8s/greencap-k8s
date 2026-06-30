package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import io.greencap.k8s.kubernetes.dto.OperatorInfo;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Route(value = "developer-experience/operators/installed", layout = MainLayout.class)
@PageTitle("Installed Operators — GreenCap K8s")
@PermitAll
public class InstalledOperatorsView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "Installed Operators";
    private static final String HELP_TEXT  = "Lists all Kubernetes Operators currently installed in this cluster via OLM. " +
            "Each operator is represented by a ClusterServiceVersion (CSV). " +
            "Select an operator and click Uninstall to remove it — CRDs are preserved.";

    private final KubernetesOperatorService operatorService;
    private final ClusterContext clusterContext;

    private final VerticalLayout noClusterMessage;
    private final VerticalLayout clusterErrorMessage;
    private final VerticalLayout olmMissingMessage;

    private final Grid<OperatorInfo> grid = new Grid<>(OperatorInfo.class, false);
    private final List<OperatorInfo> allItems = new ArrayList<>();
    private final ListDataProvider<OperatorInfo> dataProvider = new ListDataProvider<>(allItems);

    public InstalledOperatorsView(KubernetesOperatorService operatorService, ClusterContext clusterContext) {
        this.operatorService = operatorService;
        this.clusterContext  = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage    = UiConstants.buildNoClusterMessage();
        clusterErrorMessage = UiConstants.buildClusterUnreachableMessage();
        olmMissingMessage   = buildOlmMissingMessage();

        buildGrid();

        boolean canUninstall = true;
        List<UiConstants.SelectionAction<OperatorInfo>> selectionActions = List.of(
                UiConstants.SelectionAction.destructive(VaadinIcon.TRASH, "Uninstall", canUninstall,
                        this::openUninstallDialog)
        );

        HorizontalLayout header = UiConstants.buildSectionHeader(
                "Installed Operators", () -> { refresh(); return true; },
                HELP_TITLE, HELP_TEXT, grid, selectionActions);

        add(header, noClusterMessage, clusterErrorMessage, olmMissingMessage, grid);
        expand(grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        clusterErrorMessage.setVisible(false);
        olmMissingMessage.setVisible(false);
        grid.setVisible(hasCluster);
        if (hasCluster) loadAsync(UI.getCurrent());
    }

    @Override
    public void refresh() {
        if (clusterContext.getCluster() != null) loadAsync(UI.getCurrent());
    }

    private void buildGrid() {
        TextField nameFilter = new TextField();
        nameFilter.setPlaceholder("Filter...");
        nameFilter.setClearButtonVisible(true);
        nameFilter.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        nameFilter.setWidthFull();

        dataProvider.setFilter(op ->
                matches(op.displayName(), nameFilter.getValue()) ||
                matches(op.name(), nameFilter.getValue()));
        nameFilter.addValueChangeListener(e -> dataProvider.refreshAll());

        var nameCol = grid.addColumn(OperatorInfo::displayName)
                .setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        grid.addComponentColumn(op -> phaseBadge(op.phase(), op.statusMessage()))
                .setHeader("Status").setWidth("130px").setResizable(true);
        grid.addColumn(OperatorInfo::version).setHeader("Version").setWidth("120px").setResizable(true);
        grid.addColumn(OperatorInfo::channel).setHeader("Channel").setWidth("110px").setResizable(true);
        grid.addColumn(OperatorInfo::catalogSource).setHeader("Catalog").setWidth("160px").setResizable(true);

        grid.setDataProvider(dataProvider);
        grid.setSizeFull();
        grid.setVisible(false);

        HeaderRow filterRow = grid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);
    }

    private void loadAsync(UI ui) {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                if (!operatorService.isOlmInstalled(cluster)) {
                    ui.access(() -> {
                        olmMissingMessage.setVisible(true);
                        grid.setVisible(false);
                    });
                    return;
                }
                List<OperatorInfo> items = operatorService.listInstalled(cluster);
                ui.access(() -> {
                    olmMissingMessage.setVisible(false);
                    clusterErrorMessage.setVisible(false);
                    allItems.clear();
                    allItems.addAll(items);
                    dataProvider.refreshAll();
                    grid.setVisible(true);
                });
            } catch (KubernetesOperationException e) {
                log.debug("Failed to load installed operators for cluster {}: {}", cluster.getName(), e.getMessage());
                ui.access(() -> {
                    allItems.clear();
                    dataProvider.refreshAll();
                    clusterErrorMessage.setVisible(true);
                    grid.setVisible(false);
                });
            }
        }, UiConstants.VIRTUAL_THREADS);
    }

    private void openUninstallDialog(OperatorInfo operator) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Uninstall Operator");
        dialog.setWidth("440px");

        Paragraph warning = new Paragraph(
                "This will remove the Subscription and ClusterServiceVersion for \"" +
                operator.displayName() + "\". Custom Resource Definitions (CRDs) will NOT be deleted.");
        warning.addClassNames(LumoUtility.FontSize.SMALL);

        TextField confirmField = new TextField("Type the operator name to confirm");
        confirmField.setPlaceholder(operator.name());
        confirmField.setWidthFull();

        Button confirmBtn = new Button("Uninstall", e -> {
            if (!operator.name().equals(confirmField.getValue())) {
                confirmField.setInvalid(true);
                return;
            }
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                operatorService.uninstall(cluster, operator.name());
                dialog.close();
                grid.asSingleSelect().clear();
                loadAsync(UI.getCurrent());
                Notification.show("Operator uninstalled successfully.",
                        3000, Notification.Position.BOTTOM_END);
            } catch (KubernetesOperationException ex) {
                Notification n = Notification.show("Failed to uninstall: " + ex.getMessage(),
                        5000, Notification.Position.BOTTOM_END);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        confirmBtn.setEnabled(false);
        confirmField.addValueChangeListener(e ->
                confirmBtn.setEnabled(operator.name().equals(e.getValue())));

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        dialog.add(new VerticalLayout(warning, confirmField) {{ setPadding(false); }});
        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    private Span phaseBadge(String phase, String statusMessage) {
        Span badge = new Span(phase != null ? phase : "Unknown");
        badge.getElement().getThemeList().add("badge");
        if ("Succeeded".equalsIgnoreCase(phase)) {
            badge.getElement().getThemeList().add("success");
        } else if ("Failed".equalsIgnoreCase(phase)) {
            badge.getElement().getThemeList().add("error");
            if (statusMessage != null && !statusMessage.isBlank()) {
                badge.getElement().setAttribute("title", statusMessage);
            }
        } else {
            badge.getElement().getThemeList().add("contrast");
        }
        return badge;
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
