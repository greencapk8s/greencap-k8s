package io.greencap.k8s.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.greencap.k8s.domain.user.Role;
import io.greencap.k8s.domain.user.User;
import io.greencap.k8s.domain.user.UserService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.format.DateTimeFormatter;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("Users — GreenCap K8s")
@RolesAllowed("ADMIN")
public class UserManagementView extends VerticalLayout implements BeforeEnterObserver {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserService userService;
    private final Grid<User> grid = new Grid<>(User.class, false);

    public UserManagementView(UserService userService) {
        this.userService = userService;
        setSizeFull();
        add(buildToolbar(), buildGrid());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        refreshGrid();
    }

    private HorizontalLayout buildToolbar() {
        Button addBtn = new Button("Add User", VaadinIcon.PLUS.create(), e -> openAddDialog());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(new H2("Users"), addBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        toolbar.expand(new H2("Users"));
        toolbar.setWidthFull();
        return toolbar;
    }

    private Grid<User> buildGrid() {
        grid.addColumn(User::getUsername).setHeader("Username").setSortable(true).setFlexGrow(1).setResizable(true);
        grid.addColumn(User::getEmail).setHeader("Email").setFlexGrow(2).setResizable(true);
        grid.addColumn(u -> {
            String r = u.getRole().name();
            return r.charAt(0) + r.substring(1).toLowerCase();
        }).setHeader("Role").setWidth("110px").setResizable(true);
        grid.addComponentColumn(this::activeBadge).setHeader("Status").setWidth("100px").setResizable(true);
        grid.addColumn(u -> u.getCreatedAt().format(DATE_FORMATTER)).setHeader("Created").setWidth("160px").setResizable(true);
        grid.addComponentColumn(this::buildActions).setHeader("").setWidth("90px").setFlexGrow(0);
        grid.setSizeFull();
        return grid;
    }

    private Span activeBadge(User user) {
        Span badge = new Span(user.isActive() ? "Active" : "Inactive");
        badge.getElement().getThemeList().add("badge");
        if (user.isActive()) {
            badge.getElement().getThemeList().add("success");
        } else {
            badge.getElement().getThemeList().add("contrast");
        }
        return badge;
    }

    private HorizontalLayout buildActions(User user) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isSelf = user.getUsername().equals(currentUsername);

        var deactivateIcon = VaadinIcon.BAN.create();
        deactivateIcon.setSize(UiConstants.ICON_SIZE);
        Button deactivateBtn = new Button(deactivateIcon, e -> confirmDeactivate(user));
        deactivateBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
        deactivateBtn.getElement().setAttribute("title", "Deactivate user");
        deactivateBtn.setEnabled(user.isActive() && !isSelf);

        return new HorizontalLayout(deactivateBtn);
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

        Select<Role> roleSelect = new Select<>();
        roleSelect.setLabel("Role");
        roleSelect.setItems(Role.values());
        roleSelect.setItemLabelGenerator(r -> r.name().charAt(0) + r.name().substring(1).toLowerCase());
        roleSelect.setValue(Role.VIEWER);
        roleSelect.setWidthFull();

        FormLayout form = new FormLayout(usernameField, emailField, passwordField, roleSelect);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("New User");
        dialog.setWidth("420px");
        dialog.add(form);

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
                        roleSelect.getValue()
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

    private void refreshGrid() {
        grid.setItems(userService.findAll());
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
