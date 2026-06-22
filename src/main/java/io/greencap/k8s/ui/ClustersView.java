package io.greencap.k8s.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
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
import java.util.ArrayList;
import java.util.List;

@Route(value = "clusters", layout = MainLayout.class)
@PageTitle("Clusters — GreenCap K8s")
@PermitAll
public class ClustersView extends VerticalLayout implements BeforeEnterObserver {

    private static final String HELP_TITLE = "Clusters";
    private static final String HELP_TEXT = "A Cluster is a registered Kubernetes environment (kubeconfig). " +
            "Select the active cluster using the radio button — all workload views will reflect its resources. " +
            "Select a row and use \"Test Connection\" to verify reachability, or \"Remove\" to delete the registration " +
            "(the cluster itself is not affected).";

    private final ClusterService clusterService;
    private final KubeconfigValidator kubeconfigValidator;
    private final ClusterContext clusterContext;
    private final UserService userService;
    private final Grid<Cluster> grid = new Grid<>(Cluster.class, false);

    public ClustersView(ClusterService clusterService, KubeconfigValidator kubeconfigValidator,
                        ClusterContext clusterContext, UserService userService,
                        GridSelectionMemory selectionMemory) {
        this.clusterService = clusterService;
        this.kubeconfigValidator = kubeconfigValidator;
        this.clusterContext = clusterContext;
        this.userService = userService;

        setSizeFull();
        setPadding(true);

        buildGrid();
        UiConstants.configureSingleSelection(grid, selectionMemory, getClass().getSimpleName(), Cluster::getName);

        boolean canWrite = SecurityUtils.hasPermission(Permission.GLOBAL_CLUSTERS_WRITE);

        List<UiConstants.SelectionAction<Cluster>> selectionActions = List.of(
                UiConstants.SelectionAction.of(VaadinIcon.CONNECT, "Test Connection", this::testConnection),
                UiConstants.SelectionAction.destructive(VaadinIcon.TRASH, "Remove", canWrite, this::confirmDelete)
        );

        List<Button> extraButtons = new ArrayList<>();
        if (canWrite) {
            Button addBtn = new Button("Add Cluster", VaadinIcon.PLUS.create());
            addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            addBtn.addClickListener(e -> openAddDialog());
            extraButtons.add(addBtn);
        }

        add(UiConstants.buildSectionHeader("Clusters", this::refreshGrid, HELP_TITLE, HELP_TEXT,
                        grid, selectionActions, extraButtons),
                grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.GLOBAL_CLUSTERS_VIEW)) {
            event.forwardTo("");
            return;
        }
        refreshGrid();
    }

    private void buildGrid() {
        grid.addComponentColumn(this::buildRadioCell).setHeader("Active").setWidth("90px").setFlexGrow(0).setResizable(true);
        grid.addColumn(Cluster::getName).setHeader("Name").setSortable(true).setFlexGrow(1).setResizable(true);
        grid.addComponentColumn(c -> statusBadge(c.getConnectionStatus()))
                .setHeader("Status").setWidth("140px").setResizable(true);
        grid.setSizeFull();
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

    private void confirmDelete(Cluster cluster) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Remove cluster");

        dialog.add(new com.vaadin.flow.component.html.Paragraph(
                "Are you sure you want to remove \"" + cluster.getName() + "\"? This action cannot be undone."));

        Button confirmBtn = new Button("Remove", e -> {
            boolean wasActive = isActiveCluster(cluster);
            clusterService.deleteCluster(cluster);
            if (wasActive) {
                clusterService.findAll().stream()
                        .findFirst()
                        .ifPresentOrElse(
                                this::activateCluster,
                                () -> {
                                    clusterContext.setCluster(null);
                                    clusterContext.setNamespace("default");
                                    persistActiveCluster(null);
                                    getMainLayout().ifPresent(MainLayout::refreshClusterState);
                                }
                        );
            }
            dialog.close();
            refreshGrid();
            notify("Cluster " + cluster.getName() + " removed", NotificationVariant.LUMO_SUCCESS);
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

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
        dialog.setWidth("580px");
        dialog.setHeight("520px");
        dialog.setResizable(true);

        TextField nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setWidthFull();

        // --- Kubeconfig tab ---
        TextArea kubeconfigArea = new TextArea("Kubeconfig YAML");
        kubeconfigArea.setWidthFull();
        kubeconfigArea.setMinHeight("200px");
        kubeconfigArea.setPlaceholder(
                "Paste the kubeconfig content or upload the file.\n\n" +
                "⚠️ The kubeconfig must be portable and self-contained — use the command below to generate it.");

        Div commandBlock = buildCopyableCommand("kubectl config view --flatten --minify");

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

        VerticalLayout kubeconfigContent = new VerticalLayout(commandBlock, upload, kubeconfigArea);
        kubeconfigContent.setPadding(false);
        kubeconfigContent.setSpacing(true);

        // --- Token + URL tab ---
        TextField apiUrlField = new TextField("API Server URL");
        apiUrlField.setWidthFull();
        apiUrlField.setPlaceholder("https://192.168.49.2:8443");
        apiUrlField.setRequired(true);

        TextArea tokenArea = new TextArea("Bearer Token");
        tokenArea.setWidthFull();
        tokenArea.setMinHeight("80px");
        tokenArea.setPlaceholder("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...");
        tokenArea.setRequired(true);

        TextArea caCertArea = new TextArea("CA Certificate (PEM or base64)");
        caCertArea.setWidthFull();
        caCertArea.setMinHeight("80px");
        caCertArea.setPlaceholder("-----BEGIN CERTIFICATE-----\n...");

        Details caDetails = new Details("CA Certificate (optional)", caCertArea);
        caDetails.setWidthFull();

        FormLayout tokenForm = new FormLayout(apiUrlField, tokenArea);
        tokenForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        VerticalLayout tokenContent = new VerticalLayout(tokenForm, caDetails);
        tokenContent.setPadding(false);
        tokenContent.setSpacing(true);

        // --- TabSheet ---
        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        Tab tokenUrlTab   = tabs.add("Token + URL", tokenContent);
        Tab kubeconfigTab = tabs.add("Kubeconfig", kubeconfigContent);

        VerticalLayout dialogContent = new VerticalLayout(nameField, tabs);
        dialogContent.setPadding(false);
        dialogContent.setSpacing(true);
        dialog.add(dialogContent);

        Button saveBtn = new Button("Save");
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        saveBtn.addClickListener(e -> {
            if (nameField.isEmpty()) {
                nameField.setErrorMessage("Name is required");
                nameField.setInvalid(true);
                return;
            }

            String kubeconfigToSave;

            if (tabs.getSelectedTab() == tokenUrlTab) {
                if (apiUrlField.isEmpty()) {
                    apiUrlField.setErrorMessage("API Server URL is required");
                    apiUrlField.setInvalid(true);
                    return;
                }
                if (tokenArea.isEmpty()) {
                    tokenArea.setErrorMessage("Bearer Token is required");
                    tokenArea.setInvalid(true);
                    return;
                }
                kubeconfigToSave = clusterService.synthesizeKubeconfig(
                        apiUrlField.getValue(),
                        tokenArea.getValue(),
                        caCertArea.getValue()
                );
            } else {
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
                kubeconfigToSave = kubeconfigArea.getValue();
            }

            Cluster saved = clusterService.createCluster(new CreateClusterRequest(
                    nameField.getValue().trim(),
                    kubeconfigToSave
            ));

            if (clusterContext.getCluster() == null) {
                activateCluster(saved);
            }

            dialog.close();
            refreshGrid();

            String statusMsg = saved.getConnectionStatus() == ConnectionStatus.CONNECTED
                    ? "connected successfully"
                    : "added (no connection — check the credentials)";
            notify("Cluster " + saved.getName() + " " + statusMsg,
                    saved.getConnectionStatus() == ConnectionStatus.CONNECTED
                            ? NotificationVariant.LUMO_SUCCESS
                            : NotificationVariant.LUMO_WARNING);
        });

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
        nameField.focus();
    }

    private Div buildCopyableCommand(String command) {
        Span commandText = new Span(command);
        commandText.getStyle()
                .set("font-family", "monospace")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "#e2e8f0")
                .set("flex", "1");

        Icon copyIcon = VaadinIcon.COPY_O.create();
        copyIcon.setSize("20px");
        copyIcon.getStyle().set("color", "#94a3b8").set("flex-shrink", "0");

        Button copyBtn = new Button(copyIcon);
        copyBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        copyBtn.getStyle()
                .set("color", "#94a3b8")
                .set("padding", "0")
                .set("cursor", "pointer");
        copyBtn.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.getPage().executeJs(
                    "navigator.clipboard.writeText($0)", command));
            notify("Command copied to clipboard", NotificationVariant.LUMO_SUCCESS);
        });

        HorizontalLayout row = new HorizontalLayout(commandText, copyBtn);
        row.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        row.setWidthFull();
        row.setPadding(false);
        row.setSpacing(false);
        row.getStyle().set("gap", "8px");

        Div block = new Div(row);
        block.setWidthFull();
        block.getStyle()
                .set("background", "#1e293b")
                .set("border-radius", "6px")
                .set("padding", "10px 12px")
                .set("box-sizing", "border-box");
        return block;
    }

    private java.util.Optional<MainLayout> getMainLayout() {
        return getUI().flatMap(ui -> ui.getChildren()
                .filter(c -> c instanceof MainLayout)
                .map(c -> (MainLayout) c)
                .findFirst());
    }

    private boolean refreshGrid() {
        grid.setItems(clusterService.findAll());
        return true;
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
