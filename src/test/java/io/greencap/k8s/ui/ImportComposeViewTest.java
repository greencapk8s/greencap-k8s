package io.greencap.k8s.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import io.greencap.k8s.KaribuTest;
import io.greencap.k8s.domain.user.UserService;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.ObservabilityService;
import io.greencap.k8s.kubernetes.RegistryService;
import io.greencap.k8s.kubernetes.StorageService;
import io.greencap.k8s.kubernetes.compose.ComposeParser;
import io.greencap.k8s.kubernetes.compose.ImportComposeService;
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
class ImportComposeViewTest extends KaribuTest {

    @Mock private ClusterContext clusterContext;
    @Mock private ImportComposeService importComposeService;
    @Mock private RegistryService registryService;
    @Mock private ObservabilityService observabilityService;
    @Mock private StorageService storageService;
    @Mock private UserService userService;

    private ImportComposeView view;

    @BeforeEach
    void setupView() {
        lenient().when(clusterContext.getCluster()).thenReturn(null);
        loginAs("GLOBAL_DEPLOY");
        view = new ImportComposeView(clusterContext, new ComposeParser(), importComposeService,
                registryService, observabilityService, storageService, userService);
        view.beforeEnter(null);
    }

    @Test
    void gitIsTheDefaultSourceAndThePickerStaysHidden() {
        assertThat(originSelector().getValue()).isEqualTo(BuildContextOrigin.GIT_REPOSITORY);
        assertThat(visibleFieldLabels()).contains("Git repository URL", "Branch", "Path to docker-compose.yml");
        assertThat(_find(view, BuildContextPicker.class)).isEmpty();
    }

    // The Compose path survives the switch to a local folder: it is how the user points at a file whose
    // name or location departs from the default, now that picking the file on its own is gone.
    @Test
    void choosingLocalFolderHidesTheGitFieldsButKeepsTheComposePath() {
        selectLocalOrigin();

        assertThat(visibleFieldLabels()).doesNotContain("Git repository URL", "Branch");
        assertThat(visibleFieldLabels()).contains("Path to docker-compose.yml", "Target namespace");
        assertThat(_find(view, BuildContextPicker.class)).hasSize(1);
    }

    @Test
    void switchingOriginLeavesNoResidualStateBehind() {
        gitUrlField().setValue("https://github.com/usuario/repo");

        selectLocalOrigin();
        BuildContextPicker picker = picker();
        picker.contextPacked("minha-app", 12, 4096);
        assertThat(picker.getFolderName()).contains("minha-app");

        originSelector().setValue(BuildContextOrigin.GIT_REPOSITORY);
        assertThat(picker.getFolderName()).isEmpty();
        assertThat(gitUrlField().getValue()).isEmpty();
    }

    @Test
    void localOriginDoesNotAdvanceWhileNoFolderIsSelected() {
        selectLocalOrigin();
        namespaceField().setValue("minha-app");

        clickNext();

        assertThat(visibleFieldLabels()).contains("Target namespace");
    }

    @Test
    void namespaceIsSuggestedFromTheSelectedFolderName() {
        selectLocalOrigin();

        picker().contextPacked("Minha Pasta", 12, 4096);

        assertThat(namespaceField().getValue()).isEqualTo("minha-pasta");
    }

    // The picker must sit outside the container each step replaces: detaching it discards the uploaded
    // archive and the folder the browser still holds, so the Build would start with nothing.
    @Test
    void thePickerLivesOutsideTheContainerReplacedOnEveryStep() {
        selectLocalOrigin();

        assertThat(picker().getParent().flatMap(Component::getParent)).contains(view);
    }

    private void selectLocalOrigin() {
        originSelector().setValue(BuildContextOrigin.LOCAL_FOLDER);
    }

    private void clickNext() {
        _get(view, Button.class, spec -> spec.withPredicate(
                button -> "Next".equals(button.getText()) || "Deploy".equals(button.getText()))).click();
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
