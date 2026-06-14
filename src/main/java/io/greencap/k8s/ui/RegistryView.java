package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import io.greencap.k8s.kubernetes.RegistryService;
import io.greencap.k8s.kubernetes.dto.RepositoryInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;

@Route(value = "registry", layout = MainLayout.class)
@PageTitle("Container Registry — GreenCap K8s")
@PermitAll
public class RegistryView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "Container Registry";
    private static final String HELP_TEXT = "The Registry is the container registry running inside the active Cluster (the Service \"registry\" in the \"kube-system\" Namespace), reached via a Kubernetes API port-forward — no separate credentials needed.\n\nA Repository is a named collection of image versions (e.g. \"greencap-demo/backend\"). Each Tag is a named reference to a specific image version (e.g. \"latest\", \"v1.2.3\"), with its digest, size and creation date.";

    private static final String EMPTY_REGISTRY_MESSAGE =
            "No repositories found. Make sure the Service \"registry\" in the \"kube-system\" namespace is available on this Cluster.";

    private final RegistryService registryService;
    private final ClusterContext clusterContext;

    private final Grid<RepositoryInfo> grid = new Grid<>(RepositoryInfo.class, false);
    private final VerticalLayout noClusterMessage;
    private final VerticalLayout emptyRegistryMessage;

    private final List<RepositoryInfo> allItems = new ArrayList<>();
    private final ListDataProvider<RepositoryInfo> dataProvider = new ListDataProvider<>(allItems);

    public RegistryView(RegistryService registryService, ClusterContext clusterContext) {
        this.registryService = registryService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        emptyRegistryMessage = buildEmptyRegistryMessage();
        buildGrid();
        UiConstants.configureSingleSelection(grid);

        add(UiConstants.buildSectionHeader("Container Registry", this::loadRepositories, HELP_TITLE, HELP_TEXT),
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

    private void buildGrid() {
        var nameCol = grid.addColumn(RepositoryInfo::name).setHeader("Repository").setSortable(true).setFlexGrow(3).setResizable(true);
        grid.addColumn(RepositoryInfo::tagCount).setHeader("Tags").setWidth("100px").setSortable(true).setResizable(true);

        UiConstants.addActionsColumn(grid, 1, repository -> {
            var tagsIcon = VaadinIcon.LIST.create();
            tagsIcon.setSize(UiConstants.ICON_SIZE);
            Button tagsBtn = new Button(tagsIcon);
            tagsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            tagsBtn.getElement().setAttribute("title", "View Tags");
            tagsBtn.addClickListener(e -> UI.getCurrent().navigate("registry/" + repository.name()));
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
        List<RepositoryInfo> items = registryService.listRepositories(cluster);
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
