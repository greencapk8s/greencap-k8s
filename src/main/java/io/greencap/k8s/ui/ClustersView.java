package io.greencap.k8s.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.cluster.ClusterProvider;
import io.greencap.k8s.domain.cluster.ClusterService;
import io.greencap.k8s.domain.cluster.ConnectionStatus;
import io.greencap.k8s.domain.cluster.CreateClusterRequest;
import io.greencap.k8s.domain.user.UserService;
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.user.Permission;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubeconfigValidator;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Route(value = "clusters", layout = MainLayout.class)
@PageTitle("Clusters — GreenCap K8s")
@PermitAll
public class ClustersView extends VerticalLayout implements BeforeEnterObserver {

    private final ClusterService clusterService;
    private final KubeconfigValidator kubeconfigValidator;
    private final ClusterContext clusterContext;
    private final UserService userService;
    private final Grid<Cluster> grid = new Grid<>(Cluster.class, false);

    public ClustersView(ClusterService clusterService, KubeconfigValidator kubeconfigValidator,
                        ClusterContext clusterContext, UserService userService) {
        this.clusterService = clusterService;
        this.kubeconfigValidator = kubeconfigValidator;
        this.clusterContext = clusterContext;
        this.userService = userService;
        setSizeFull();
        add(buildToolbar(), buildGrid());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.GLOBAL_CLUSTERS_VIEW)) {
            event.forwardTo("");
            return;
        }
        refreshGrid();
    }

    private HorizontalLayout buildToolbar() {
        Button addBtn = new Button("Add Cluster", VaadinIcon.PLUS.create(),
                e -> openAddDialog());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBtn.setEnabled(SecurityUtils.hasPermission(Permission.GLOBAL_CLUSTERS_WRITE));

        HorizontalLayout toolbar = new HorizontalLayout(new H2("Clusters"), addBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        toolbar.expand(new H2("Clusters"));
        toolbar.setWidthFull();
        return toolbar;
    }

    private Grid<Cluster> buildGrid() {
        grid.addComponentColumn(this::buildRadioCell).setHeader("Active").setWidth("90px").setFlexGrow(0).setResizable(true);
        grid.addColumn(Cluster::getName).setHeader("Name").setSortable(true).setFlexGrow(1).setResizable(true);
        grid.addColumn(c -> c.getProvider().name()).setHeader("Provider").setWidth("120px").setResizable(true);
        grid.addComponentColumn(c -> statusBadge(c.getConnectionStatus()))
                .setHeader("Status").setWidth("140px").setResizable(true);
        UiConstants.addActionsColumn(grid, 2, this::buildActions);
        grid.setSizeFull();
        return grid;
    }

    private Div buildRadioCell(Cluster cluster) {
        Element inputEl = new Element("input");
        inputEl.setAttribute("type", "radio");
        inputEl.setAttribute("name", "cluster-active");
        inputEl.getStyle().set("cursor", "pointer").set("width", "16px").set("height", "16px");
        if (isActiveCluster(cluster)) {
            inputEl.setAttribute("checked", "true");
        }
        inputEl.addEventListener("change", e -> activateCluster(cluster));

        Div wrapper = new Div();
        wrapper.getElement().appendChild(inputEl);
        wrapper.getStyle().set("display", "flex").set("justify-content", "center").set("align-items", "center");
        return wrapper;
    }

    private boolean isActiveCluster(Cluster cluster) {
        return clusterContext.getCluster() != null
                && clusterContext.getCluster().getId().equals(cluster.getId());
    }

    private void activateCluster(Cluster cluster) {
        clusterContext.setCluster(cluster);
        clusterContext.setNamespace("default");
        persistActiveCluster(cluster);
        refreshGrid();
        getMainLayout().ifPresent(MainLayout::refreshClusterState);
        notify("Cluster \"" + cluster.getName() + "\" set as active", NotificationVariant.LUMO_SUCCESS);
    }

    private void persistActiveCluster(Cluster cluster) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.updateActiveCluster(username, cluster);
    }

    private Span statusBadge(ConnectionStatus status) {
        Span badge = new Span(status.name());
        badge.getElement().getThemeList().add("badge");
        switch (status) {
            case CONNECTED    -> badge.getElement().getThemeList().add("success");
            case ERROR        -> badge.getElement().getThemeList().add("error");
            case DISCONNECTED -> badge.getElement().getThemeList().add("contrast");
            default           -> {}
        }
        return badge;
    }

    private List<Button> buildActions(Cluster cluster) {
        var testIcon = VaadinIcon.CONNECT.create();
        testIcon.setSize(UiConstants.ICON_SIZE);
        Button testBtn = new Button(testIcon, e -> testConnection(cluster));
        testBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        testBtn.getElement().setAttribute("title", "Test connection");

        var deleteIcon = VaadinIcon.TRASH.create();
        deleteIcon.setSize(UiConstants.ICON_SIZE);
        Button deleteBtn = new Button(deleteIcon, e -> confirmDelete(cluster));
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
        deleteBtn.getElement().setAttribute("title", "Remove cluster");
        deleteBtn.setEnabled(SecurityUtils.hasPermission(Permission.GLOBAL_CLUSTERS_WRITE));

        return List.of(testBtn, deleteBtn);
    }

    private void confirmDelete(Cluster cluster) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Remove cluster");

        dialog.add(new com.vaadin.flow.component.html.Paragraph(
                "Are you sure you want to remove \"" + cluster.getName() + "\"? This action cannot be undone."));

        Button confirmBtn = new Button("Remove", e -> {
            if (isActiveCluster(cluster)) {
                clusterContext.setCluster(null);
                clusterContext.setNamespace("default");
                persistActiveCluster(null);
            }
            clusterService.deleteCluster(cluster);
            dialog.close();
            refreshGrid();
            notify("Cluster " + cluster.getName() + " removed", NotificationVariant.LUMO_SUCCESS);
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    private void testConnection(Cluster cluster) {
        ConnectionStatus status = clusterService.testConnection(cluster);
        if (isActiveCluster(cluster)) {
            clusterService.findAll().stream()
                    .filter(c -> c.getId().equals(cluster.getId()))
                    .findFirst()
                    .ifPresent(fresh -> {
                        clusterContext.setCluster(fresh);
                        getMainLayout().ifPresent(MainLayout::refreshClusterState);
                    });
        }
        refreshGrid();
        if (status == ConnectionStatus.CONNECTED) {
            notify("Connection to " + cluster.getName() + " successful", NotificationVariant.LUMO_SUCCESS);
        } else {
            notify("Failed to connect to " + cluster.getName(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void openAddDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("New Cluster");
        dialog.setWidth("560px");

        TextField nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setWidthFull();

        Select<ClusterProvider> providerSelect = new Select<>();
        providerSelect.setLabel("Provider");
        providerSelect.setItems(ClusterProvider.values());
        providerSelect.setValue(ClusterProvider.Kubernetes);
        providerSelect.setWidthFull();

        TextArea kubeconfigArea = new TextArea("Kubeconfig YAML");
        kubeconfigArea.setWidthFull();
        kubeconfigArea.setMinHeight("200px");
        kubeconfigArea.setPlaceholder(
                "Paste the kubeconfig content or upload the file.\n\n" +
                "⚠️ The kubeconfig must be portable and self-contained: generate it with\n" +
                "kubectl config view --flatten --minify\n" +
                "to embed certificates and export only the required context.");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setMaxFileSize(512 * 1024);
        upload.setDropLabel(new Span("Drop the kubeconfig here"));
        upload.addSucceededListener(e -> {
            try {
                String content = new String(buffer.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                kubeconfigArea.setValue(content);
                kubeconfigValidator.findPathReferencedCertificates(content).ifPresent(warning -> {
                    kubeconfigArea.setErrorMessage(warning);
                    kubeconfigArea.setInvalid(true);
                });
            } catch (IOException ex) {
                notify("Error reading file", NotificationVariant.LUMO_ERROR);
            }
        });

        FormLayout form = new FormLayout(nameField, providerSelect);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        VerticalLayout content = new VerticalLayout(form, upload, kubeconfigArea);
        content.setPadding(false);
        content.setSpacing(true);
        dialog.add(content);

        Button saveBtn = new Button("Save", e -> {
            if (nameField.isEmpty()) {
                nameField.setErrorMessage("Name is required");
                nameField.setInvalid(true);
                return;
            }
            if (kubeconfigArea.isEmpty()) {
                notify("Kubeconfig is required", NotificationVariant.LUMO_ERROR);
                return;
            }

            var certWarning = kubeconfigValidator.findPathReferencedCertificates(kubeconfigArea.getValue());
            if (certWarning.isPresent()) {
                kubeconfigArea.setErrorMessage(certWarning.get());
                kubeconfigArea.setInvalid(true);
                return;
            }

            Cluster saved = clusterService.createCluster(new CreateClusterRequest(
                    nameField.getValue().trim(),
                    providerSelect.getValue(),
                    kubeconfigArea.getValue()
            ));

            dialog.close();
            refreshGrid();

            String statusMsg = saved.getConnectionStatus() == ConnectionStatus.CONNECTED
                    ? "connected successfully"
                    : "added (no connection — check the kubeconfig)";
            notify("Cluster " + saved.getName() + " " + statusMsg,
                    saved.getConnectionStatus() == ConnectionStatus.CONNECTED
                            ? NotificationVariant.LUMO_SUCCESS
                            : NotificationVariant.LUMO_WARNING);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
        nameField.focus();
    }

    private java.util.Optional<MainLayout> getMainLayout() {
        return getUI().flatMap(ui -> ui.getChildren()
                .filter(c -> c instanceof MainLayout)
                .map(c -> (MainLayout) c)
                .findFirst());
    }

    private void refreshGrid() {
        grid.setItems(clusterService.findAll());
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
