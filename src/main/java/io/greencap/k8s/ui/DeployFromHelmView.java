package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.helm.HelmRepository;
import io.greencap.k8s.domain.helm.HelmRepositoryService;
import io.greencap.k8s.domain.user.UserService;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.HelmOperationException;
import io.greencap.k8s.kubernetes.HelmService;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Route(value = "deploy/helm", layout = MainLayout.class)
@PageTitle("Deploy from Helm — GreenCap K8s")
@PermitAll
public class DeployFromHelmView extends VerticalLayout implements BeforeEnterObserver {

    private final HelmService helmService;
    private final HelmRepositoryService repositoryService;
    private final ClusterContext clusterContext;
    private final UserService userService;

    private int currentStep = 1;

    // Step 1
    private ComboBox<HelmRepository> repoSelect;
    private TextField chartField;
    private TextField versionField;

    // Step 2
    private TextField releaseNameField;
    private TextField namespaceField;

    // Step 3
    private CodeMirrorEditor valuesArea;
    private Pre outputPre;

    private final Div stepContent = new Div();
    private final Span stepIndicator = new Span();
    private final Button backBtn    = new Button(VaadinIcon.ARROW_LEFT.create());
    private final Button nextBtn    = new Button("Next",    VaadinIcon.ARROW_RIGHT.create());
    private final Button installBtn = new Button("Install", VaadinIcon.PACKAGE.create());

    public DeployFromHelmView(HelmService helmService, HelmRepositoryService repositoryService,
                              ClusterContext clusterContext, UserService userService) {
        this.helmService       = helmService;
        this.repositoryService = repositoryService;
        this.clusterContext    = clusterContext;
        this.userService       = userService;

        setPadding(true);
        setSpacing(true);
        setMaxWidth("820px");
        setWidthFull();

        initModeSelector();
        initStepIndicator();
        initStepContent();
        initNavigation();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        loadRepositories();
        namespaceField.setValue(clusterContext.getNamespace() != null ? clusterContext.getNamespace() : "");
        renderStep(1);
    }

    private void initModeSelector() {
        Button imageBtn      = new Button("Deploy from Image",      VaadinIcon.ROCKET.create());
        Button dockerfileBtn = new Button("Deploy from Dockerfile", VaadinIcon.CODE.create());
        Button composeBtn    = new Button("Deploy from Compose",    VaadinIcon.FILE_CODE.create());
        Button helmBtn       = new Button("Deploy from Helm",       VaadinIcon.PACKAGE.create());

        imageBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        dockerfileBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        composeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        helmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        imageBtn.addClickListener(e -> UI.getCurrent().navigate(DeployApplicationView.class));
        dockerfileBtn.addClickListener(e -> UI.getCurrent().navigate(DeployFromDockerfileView.class));
        composeBtn.addClickListener(e -> UI.getCurrent().navigate(ImportComposeView.class));

        add(new HorizontalLayout(imageBtn, dockerfileBtn, composeBtn, helmBtn) {{ setSpacing(true); }});
    }

    private void initStepIndicator() {
        stepIndicator.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        add(stepIndicator);
    }

    private void initStepContent() {
        // Step 1
        repoSelect   = new ComboBox<>("Repository");
        repoSelect.setItemLabelGenerator(HelmRepository::getName);
        repoSelect.setWidthFull();
        repoSelect.setRequired(true);

        chartField   = new TextField("Chart Name");
        chartField.setWidthFull();
        chartField.setRequired(true);
        chartField.setPlaceholder("e.g. nginx");

        versionField = new TextField("Version");
        versionField.setWidthFull();
        versionField.setPlaceholder("latest");

        // Step 2
        releaseNameField = new TextField("Release Name");
        releaseNameField.setWidthFull();
        releaseNameField.setRequired(true);
        releaseNameField.setHelperText("Lowercase letters, numbers and hyphens only");

        namespaceField = new TextField("Namespace");
        namespaceField.setWidthFull();
        namespaceField.setRequired(true);

        chartField.addValueChangeListener(e -> {
            if (releaseNameField.getValue().isBlank()) {
                releaseNameField.setValue(e.getValue().toLowerCase().replaceAll("[^a-z0-9-]", "-"));
            }
        });

        // Step 3
        valuesArea = new CodeMirrorEditor();
        valuesArea.setWidthFull();
        valuesArea.setHeight("280px");

        outputPre = new Pre();
        outputPre.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("overflow", "auto")
                .set("white-space", "pre-wrap")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("min-height", "100px");
        outputPre.setVisible(false);

        stepContent.setWidthFull();
    }

