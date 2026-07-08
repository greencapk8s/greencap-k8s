package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.RegistryMaintenanceService;
import io.greencap.k8s.kubernetes.RegistryService;
import io.greencap.k8s.kubernetes.dto.TagInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Route(value = "registry/:repository*", layout = MainLayout.class)
@PageTitle("Container Registry — GreenCap K8s")
@PermitAll
public class RegistryTagsView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "Tags";
    private static final String HELP_TEXT = "A Tag is a named reference to a specific image version within a Repository (e.g. \"latest\", \"v1.2.3\"), shown here with its digest (content hash of the image manifest), size and creation date.\n\nSelect one or more Tags using the checkboxes and click \"Remove Tags\" to permanently delete them. This frees the manifest references but does not immediately release storage — unreferenced blobs are only reclaimed when \"Remove Repository\" runs garbage collection.";

    private final RegistryService registryService;
    private final RegistryMaintenanceService registryMaintenanceService;
    private final ClusterContext clusterContext;

    private final Span repositorySpan = new Span();
    private final Grid<TagInfo> grid = new Grid<>(TagInfo.class, false);
    private final VerticalLayout noClusterMessage;
    private final VerticalLayout emptyTagsMessage;

    private final List<TagInfo> allItems = new ArrayList<>();
    private final ListDataProvider<TagInfo> dataProvider = new ListDataProvider<>(allItems);

    private String repository = "";
    private Button removeTagsBtn;

    public RegistryTagsView(RegistryService registryService, RegistryMaintenanceService registryMaintenanceService,
                             ClusterContext clusterContext) {
        this.registryService = registryService;
        this.registryMaintenanceService = registryMaintenanceService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        emptyTagsMessage = buildEmptyTagsMessage();

        boolean canDelete = true;
        buildGrid(canDelete);

        add(buildHeader(canDelete), noClusterMessage, emptyTagsMessage, grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        RouteParameters params = event.getRouteParameters();
        repository = params.get("repository").orElse("");
        repositorySpan.setText(repository);

        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        grid.setVisible(hasCluster);
        emptyTagsMessage.setVisible(false);
        if (hasCluster) {
            loadTags();
        }
    }

    private HorizontalLayout buildHeader(boolean canDelete) {
        Button backBtn = new Button(VaadinIcon.ARROW_LEFT.create(), e -> UI.getCurrent().navigate(RegistryView.class));
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        backBtn.getElement().setAttribute("title", "Back to Registry");

        repositorySpan.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);

        var refreshIcon = VaadinIcon.REFRESH.create();
        refreshIcon.setSize(UiConstants.ICON_SIZE);
        Button refreshBtn = new Button(refreshIcon, e -> loadTags());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        refreshBtn.getElement().setAttribute("title", "Refresh");

        var helpIcon = VaadinIcon.QUESTION_CIRCLE.create();
        helpIcon.setSize(UiConstants.ICON_SIZE);
        Button helpBtn = new Button(helpIcon, e -> HelpDialog.open(HELP_TITLE, HELP_TEXT));
        helpBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        helpBtn.getElement().setAttribute("title", "Help");

        HorizontalLayout header = new HorizontalLayout(backBtn, repositorySpan);
        if (canDelete) {
            var trashIcon = VaadinIcon.TRASH.create();
            trashIcon.setSize(UiConstants.ICON_SIZE);
            removeTagsBtn = new Button(trashIcon, e -> openDeleteTagsDialog(grid.asMultiSelect().getSelectedItems()));
            removeTagsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
            removeTagsBtn.getElement().setAttribute("title", "Remove Tags");
            removeTagsBtn.setEnabled(false);
            header.add(removeTagsBtn);
        }
        header.add(refreshBtn, helpBtn);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.setWidthFull();
        header.expand(repositorySpan);
        return header;
    }

    private void buildGrid(boolean canDelete) {
        if (canDelete) {
            grid.setSelectionMode(Grid.SelectionMode.MULTI);
            grid.asMultiSelect().addValueChangeListener(e -> {
                if (removeTagsBtn != null) {
                    removeTagsBtn.setEnabled(!e.getValue().isEmpty());
                }
            });
        }

        grid.addColumn(TagInfo::name).setHeader("Tag").setSortable(true).setFlexGrow(1).setResizable(true);
        grid.addComponentColumn(this::buildDigestSpan).setHeader("Digest").setFlexGrow(2).setResizable(true);
        grid.addColumn(TagInfo::size).setHeader("Size").setWidth("110px").setResizable(true);
        grid.addColumn(TagInfo::createdAt).setHeader("Created").setWidth("90px").setResizable(true);

        grid.setDataProvider(dataProvider);
        grid.setSizeFull();
        grid.setVisible(false);
    }

    private Span buildDigestSpan(TagInfo tag) {
        String digest = tag.digest();
        Span span = new Span(digest);
        span.getElement().setAttribute("title", digest);
        span.getStyle()
                .set("display", "block")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap");
        return span;
    }

    private boolean loadTags() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        List<TagInfo> items = registryService.listTags(cluster, repository);
        allItems.clear();
        allItems.addAll(items);
        dataProvider.refreshAll();
        emptyTagsMessage.setVisible(items.isEmpty());
        grid.setVisible(!items.isEmpty());
        return true;
    }

    @Override
    public void refresh() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        loadTags();
    }

    private void openDeleteTagsDialog(Set<TagInfo> selected) {
        String tagNames = selected.stream().map(TagInfo::name).sorted().collect(Collectors.joining(", "));

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Remove Tags");
        dialog.setText("Removing " + selected.size() + " tag(s) from \"" + repository + "\" (" + tagNames
                + ") is permanent and cannot be undone.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Remove");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                registryMaintenanceService.deleteTags(cluster, repository, List.copyOf(selected));
                loadTags();
                notify(selected.size() + " tag(s) removed", NotificationVariant.LUMO_SUCCESS);
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

    private VerticalLayout buildEmptyTagsMessage() {
        Span text = new Span("No tags found for this repository.");
        text.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.MEDIUM);

        VerticalLayout layout = new VerticalLayout(text);
        layout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        layout.setJustifyContentMode(JustifyContentMode.CENTER);
        layout.setSizeFull();
        layout.setVisible(false);
        return layout;
    }
}
