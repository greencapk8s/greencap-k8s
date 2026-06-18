package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.user.Permission;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.RegistryMaintenanceService;
import io.greencap.k8s.kubernetes.RegistryService;
import io.greencap.k8s.kubernetes.dto.BuildRequest;
import io.greencap.k8s.kubernetes.dto.RepositoryInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Route(value = "registry", layout = MainLayout.class)
@PageTitle("Container Registry — GreenCap K8s")
@PermitAll
public class RegistryView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "Container Registry";
    private static final String HELP_TEXT = "The Registry is the container registry running inside the active Cluster (the Service \"registry\" in the \"kube-system\" Namespace), reached via a Kubernetes API port-forward — no separate credentials needed.\n\nA Repository is a named collection of image versions (e.g. \"greencap-demo/backend\"). Each Tag is a named reference to a specific image version (e.g. \"latest\", \"v1.2.3\"), with its digest, size and creation date.\n\nSelecting a Repository and clicking \"Remove Repository\" permanently deletes all of its Tags and runs garbage collection on the Registry. This action cannot be undone.";

    private static final String EMPTY_REGISTRY_MESSAGE =
            "No repositories found. Make sure the Service \"registry\" in the \"kube-system\" namespace is available on this Cluster.";

    private static final Pattern GIT_URL_PATTERN = Pattern.compile("^https?://\\S+$");
    private static final Pattern REPOSITORY_PATTERN =
            Pattern.compile("^[a-z0-9]+((\\.|_|__|-+)[a-z0-9]+)*(/[a-z0-9]+((\\.|_|__|-+)[a-z0-9]+)*)*$");
    private static final Pattern TAG_PATTERN = Pattern.compile("^[a-zA-Z0-9_][a-zA-Z0-9._-]{0,127}$");

    private final RegistryService registryService;
    private final RegistryMaintenanceService registryMaintenanceService;
    private final ClusterContext clusterContext;
    private final GridSelectionMemory selectionMemory;

    private final Grid<RepositoryInfo> grid = new Grid<>(RepositoryInfo.class, false);
    private final VerticalLayout noClusterMessage;
    private final VerticalLayout emptyRegistryMessage;

    private final List<RepositoryInfo> allItems = new ArrayList<>();
    private final ListDataProvider<RepositoryInfo> dataProvider = new ListDataProvider<>(allItems);
    private final Set<String> deletedRepositoryNames = new HashSet<>();

    public RegistryView(RegistryService registryService, RegistryMaintenanceService registryMaintenanceService,
                         ClusterContext clusterContext, GridSelectionMemory selectionMemory) {
        this.registryService = registryService;
        this.registryMaintenanceService = registryMaintenanceService;
        this.clusterContext = clusterContext;
        this.selectionMemory = selectionMemory;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        emptyRegistryMessage = buildEmptyRegistryMessage();
        buildGrid();
        UiConstants.configureSingleSelection(grid, selectionMemory, "registry", RepositoryInfo::name);

        boolean canDelete = SecurityUtils.hasPermission(Permission.GLOBAL_REGISTRY_DELETE);
        List<UiConstants.SelectionAction<RepositoryInfo>> selectionActions = List.of(
                UiConstants.SelectionAction.destructive(VaadinIcon.TRASH, "Remove Repository", canDelete, this::openDeleteRepositoryDialog)
        );

        add(UiConstants.buildSectionHeader("Container Registry", this::loadRepositories, HELP_TITLE, HELP_TEXT,
                        grid, selectionActions, buildHeaderButtons()),
                noClusterMessage, emptyRegistryMessage, grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.GLOBAL_REGISTRY_VIEW)) {
            event.forwardTo("");
            return;
        }
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        grid.setVisible(hasCluster);
        emptyRegistryMessage.setVisible(false);
        if (hasCluster) {
            loadRepositories();
        }
    }

    private List<Button> buildHeaderButtons() {
        if (!SecurityUtils.hasPermission(Permission.GLOBAL_REGISTRY_BUILD)) {
            return List.of();
        }
        Button buildBtn = new Button("Build Image", VaadinIcon.HAMMER.create(), e -> openBuildDialog());
        buildBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        return List.of(buildBtn);
    }

    private void openBuildDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Build Image");
        dialog.setWidth("560px");

        TextField repositoryUrlField = new TextField("Git Repository URL");
        repositoryUrlField.setPlaceholder("https://github.com/usuario/repo");
        repositoryUrlField.setRequired(true);
        repositoryUrlField.setWidthFull();

        TextField branchField = new TextField("Branch");
        branchField.setPlaceholder("main");
        branchField.setWidthFull();

        TextField contextPathField = new TextField("Context path");
        contextPathField.setPlaceholder("Repository root");
        contextPathField.setHelperText("Subdirectory used as the build context (e.g. for monorepos). Leave empty to use the repository root.");
        contextPathField.setWidthFull();

        TextField dockerfilePathField = new TextField("Dockerfile path");
        dockerfilePathField.setPlaceholder("Dockerfile");
        dockerfilePathField.setHelperText("Path to the Dockerfile, relative to the Context path.");
        dockerfilePathField.setWidthFull();

        TextField repositoryField = new TextField("Repository");
        repositoryField.setPlaceholder("meu-grupo/minha-app");
        repositoryField.setRequired(true);
        repositoryField.setWidthFull();

        TextField tagField = new TextField("Tag");
        tagField.setPlaceholder("latest");
        tagField.setRequired(true);
        tagField.setWidthFull();

        FormLayout form = new FormLayout(repositoryUrlField, branchField, contextPathField, dockerfilePathField, repositoryField, tagField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        dialog.add(form);

        Button buildBtn = new Button("Build", e -> {
            if (!validateBuildForm(repositoryUrlField, repositoryField, tagField)) {
                return;
            }

            Cluster cluster = clusterContext.getCluster();
            try {
                String jobName = registryService.startBuild(cluster, new BuildRequest(
                        repositoryUrlField.getValue().trim(),
                        branchField.getValue(),
                        contextPathField.getValue(),
                        dockerfilePathField.getValue(),
                        repositoryField.getValue().trim(),
                        tagField.getValue().trim()
                ));
                dialog.close();
                UI.getCurrent().navigate("registry/build/" + jobName);
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        buildBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        dialog.getFooter().add(cancelBtn, buildBtn);
        dialog.open();
        repositoryUrlField.focus();
    }

    private boolean validateBuildForm(TextField repositoryUrlField, TextField repositoryField, TextField tagField) {
        boolean valid = true;

        if (!GIT_URL_PATTERN.matcher(repositoryUrlField.getValue().trim()).matches()) {
            repositoryUrlField.setErrorMessage("Enter a valid Git repository URL (http:// or https://)");
            repositoryUrlField.setInvalid(true);
            valid = false;
        } else {
            repositoryUrlField.setInvalid(false);
        }

        if (!REPOSITORY_PATTERN.matcher(repositoryField.getValue().trim()).matches()) {
            repositoryField.setErrorMessage("Use lowercase letters, digits, and . _ - / as separators");
            repositoryField.setInvalid(true);
            valid = false;
        } else {
            repositoryField.setInvalid(false);
        }

        if (!TAG_PATTERN.matcher(tagField.getValue().trim()).matches()) {
            tagField.setErrorMessage("Use letters, digits, and . _ - (max 128 characters)");
            tagField.setInvalid(true);
            valid = false;
        } else {
            tagField.setInvalid(false);
        }

        return valid;
    }

    private void openDeleteRepositoryDialog(RepositoryInfo repository) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Remove Repository");
        dialog.setText("Removing Repository \"" + repository.name() + "\" will permanently delete all " + repository.tagCount()
                + " tag(s) and run garbage collection on the Registry. This action cannot be undone.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Remove");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                registryMaintenanceService.deleteRepository(cluster, repository.name());
                deletedRepositoryNames.add(repository.name());
                allItems.removeIf(r -> r.name().equals(repository.name()));
                dataProvider.refreshAll();
                emptyRegistryMessage.setVisible(allItems.isEmpty());
                grid.setVisible(!allItems.isEmpty());
                notify("Repository " + repository.name() + " removed", NotificationVariant.LUMO_SUCCESS);
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }

    private void buildGrid() {
        var nameCol = grid.addColumn(RepositoryInfo::name).setHeader("Repository").setSortable(true).setFlexGrow(3).setResizable(true);
        grid.addColumn(RepositoryInfo::tagCount).setHeader("Tags").setWidth("100px").setSortable(true).setResizable(true);

        UiConstants.addActionsColumn(grid, 1, repository -> {
            var tagsIcon = VaadinIcon.LIST.create();
            tagsIcon.setSize(UiConstants.ICON_SIZE);
            Button tagsBtn = new Button(tagsIcon);
            tagsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            tagsBtn.getElement().setAttribute("title", "View Tags");
            tagsBtn.addClickListener(e -> {
                grid.select(repository);
                UI.getCurrent().navigate("registry/" + repository.name());
            });
            return List.of(tagsBtn);
        });

        grid.setDataProvider(dataProvider);

        TextField nameFilter = buildFilterField();
        dataProvider.setFilter(item -> matches(item.name(), nameFilter.getValue()));
        nameFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, RepositoryInfo::name);
        });

        HeaderRow filterRow = grid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);

        grid.setSizeFull();
        grid.setVisible(false);
    }

    private boolean loadRepositories() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        List<RepositoryInfo> items = registryService.listRepositories(cluster).stream()
                .filter(r -> !deletedRepositoryNames.contains(r.name()))
                .toList();
        allItems.clear();
        allItems.addAll(items);
        dataProvider.refreshAll();
        UiConstants.selectFirstOrPreserve(grid, dataProvider, RepositoryInfo::name);
        emptyRegistryMessage.setVisible(items.isEmpty());
        grid.setVisible(!items.isEmpty());
        return true;
    }

    @Override
    public void refresh() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        loadRepositories();
    }

    private VerticalLayout buildEmptyRegistryMessage() {
        Span text = new Span(EMPTY_REGISTRY_MESSAGE);
        text.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.MEDIUM);

        VerticalLayout layout = new VerticalLayout(text);
        layout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        layout.setJustifyContentMode(JustifyContentMode.CENTER);
        layout.setSizeFull();
        layout.setVisible(false);
        return layout;
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
}
