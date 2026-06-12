package io.greencap.k8s.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
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

    private final UserService userService;
    private final Grid<User> grid = new Grid<>(User.class, false);

    public UserManagementView(UserService userService) {
        this.userService = userService;
        setSizeFull();
        add(buildToolbar(), buildGrid());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.SETTINGS_USERS_VIEW)) {
            event.forwardTo("");
            return;
        }
        refreshGrid();
    }

    private HorizontalLayout buildToolbar() {
        Button addBtn = new Button("Add User", VaadinIcon.PLUS.create(), e -> openAddDialog());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBtn.setEnabled(SecurityUtils.hasPermission(Permission.SETTINGS_USERS_WRITE));

        H2 title = new H2("Users");
        HorizontalLayout toolbar = new HorizontalLayout(title, addBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        toolbar.expand(title);
        toolbar.setWidthFull();
        return toolbar;
    }

    private Grid<User> buildGrid() {
        grid.addColumn(User::getUsername).setHeader("Username").setSortable(true).setFlexGrow(1).setResizable(true);
        grid.addColumn(User::getEmail).setHeader("Email").setFlexGrow(2).setResizable(true);
        grid.addComponentColumn(this::permissionCountBadge).setHeader("Permissions").setWidth("140px").setResizable(true);
        grid.addComponentColumn(this::activeBadge).setHeader("Status").setWidth("100px").setResizable(true);
        grid.addColumn(u -> u.getCreatedAt().format(DATE_FORMATTER)).setHeader("Created").setWidth("160px").setResizable(true);
        UiConstants.addActionsColumn(grid, 2, this::buildActions);
        grid.setSizeFull();
        return grid;
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

    private List<Button> buildActions(User user) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isSelf = user.getUsername().equals(currentUsername);
        boolean canWrite = SecurityUtils.hasPermission(Permission.SETTINGS_USERS_WRITE);

        var editIcon = VaadinIcon.EDIT.create();
        editIcon.setSize(UiConstants.ICON_SIZE);
        Button editBtn = new Button(editIcon, e -> openEditPermissionsDialog(user));
        editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        editBtn.getElement().setAttribute("title", "Edit permissions");
        boolean isDefaultAdmin = DEFAULT_ADMIN_USERNAME.equals(user.getUsername());
        editBtn.setEnabled(canWrite && !isDefaultAdmin);
        if (isDefaultAdmin) {
            editBtn.getElement().setAttribute("title", "Default admin permissions are protected");
        }

        var deactivateIcon = VaadinIcon.BAN.create();
        deactivateIcon.setSize(UiConstants.ICON_SIZE);
        Button deactivateBtn = new Button(deactivateIcon, e -> confirmDeactivate(user));
        deactivateBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
        deactivateBtn.getElement().setAttribute("title", "Deactivate user");
        deactivateBtn.setEnabled(user.isActive() && !isSelf && canWrite);

        return List.of(editBtn, deactivateBtn);
    }

    private void confirmDeactivate(User user) {
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

    private void refreshGrid() {
        grid.setItems(userService.findAll());
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

            HorizontalLayout bulkActions = new HorizontalLayout(selectAllBtn, deselectAllBtn);
            bulkActions.setSpacing(true);
            bulkActions.setPadding(false);
            add(bulkActions);

            add(buildSection("PROJECT", buildProjectGroups(initial)));
            add(buildSection("OBSERVABILITY", buildObservabilityGroups(initial)));
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

            groups.add(buildGroup("Topology", Map.of(
                    "Topology", Permission.TOPOLOGY_VIEW
            ), initial));

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

        private List<GroupNode> buildObservabilityGroups(Set<Permission> initial) {
            return List.of(buildGroup("Observability", new LinkedHashMap<>() {{
                put("Dashboard", Permission.OBSERVABILITY_DASHBOARD_VIEW);
                put("Events", Permission.OBSERVABILITY_EVENTS_VIEW);
                put("Metrics", Permission.OBSERVABILITY_METRICS_VIEW);
            }}, initial));
        }

        private List<GroupNode> buildSettingsGroups(Set<Permission> initial) {
            List<GroupNode> groups = new ArrayList<>();

            groups.add(buildGroup("Clusters", new LinkedHashMap<>() {{
                put("Clusters (View)", Permission.SETTINGS_CLUSTERS_VIEW);
                put("Clusters (Write)", Permission.SETTINGS_CLUSTERS_WRITE);
            }}, initial));

            groups.add(buildGroup("Infrastructure", Map.of(
                    "Infrastructure", Permission.SETTINGS_INFRASTRUCTURE_VIEW
            ), initial));

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

            setPadding(false);
            setSpacing(false);
            add(header);
            displayItems.forEach(this::add);
            syncState();
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
