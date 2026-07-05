package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.cluster.ClusterService;
import io.greencap.k8s.domain.user.User;
import io.greencap.k8s.domain.user.UserService;
import io.greencap.k8s.kubernetes.UserProvisioningService;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Route(value = "users", layout = MainLayout.class)
@PageTitle("Users — GreenCap K8s")
@PermitAll
public class UserManagementView extends VerticalLayout implements BeforeEnterObserver {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String HELP_TITLE = "Users";
    private static final String HELP_TEXT = "Users are the accounts that can log into GreenCap. " +
            "Each user is backed by a Kubernetes ServiceAccount bound to a ClusterRole in their assigned cluster. " +
            "Use \"Add User\" to provision a new user and their K8s identity, " +
            "\"Edit Role\" to change the ClusterRole, or \"Remove\" to permanently delete the user and revoke their K8s identity.";

    private final UserService userService;
    private final ClusterService clusterService;
    private final UserProvisioningService userProvisioningService;
    private final Grid<User> grid = new Grid<>(User.class, false);

    public UserManagementView(UserService userService, ClusterService clusterService,
                              UserProvisioningService userProvisioningService,
                              GridSelectionMemory selectionMemory) {
        this.userService = userService;
        this.clusterService = clusterService;
        this.userProvisioningService = userProvisioningService;

        setSizeFull();
        setPadding(true);

        buildGrid();
        UiConstants.configureSingleSelection(grid, selectionMemory, getClass().getSimpleName(), User::getUsername);

        List<UiConstants.SelectionAction<User>> selectionActions = List.of(
                UiConstants.SelectionAction.of(VaadinIcon.EDIT, "Edit Role", true, this::openEditRoleDialog),
                UiConstants.SelectionAction.destructive(VaadinIcon.TRASH, "Remove", true, this::confirmRemove)
        );

        List<Button> extraButtons = new ArrayList<>();
        Button addBtn = new Button("Add User", VaadinIcon.PLUS.create());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        addBtn.addClickListener(e -> openAddDialog());
        extraButtons.add(addBtn);

        add(UiConstants.buildSectionHeader("Users", this::refreshGrid, HELP_TITLE, HELP_TEXT,
                        grid, selectionActions, extraButtons),
                grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.isAdmin()) {
            // Notification.show() during the same beforeEnter() call that forwards away is
            // silently dropped by Flow — the reroute discards the pending client push before
            // it's sent. Deferring via UI.access() runs it in its own push cycle, after the
            // forward has committed.
            UI ui = UI.getCurrent();
            ui.access(() -> notify("Access restricted to administrators", NotificationVariant.LUMO_WARNING));
            event.forwardTo("");
            return;
        }
        refreshGrid();
    }

    private void buildGrid() {
        grid.addColumn(User::getUsername).setHeader("Username").setSortable(true).setFlexGrow(1).setResizable(true);
        grid.addColumn(User::getEmail).setHeader("Email").setFlexGrow(2).setResizable(true);
        grid.addComponentColumn(this::clusterBadge).setHeader("Cluster").setWidth("160px").setResizable(true);
        grid.addComponentColumn(this::roleBadge).setHeader("Role").setWidth("160px").setResizable(true);
        grid.addColumn(u -> u.getCreatedAt().format(DATE_FORMATTER)).setHeader("Created").setWidth("160px").setResizable(true);
        grid.setSizeFull();
    }

    private Span clusterBadge(User user) {
        String clusterName = user.getActiveCluster() != null ? user.getActiveCluster().getName() : "—";
        Span badge = new Span(clusterName);
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add("contrast");
        return badge;
    }

    private Span roleBadge(User user) {
        String role = user.getClusterRoleName() != null ? user.getClusterRoleName() : "—";
        Span badge = new Span(role);
        badge.getElement().getThemeList().add("badge");
        if (user.getClusterRoleName() != null) {
            badge.getElement().getThemeList().add("success");
        }
        return badge;
    }

