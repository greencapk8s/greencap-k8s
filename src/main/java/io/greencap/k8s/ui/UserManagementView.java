package io.greencap.k8s.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.user.Permission;
import io.greencap.k8s.domain.user.User;
import io.greencap.k8s.domain.user.UserService;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("Users — GreenCap K8s")
@PermitAll
public class UserManagementView extends VerticalLayout implements BeforeEnterObserver {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String HELP_TITLE = "Users";
    private static final String HELP_TEXT = "Users are the accounts that can log into GreenCap K8s. " +
            "Each user has a set of Permissions that control which views and actions they can access. " +
            "Select a user and use \"Edit Permissions\" to adjust access, or \"Deactivate\" to revoke login. " +
            "The default admin account cannot have its permissions modified.";

    private final UserService userService;
    private final Grid<User> grid = new Grid<>(User.class, false);

    public UserManagementView(UserService userService, GridSelectionMemory selectionMemory) {
        this.userService = userService;

        setSizeFull();
        setPadding(true);

        buildGrid();
        UiConstants.configureSingleSelection(grid, selectionMemory, getClass().getSimpleName(), User::getUsername);

        boolean canWrite = SecurityUtils.hasPermission(Permission.SETTINGS_USERS_WRITE);

        List<UiConstants.SelectionAction<User>> selectionActions = List.of(
                UiConstants.SelectionAction.of(VaadinIcon.EDIT, "Edit Permissions", canWrite, this::openEditPermissionsDialog),
                UiConstants.SelectionAction.destructive(VaadinIcon.BAN, "Deactivate", canWrite, this::confirmDeactivate)
        );

        List<Button> extraButtons = new ArrayList<>();
        if (canWrite) {
            Button addBtn = new Button("Add User", VaadinIcon.PLUS.create());
            addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            addBtn.addClickListener(e -> openAddDialog());
            extraButtons.add(addBtn);
        }

        add(UiConstants.buildSectionHeader("Users", this::refreshGrid, HELP_TITLE, HELP_TEXT,
                        grid, selectionActions, extraButtons),
                grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.SETTINGS_USERS_VIEW)) {
            event.forwardTo("");
            return;
        }
        refreshGrid();
    }

    private void buildGrid() {
        grid.addColumn(User::getUsername).setHeader("Username").setSortable(true).setFlexGrow(1).setResizable(true);
        grid.addColumn(User::getEmail).setHeader("Email").setFlexGrow(2).setResizable(true);
        grid.addComponentColumn(this::permissionCountBadge).setHeader("Permissions").setWidth("140px").setResizable(true);
        grid.addComponentColumn(this::activeBadge).setHeader("Status").setWidth("100px").setResizable(true);
        grid.addColumn(u -> u.getCreatedAt().format(DATE_FORMATTER)).setHeader("Created").setWidth("160px").setResizable(true);
        grid.setSizeFull();
    }

    private Span permissionCountBadge(User user) {
        int count = user.getPermissions().size();
        int total = Permission.values().length;
        Span badge = new Span(count + " / " + total);
        badge.getElement().getThemeList().add("badge");
        if (count == total) {
            badge.getElement().getThemeList().add("success");
        } else if (count == 0) {
            badge.getElement().getThemeList().add("error");
        } else {
            badge.getElement().getThemeList().add("contrast");
        }
        return badge;
    }

    private Span activeBadge(User user) {
        Span badge = new Span(user.isActive() ? "Active" : "Inactive");
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add(user.isActive() ? "success" : "contrast");
        return badge;
    }

    private void confirmDeactivate(User user) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getUsername().equals(currentUsername)) {
            notify("Cannot deactivate your own account", NotificationVariant.LUMO_WARNING);
            return;
        }
        if (!user.isActive()) {
            notify("User " + user.getUsername() + " is already inactive", NotificationVariant.LUMO_WARNING);
            return;
        }
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Deactivate user");
        dialog.add(new com.vaadin.flow.component.html.Paragraph(
                "Deactivate \"" + user.getUsername() + "\"? They will no longer be able to log in."));

        Button confirmBtn = new Button("Deactivate", e -> {
            userService.deactivateUser(user.getId());
            dialog.close();
            refreshGrid();
            notify("User " + user.getUsername() + " deactivated", NotificationVariant.LUMO_SUCCESS);
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    private void openAddDialog() {
        TextField usernameField = new TextField("Username");
        usernameField.setRequired(true);
        usernameField.setWidthFull();

        EmailField emailField = new EmailField("Email");
        emailField.setRequired(true);
        emailField.setWidthFull();

        PasswordField passwordField = new PasswordField("Password");
        passwordField.setRequired(true);
        passwordField.setWidthFull();

        FormLayout form = new FormLayout(usernameField, emailField, passwordField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        PermissionTreePanel permissionPanel = new PermissionTreePanel(Set.of());

        VerticalLayout content = new VerticalLayout(form, new Hr(), permissionPanel);
        content.setPadding(false);
        content.setSpacing(true);

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("New User");
        dialog.setWidth("560px");
        dialog.add(content);

        Button saveBtn = new Button("Save", e -> {
            boolean valid = true;
            if (usernameField.isEmpty()) {
                usernameField.setErrorMessage("Username is required");
                usernameField.setInvalid(true);
                valid = false;
            }
            if (emailField.isEmpty()) {
                emailField.setErrorMessage("Email is required");
                emailField.setInvalid(true);
                valid = false;
            }
            if (passwordField.isEmpty()) {
                passwordField.setErrorMessage("Password is required");
                passwordField.setInvalid(true);
                valid = false;
            }
            if (!valid) return;
            try {
                userService.createUser(
                        usernameField.getValue().trim(),
                        emailField.getValue().trim(),
                        passwordField.getValue(),
                        permissionPanel.getSelectedPermissions()
                );
                dialog.close();
                refreshGrid();
                notify("User " + usernameField.getValue().trim() + " created", NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                notify("Failed to create user: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
        usernameField.focus();
    }

    private void openEditPermissionsDialog(User user) {
        if (DEFAULT_ADMIN_USERNAME.equals(user.getUsername())) {
            notify("Default admin permissions are protected", NotificationVariant.LUMO_WARNING);
            return;
        }
        PermissionTreePanel permissionPanel = new PermissionTreePanel(user.getPermissions());

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Permissions — " + user.getUsername());
        dialog.setWidth("560px");
        dialog.add(permissionPanel);

        Button saveBtn = new Button("Save", e -> {
            userService.updatePermissions(user.getId(), permissionPanel.getSelectedPermissions());
            dialog.close();
            refreshGrid();
            notify("Permissions updated for " + user.getUsername(), NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private boolean refreshGrid() {
        grid.setItems(userService.findAll());
        return true;
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }

    // -------------------------------------------------------------------------
    // Permission tree panel
    // -------------------------------------------------------------------------

    private static class PermissionTreePanel extends VerticalLayout {

        private final List<PermissionNode> leafNodes = new ArrayList<>();
        private final List<GroupNode> groupNodes = new ArrayList<>();

        PermissionTreePanel(Set<Permission> initial) {
            setPadding(false);
            setSpacing(false);

            Button selectAllBtn = new Button("Select All", e -> selectAll(true));
            selectAllBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button deselectAllBtn = new Button("Deselect All", e -> selectAll(false));
            deselectAllBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button expandAllBtn = new Button("Expand All", e -> groupNodes.forEach(g -> g.setExpanded(true)));
            expandAllBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button collapseAllBtn = new Button("Collapse All", e -> groupNodes.forEach(g -> g.setExpanded(false)));
            collapseAllBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            HorizontalLayout bulkActions = new HorizontalLayout(selectAllBtn, deselectAllBtn, expandAllBtn, collapseAllBtn);
            bulkActions.setSpacing(true);
            bulkActions.setPadding(false);
            add(bulkActions);

            add(buildSection("PROJECT", buildProjectGroups(initial)));
            add(buildSection("GLOBAL", buildGlobalGroups(initial)));
            add(buildSection("SETTINGS", buildSettingsGroups(initial)));
        }

        private VerticalLayout buildSection(String label, List<GroupNode> groups) {
            Span sectionLabel = new Span(label);
            sectionLabel.getStyle()
                    .set("font-size", "var(--lumo-font-size-xxs)")
                    .set("font-weight", "bold")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("margin-top", "var(--lumo-space-m)")
                    .set("margin-bottom", "var(--lumo-space-xs)");

            VerticalLayout section = new VerticalLayout(sectionLabel);
            section.setPadding(false);
            section.setSpacing(false);
            for (GroupNode group : groups) {
                section.add(group);
                groupNodes.add(group);
            }
            return section;
        }

        private List<GroupNode> buildProjectGroups(Set<Permission> initial) {
            List<GroupNode> groups = new ArrayList<>();

            groups.add(buildGroup("Deploy Application", Map.of(
                    "Deploy Application", Permission.PROJECT_DEPLOY_APPLICATION
            ), initial));

            groups.add(buildGroup("Topology", Map.of(
                    "Topology", Permission.TOPOLOGY_VIEW
            ), initial));

            groups.add(buildGroup("Observability", new LinkedHashMap<>() {{
                put("Dashboard", Permission.OBSERVABILITY_DASHBOARD_VIEW);
                put("Events", Permission.OBSERVABILITY_EVENTS_VIEW);
                put("Metrics", Permission.OBSERVABILITY_METRICS_VIEW);
            }}, initial));

            groups.add(buildWorkloadsGroup(initial));

            groups.add(buildGroup("Networking", new LinkedHashMap<>() {{
                put("Services", Permission.NETWORKING_SERVICES_VIEW);
                put("Ingresses", Permission.NETWORKING_INGRESS_VIEW);
            }}, initial));

            groups.add(buildGroup("Parameters", new LinkedHashMap<>() {{
                put("ConfigMaps", Permission.PARAMETERS_CONFIGMAPS_VIEW);
                put("Secrets", Permission.PARAMETERS_SECRETS_VIEW);
            }}, initial));

            groups.add(buildGroup("Auto Scaling", new LinkedHashMap<>() {{
                put("Horizontal Scaler (View)", Permission.AUTOSCALING_HORIZONTALSCALER_VIEW);
                put("Horizontal Scaler (Write)", Permission.AUTOSCALING_HORIZONTALSCALER_WRITE);
            }}, initial));

            groups.add(buildGroup("Storage", Map.of(
                    "Volume Claims (PVC)", Permission.STORAGE_PVC_VIEW
            ), initial));

            return groups;
        }

        private List<GroupNode> buildGlobalGroups(Set<Permission> initial) {
            List<GroupNode> groups = new ArrayList<>();

            groups.add(buildGroup("Clusters", new LinkedHashMap<>() {{
                put("Clusters (View)", Permission.GLOBAL_CLUSTERS_VIEW);
                put("Clusters (Write)", Permission.GLOBAL_CLUSTERS_WRITE);
            }}, initial));

            groups.add(buildGroup("Namespaces", new LinkedHashMap<>() {{
                put("Namespaces (View)", Permission.GLOBAL_NAMESPACES_VIEW);
                put("Namespaces (Write)", Permission.GLOBAL_NAMESPACES_WRITE);
                put("Namespaces (Delete)", Permission.GLOBAL_NAMESPACES_DELETE);
            }}, initial));

            groups.add(buildGroup("Infrastructure", Map.of(
                    "Infrastructure", Permission.GLOBAL_INFRASTRUCTURE_VIEW
            ), initial));

            groups.add(buildGroup("Container Registry", new LinkedHashMap<>() {{
                put("Container Registry (View)", Permission.GLOBAL_REGISTRY_VIEW);
                put("Container Registry (Build)", Permission.GLOBAL_REGISTRY_BUILD);
                put("Container Registry (Delete)", Permission.GLOBAL_REGISTRY_DELETE);
            }}, initial));

            return groups;
        }

        private List<GroupNode> buildSettingsGroups(Set<Permission> initial) {
            List<GroupNode> groups = new ArrayList<>();

            groups.add(buildGroup("Users", new LinkedHashMap<>() {{
                put("Users (View)", Permission.SETTINGS_USERS_VIEW);
                put("Users (Write)", Permission.SETTINGS_USERS_WRITE);
            }}, initial));

            groups.add(buildGroup("Platform Settings", Map.of(
                    "Platform Settings", Permission.SETTINGS_PLATFORM_VIEW
            ), initial));

            return groups;
        }

        private GroupNode buildGroup(String label, Map<String, Permission> items, Set<Permission> initial) {
            List<PermissionNode> nodes = items.entrySet().stream()
                    .map(e -> new PermissionNode(e.getKey(), e.getValue(), initial.contains(e.getValue())))
                    .toList();
            leafNodes.addAll(nodes);
            GroupNode group = new GroupNode(label, nodes, new ArrayList<>(nodes));
            nodes.forEach(n -> n.getCheckbox().addValueChangeListener(ev -> group.syncState()));
            return group;
        }

        private GroupNode buildWorkloadsGroup(Set<Permission> initial) {
            SubGroupNode deploymentsSubGroup = new SubGroupNode(
                    "Deployments", Permission.WORKLOADS_DEPLOYMENTS_VIEW,
                    new LinkedHashMap<>() {{
                        put("Scale", Permission.WORKLOADS_DEPLOYMENTS_SCALE);
                        put("Restart", Permission.WORKLOADS_DEPLOYMENTS_RESTART);
                        put("Rollback", Permission.WORKLOADS_DEPLOYMENTS_ROLLBACK);
                    }},
                    initial
            );

            SubGroupNode statefulSetsSubGroup = new SubGroupNode(
                    "StatefulSets", Permission.WORKLOADS_STATEFULSETS_VIEW,
                    new LinkedHashMap<>() {{
                        put("Scale", Permission.WORKLOADS_STATEFULSETS_SCALE);
                        put("Restart", Permission.WORKLOADS_STATEFULSETS_RESTART);
                        put("Rollback", Permission.WORKLOADS_STATEFULSETS_ROLLBACK);
                    }},
                    initial
            );

            SubGroupNode jobsSubGroup = new SubGroupNode(
                    "Jobs", Permission.WORKLOADS_JOBS_VIEW,
                    new LinkedHashMap<>() {{
                        put("Delete", Permission.WORKLOADS_JOBS_DELETE);
                    }},
                    initial
            );

            SubGroupNode cronJobsSubGroup = new SubGroupNode(
                    "CronJobs", Permission.WORKLOADS_CRONJOBS_VIEW,
                    new LinkedHashMap<>() {{
                        put("Run Now", Permission.WORKLOADS_CRONJOBS_RUN_NOW);
                        put("Suspend", Permission.WORKLOADS_CRONJOBS_SUSPEND);
                        put("Delete", Permission.WORKLOADS_CRONJOBS_DELETE);
                    }},
                    initial
            );

            List<PermissionNode> otherNodes = List.of(
                    new PermissionNode("ReplicaSets", Permission.WORKLOADS_REPLICASETS_VIEW, initial.contains(Permission.WORKLOADS_REPLICASETS_VIEW)),
                    new PermissionNode("Pods", Permission.WORKLOADS_PODS_VIEW, initial.contains(Permission.WORKLOADS_PODS_VIEW))
            );

            List<PermissionNode> allLeaves = new ArrayList<>(deploymentsSubGroup.allLeaves());
            allLeaves.addAll(statefulSetsSubGroup.allLeaves());
            allLeaves.addAll(jobsSubGroup.allLeaves());
            allLeaves.addAll(cronJobsSubGroup.allLeaves());
            allLeaves.addAll(otherNodes);
            leafNodes.addAll(allLeaves);

            List<Component> displayItems = new ArrayList<>();
            displayItems.add(deploymentsSubGroup);
            displayItems.add(statefulSetsSubGroup);
            displayItems.add(jobsSubGroup);
            displayItems.add(cronJobsSubGroup);
            displayItems.addAll(otherNodes);

            GroupNode group = new GroupNode("Workloads", allLeaves, displayItems);
            allLeaves.forEach(n -> n.getCheckbox().addValueChangeListener(ev -> group.syncState()));
            return group;
        }

        private void selectAll(boolean selected) {
            leafNodes.forEach(n -> n.getCheckbox().setValue(selected));
        }

        Set<Permission> getSelectedPermissions() {
            return leafNodes.stream()
                    .filter(n -> n.getCheckbox().getValue())
                    .map(PermissionNode::getPermission)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(Permission.class)));
        }
    }

    private static class PermissionNode extends HorizontalLayout {

        private final Checkbox checkbox;
        private final Permission permission;

        PermissionNode(String label, Permission permission, boolean checked) {
            this.permission = permission;
            this.checkbox = new Checkbox(label, checked);
            checkbox.getStyle().set("margin-left", "var(--lumo-space-l)");
            setPadding(false);
            setSpacing(false);
            add(checkbox);
        }

        Checkbox getCheckbox() { return checkbox; }
        Permission getPermission() { return permission; }
    }

    private static class SubGroupNode extends VerticalLayout {

        private final PermissionNode parentNode;
        private final List<PermissionNode> actionNodes;

        SubGroupNode(String viewLabel, Permission viewPermission,
                     Map<String, Permission> actions, Set<Permission> initial) {
            parentNode = new PermissionNode(viewLabel, viewPermission, initial.contains(viewPermission));
            actionNodes = actions.entrySet().stream()
                    .map(e -> {
                        PermissionNode node = new PermissionNode(e.getKey(), e.getValue(), initial.contains(e.getValue()));
                        node.getCheckbox().getStyle().set("margin-left", "var(--lumo-space-xl)");
                        return node;
                    })
                    .toList();

            // Unchecking view unchecks all actions — actions require view to be meaningful
            parentNode.getCheckbox().addValueChangeListener(ev -> {
                if (!ev.getValue()) actionNodes.forEach(a -> a.getCheckbox().setValue(false));
            });

            setPadding(false);
            setSpacing(false);
            add(parentNode);
            actionNodes.forEach(this::add);
        }

        List<PermissionNode> allLeaves() {
            List<PermissionNode> all = new ArrayList<>();
            all.add(parentNode);
            all.addAll(actionNodes);
            return all;
        }
    }

    private static class GroupNode extends VerticalLayout {

        private final Checkbox header;
        private final Button toggleButton;
        private final VerticalLayout itemsContainer;
        private final List<PermissionNode> children;
        private boolean syncing = false;

        GroupNode(String label, List<PermissionNode> leaves, List<Component> displayItems) {
            this.children = leaves;
            this.header = new Checkbox(label);
            header.getStyle().set("font-weight", "500");
            header.addValueChangeListener(e -> {
                if (syncing) return;
                children.forEach(c -> c.getCheckbox().setValue(e.getValue()));
            });

            itemsContainer = new VerticalLayout();
            itemsContainer.setPadding(false);
            itemsContainer.setSpacing(false);
            itemsContainer.getStyle().set("margin-left", "var(--lumo-size-s)");
            displayItems.forEach(itemsContainer::add);

            var initialIcon = VaadinIcon.CHEVRON_RIGHT.create();
            initialIcon.setSize(UiConstants.ICON_SIZE);
            toggleButton = new Button(initialIcon, e -> setExpanded(!itemsContainer.isVisible()));
            toggleButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);

            HorizontalLayout headerRow = new HorizontalLayout(toggleButton, header);
            headerRow.setSpacing(false);
            headerRow.setPadding(false);
            headerRow.setAlignItems(Alignment.CENTER);

            setPadding(false);
            setSpacing(false);
            add(headerRow, itemsContainer);
            syncState();
            setExpanded(children.stream().anyMatch(c -> c.getCheckbox().getValue()));
        }

        void setExpanded(boolean expanded) {
            itemsContainer.setVisible(expanded);
            var icon = (expanded ? VaadinIcon.CHEVRON_DOWN : VaadinIcon.CHEVRON_RIGHT).create();
            icon.setSize(UiConstants.ICON_SIZE);
            toggleButton.setIcon(icon);
            toggleButton.getElement().setAttribute("title", expanded ? "Collapse" : "Expand");
        }

        void syncState() {
            syncing = true;
            long checked = children.stream().filter(c -> c.getCheckbox().getValue()).count();
            if (checked == 0) {
                header.setIndeterminate(false);
                header.setValue(false);
            } else if (checked == children.size()) {
                header.setIndeterminate(false);
                header.setValue(true);
            } else {
                header.setIndeterminate(true);
            }
            syncing = false;
        }
    }
}
