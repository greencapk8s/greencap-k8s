package io.greencap.k8s.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import io.greencap.k8s.KaribuTest;
import io.greencap.k8s.domain.user.UserService;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.DeployApplicationService;
import io.greencap.k8s.kubernetes.DockerfileParser;
import io.greencap.k8s.kubernetes.NetworkingService;
import io.greencap.k8s.kubernetes.ObservabilityService;
import io.greencap.k8s.kubernetes.RegistryService;
import io.greencap.k8s.kubernetes.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DeployFromDockerfileViewTest extends KaribuTest {

    @Mock private ClusterContext clusterContext;
    @Mock private RegistryService registryService;
    @Mock private ObservabilityService observabilityService;
    @Mock private DeployApplicationService deployApplicationService;
    @Mock private StorageService storageService;
    @Mock private NetworkingService networkingService;
    @Mock private UserService userService;
    @Mock private DockerfileParser dockerfileParser;

    private DeployFromDockerfileView view;

    @BeforeEach
    void setupView() {
        // No active cluster: the wizard renders step 1 without firing the async StorageClass lookups.
        lenient().when(clusterContext.getCluster()).thenReturn(null);
        loginAs("GLOBAL_DEPLOY");
        view = new DeployFromDockerfileView(clusterContext, registryService, observabilityService,
                deployApplicationService, storageService, networkingService, userService, dockerfileParser);
        view.beforeEnter(null);
    }

    @Test
    void gitIsTheDefaultOriginAndTheFolderPickerStaysHidden() {
        assertThat(originSelector().getValue()).isEqualTo(BuildContextOrigin.GIT_REPOSITORY);
        assertThat(visibleFieldLabels()).contains("Git repository URL", "Branch");
        assertThat(_find(view, BuildContextPicker.class)).isEmpty();
    }

    @Test
    void choosingLocalFolderHidesTheGitFieldsAndShowsThePicker() {
        selectLocalFolderOrigin();

        assertThat(visibleFieldLabels()).doesNotContain("Git repository URL", "Branch");
        assertThat(visibleFieldLabels()).contains("Dockerfile path", "Context path", "Target namespace");
        assertThat(_find(view, BuildContextPicker.class)).hasSize(1);
    }

    @Test
    void switchingOriginLeavesNoResidualStateBehind() {
        gitUrlField().setValue("https://github.com/usuario/repo");

        selectLocalFolderOrigin();
        BuildContextPicker picker = picker();
        picker.contextPacked("Minha Pasta", 12, 4096);
        assertThat(picker.getFolderName()).contains("Minha Pasta");

        originSelector().setValue(BuildContextOrigin.GIT_REPOSITORY);
        assertThat(picker.getFolderName()).isEmpty();
        assertThat(gitUrlField().getValue()).isEmpty();
    }

    @Test
    void localFolderOriginDoesNotAdvanceWhileNoFolderIsSelected() {
        selectLocalFolderOrigin();
        namespaceField().setValue("minha-app");

        clickNext();

        assertThat(visibleFieldLabels()).contains("Target namespace");
        assertThat(visibleFieldLabels()).doesNotContain("Application name", "Image tag");
    }

    @Test
    void aRejectedContextClearsTheSelectionAndBlocksTheStep() {
        selectLocalFolderOrigin();
        namespaceField().setValue("minha-app");
        picker().contextPacked("Minha Pasta", 12, 4096);

        // The browser refuses an oversized context and reports the selection as gone.
        picker().selectionCleared();
        clickNext();

        assertThat(picker().getPackedFolder()).isEmpty();
        assertThat(visibleFieldLabels()).doesNotContain("Application name", "Image tag");
    }

    // Vaadin only exempts ".../<uiId>/<securityKey>/upload" from Spring Security's CSRF filter
    // (HandlerHelper.isUploadRequest); any other resource name makes the browser's POST fail with 403.
    @Test
    void theUploadTargetKeepsTheResourceNameVaadinExemptsFromCsrf() {
        selectLocalFolderOrigin();

        String target = picker().getElement().getAttribute("target");

        assertThat(target).matches("VAADIN/dynamic/resource/\\d+/[0-9a-z-]*/upload");
    }

    // The picker must sit outside the container each step replaces: detaching it discards the uploaded
    // archive and the folder the browser still holds, so the Build would start with nothing.
    @Test
    void thePickerLivesOutsideTheContainerReplacedOnEveryStep() {
        selectLocalFolderOrigin();

        assertThat(picker().getParent().flatMap(Component::getParent)).contains(view);
    }

    @Test
    void namespaceIsSuggestedFromTheSelectedFolderName() {
        selectLocalFolderOrigin();

        picker().contextPacked("Minha Pasta", 12, 4096);

        assertThat(namespaceField().getValue()).isEqualTo("minha-pasta");
    }

    private void selectLocalFolderOrigin() {
        originSelector().setValue(BuildContextOrigin.LOCAL_FOLDER);
    }

    private void clickNext() {
        _get(view, Button.class, spec -> spec.withText("Next")).click();
    }

    @SuppressWarnings("unchecked")
    private RadioButtonGroup<BuildContextOrigin> originSelector() {
        return _get(view, RadioButtonGroup.class);
    }

    private BuildContextPicker picker() {
        return _get(view, BuildContextPicker.class);
    }

    private TextField gitUrlField() {
        return fieldWithLabel("Git repository URL");
    }

    private TextField namespaceField() {
        return fieldWithLabel("Target namespace");
    }

    private TextField fieldWithLabel(String label) {
        return _get(view, TextField.class, spec -> spec.withPredicate(field -> label.equals(field.getLabel())));
    }

    private List<String> visibleFieldLabels() {
        return _find(view, TextField.class).stream().map(TextField::getLabel).toList();
    }
}