    private void confirmRemove(User user) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getUsername().equals(currentUsername)) {
            notify("Cannot remove your own account", NotificationVariant.LUMO_WARNING);
            return;
        }
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Remove user");
        dialog.add(new com.vaadin.flow.component.html.Paragraph(
                "Permanently remove \"" + user.getUsername() + "\"? Their ServiceAccount and ClusterRoleBinding will be deleted from the cluster."));

        Button confirmBtn = new Button("Remove", e -> {
            try {
                Cluster cluster = user.getActiveCluster();
                if (cluster != null) {
                    userProvisioningService.deprovisionUser(user, cluster);
                }
                userService.deleteUser(user.getId());
                dialog.close();
                refreshGrid();
                notify("User " + user.getUsername() + " removed", NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                log.error("Failed to remove user {}", user.getUsername(), ex);
                notify("Failed to remove user: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
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

        ComboBox<Cluster> clusterField = new ComboBox<>("Cluster");
        clusterField.setItems(clusterService.findAll());
        clusterField.setItemLabelGenerator(Cluster::getName);
        clusterField.setRequired(true);
        clusterField.setWidthFull();

        ComboBox<String> roleField = new ComboBox<>("ClusterRole");
        roleField.setRequired(true);
        roleField.setWidthFull();
        roleField.setEnabled(false);

        clusterField.addValueChangeListener(e -> {
            Cluster selected = e.getValue();
            if (selected != null) {
                try {
                    List<String> roles = userProvisioningService.listClusterRoles(selected);
                    roleField.setItems(roles);
                    roleField.setEnabled(true);
                } catch (Exception ex) {
                    notify("Failed to load ClusterRoles: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
                }
            } else {
                roleField.setItems(List.of());
                roleField.setEnabled(false);
            }
        });

        FormLayout form = new FormLayout(usernameField, emailField, passwordField, clusterField, roleField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("New User");
        dialog.setWidth("480px");
        dialog.add(form);

        Button saveBtn = new Button("Save", e -> {
            boolean valid = true;
            if (usernameField.isEmpty()) { usernameField.setInvalid(true); valid = false; }
            if (emailField.isEmpty())    { emailField.setInvalid(true);    valid = false; }
            if (passwordField.isEmpty()) { passwordField.setInvalid(true); valid = false; }
            if (clusterField.isEmpty())  { clusterField.setInvalid(true);  valid = false; }
            if (roleField.isEmpty())     { roleField.setInvalid(true);     valid = false; }
            if (!valid) return;

            try {
                User created = userService.createUser(
                        usernameField.getValue().trim(),
                        emailField.getValue().trim(),
                        passwordField.getValue()
                );
                Cluster cluster = clusterField.getValue();
                userService.updateActiveCluster(created.getUsername(), cluster);
                userProvisioningService.provisionUser(created, cluster, roleField.getValue());
                dialog.close();
                refreshGrid();
                notify("User " + created.getUsername() + " created", NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                log.error("Failed to create user", ex);
                notify("Failed to create user: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
        usernameField.focus();
    }

    private void openEditRoleDialog(User user) {
        if (DEFAULT_ADMIN_USERNAME.equals(user.getUsername())) {
            notify("Admin user does not use a ClusterRole — it uses the cluster kubeconfig directly", NotificationVariant.LUMO_WARNING);
            return;
        }
        Cluster cluster = user.getActiveCluster();
        if (cluster == null) {
            notify("User has no cluster assigned", NotificationVariant.LUMO_WARNING);
            return;
        }

        ComboBox<String> roleField = new ComboBox<>("ClusterRole");
        roleField.setWidthFull();
        try {
            List<String> roles = userProvisioningService.listClusterRoles(cluster);
            roleField.setItems(roles);
            if (user.getClusterRoleName() != null) {
                roleField.setValue(user.getClusterRoleName());
            }
        } catch (Exception ex) {
            notify("Failed to load ClusterRoles: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Role — " + user.getUsername());
        dialog.setWidth("400px");
        dialog.add(roleField);

        Button saveBtn = new Button("Save", e -> {
            if (roleField.isEmpty()) { roleField.setInvalid(true); return; }
            try {
                userProvisioningService.updateClusterRole(user, cluster, roleField.getValue());
                dialog.close();
                refreshGrid();
                notify("Role updated for " + user.getUsername(), NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                log.error("Failed to update role for user {}", user.getUsername(), ex);
                notify("Failed to update role: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
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
}