    private void initNavigation() {
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backBtn.setVisible(false);

        nextBtn.setIconAfterText(true);
        nextBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        installBtn.setIconAfterText(true);
        installBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        installBtn.setVisible(false);

        backBtn.addClickListener(e -> renderStep(currentStep - 1));
        nextBtn.addClickListener(e -> {
            if (validateCurrentStep()) renderStep(currentStep + 1);
        });
        installBtn.addClickListener(e -> executeInstall());

        Div spacer = new Div();
        HorizontalLayout nav = new HorizontalLayout(backBtn, spacer, nextBtn, installBtn);
        nav.expand(spacer);
        nav.setWidthFull();
        nav.setAlignItems(FlexComponent.Alignment.CENTER);
        nav.addClassNames(LumoUtility.Padding.Vertical.MEDIUM);

        stepContent.setWidthFull();
        add(stepContent, nav);
    }

    private void renderStep(int step) {
        currentStep = step;
        stepContent.removeAll();
        outputPre.setVisible(false);

        stepIndicator.setText("Step " + step + " of 3");

        backBtn.setVisible(step > 1);
        nextBtn.setVisible(step < 3);
        installBtn.setVisible(step == 3);

        switch (step) {
            case 1 -> {
                H4 title = new H4("Chart");
                FormLayout form = new FormLayout(repoSelect, chartField, versionField);
                form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
                stepContent.add(title, form);
            }
            case 2 -> {
                H4 title = new H4("Config");
                FormLayout form = new FormLayout(releaseNameField, namespaceField);
                form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
                stepContent.add(title, form);
            }
            case 3 -> {
                H4 title = new H4("Values & Install");
                stepContent.add(title, valuesArea, outputPre);
            }
        }
    }

    private boolean validateCurrentStep() {
        if (currentStep == 1) {
            if (repoSelect.getValue() == null) { repoSelect.setInvalid(true); return false; }
            if (chartField.getValue().isBlank()) { chartField.setInvalid(true); return false; }
        }
        if (currentStep == 2) {
            if (releaseNameField.getValue().isBlank()) { releaseNameField.setInvalid(true); return false; }
            if (namespaceField.getValue().isBlank()) { namespaceField.setInvalid(true); return false; }
        }
        return true;
    }

    private void executeInstall() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;

        installBtn.setEnabled(false);
        outputPre.setText("Installing...");
        outputPre.setVisible(true);

        HelmRepository repo    = repoSelect.getValue();
        String chart           = chartField.getValue().trim();
        String version         = versionField.getValue().trim();
        String releaseName     = releaseNameField.getValue().trim();
        String namespace       = namespaceField.getValue().trim();
        String values          = valuesArea.getValue();

        UI ui = UI.getCurrent();
        UiConstants.VIRTUAL_THREADS.execute(() -> {
            try {
                String output = helmService.install(cluster, namespace, repo.getName(), chart,
                        version.isBlank() ? null : version, releaseName, values);
                ui.access(() -> {
                    outputPre.setText(output);
                    clusterContext.setNamespace(namespace);
                    String username = SecurityContextHolder.getContext().getAuthentication().getName();
                    userService.updateActiveNamespace(username, namespace);
                    Notification.show("Release \"" + releaseName + "\" installed successfully.",
                            3000, Notification.Position.BOTTOM_END);
                    UI.getCurrent().navigate(HelmReleasesView.class);
                });
            } catch (HelmOperationException e) {
                ui.access(() -> {
                    outputPre.setText("Error:\n" + e.getMessage());
                    installBtn.setEnabled(true);
                    Notification n = Notification.show("Install failed: " + e.getMessage(),
                            6000, Notification.Position.BOTTOM_END);
                    n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                });
            }
        });
    }

    private void loadRepositories() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        List<HelmRepository> repos = repositoryService.listRepositories(cluster);
        repoSelect.setItems(repos);
        if (repos.size() == 1) repoSelect.setValue(repos.get(0));
    }
}
