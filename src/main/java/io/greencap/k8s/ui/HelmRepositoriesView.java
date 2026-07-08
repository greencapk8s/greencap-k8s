package io.greencap.k8s.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
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
import io.greencap.k8s.domain.helm.HelmRepository;
import io.greencap.k8s.domain.helm.HelmRepositoryService;
import io.greencap.k8s.kubernetes.ClusterContext;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Route(value = "helm/repositories", layout = MainLayout.class)
@PageTitle("Helm Repositories — GreenCap K8s")
@PermitAll
public class HelmRepositoriesView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "Helm Repositories";
    private static final String HELP_TEXT  = "Configure Helm chart repositories for the active Cluster. " +
            "Repositories are re-added before every install or upgrade operation. " +
            "Use the name as the alias when referencing charts (e.g. bitnami/nginx).";

    private final HelmRepositoryService repositoryService;
    private final ClusterContext clusterContext;

    private final VerticalLayout noClusterMessage;

    private final Grid<HelmRepository> grid = new Grid<>(HelmRepository.class, false);
    private final List<HelmRepository> allItems = new ArrayList<>();
    private final ListDataProvider<HelmRepository> dataProvider = new ListDataProvider<>(allItems);

    public HelmRepositoriesView(HelmRepositoryService repositoryService, ClusterContext clusterContext) {
        this.repositoryService = repositoryService;
        this.clusterContext    = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();

        buildGrid();

        boolean canInstall = true;

        Button addBtn = new Button("Add Repository", VaadinIcon.PLUS.create());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        addBtn.setVisible(canInstall);
        addBtn.addClickListener(e -> openAddDialog());

        List<UiConstants.SelectionAction<HelmRepository>> selectionActions = List.of(
                UiConstants.SelectionAction.destructive(VaadinIcon.TRASH, "Remove", canInstall,
                        this::confirmRemove)
        );

        add(UiConstants.buildSectionHeader("Helm Repositories", this::loadRepositories,
                        HELP_TITLE, HELP_TEXT, grid, selectionActions, canInstall ? List.of(addBtn) : List.of()),
                noClusterMessage, grid);
        expand(grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        grid.setVisible(hasCluster);
        if (hasCluster) loadRepositories();
    }

    @Override
    public void refresh() {
        loadRepositories();
    }

    private boolean loadRepositories() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        allItems.clear();
        allItems.addAll(repositoryService.listRepositories(cluster));
        dataProvider.refreshAll();
        return true;
    }

    private void buildGrid() {
        grid.addColumn(HelmRepository::getName).setHeader("Name").setSortable(true).setWidth("200px").setResizable(true);
        grid.addColumn(HelmRepository::getUrl).setHeader("URL").setFlexGrow(1).setResizable(true);
        grid.setDataProvider(dataProvider);
        grid.setSizeFull();
        grid.setVisible(false);
    }

    private void openAddDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Helm Repository");
        dialog.setWidth("460px");

        TextField nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setWidthFull();
        nameField.setHelperText("Alias used to reference charts (e.g. bitnami). No spaces allowed.");
        nameField.setPattern("[a-zA-Z0-9_-]+");

        TextField urlField = new TextField("URL");
        urlField.setRequired(true);
        urlField.setWidthFull();
        urlField.setPlaceholder("https://charts.example.com");

        FormLayout form = new FormLayout(nameField, urlField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        dialog.add(form);

        Button confirmBtn = new Button("Add", e -> {
            if (nameField.getValue().isBlank() || urlField.getValue().isBlank()) return;
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                repositoryService.addRepository(cluster, nameField.getValue().trim(), urlField.getValue().trim());
                dialog.close();
                loadRepositories();
                Notification.show("Repository \"" + nameField.getValue() + "\" added.",
                        3000, Notification.Position.BOTTOM_END);
            } catch (IllegalArgumentException ex) {
                Notification n = Notification.show(ex.getMessage(), 4000, Notification.Position.BOTTOM_END);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
        nameField.focus();
    }

    private void confirmRemove(HelmRepository repo) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Remove Repository");
        dialog.setText("Remove repository \"" + repo.getName() + "\" (" + repo.getUrl() + ")?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Remove");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            repositoryService.removeRepository(cluster, repo.getName());
            loadRepositories();
            Notification.show("Repository \"" + repo.getName() + "\" removed.",
                    3000, Notification.Position.BOTTOM_END);
        });
        dialog.open();
    }
}
