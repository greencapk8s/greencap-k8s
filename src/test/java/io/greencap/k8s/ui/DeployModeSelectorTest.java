package io.greencap.k8s.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.greencap.k8s.KaribuTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static org.assertj.core.api.Assertions.assertThat;

class DeployModeSelectorTest extends KaribuTest {

    @Test
    void build_alwaysOrdersButtons_dockerfileComposeImageHelm() {
        HorizontalLayout selector = DeployModeSelector.build(ImportComposeView.class);

        assertThat(_find(selector, Button.class)).extracting(Button::getText)
                .containsExactly("Deploy from Dockerfile", "Deploy from Compose", "Deploy from Image", "Deploy from Helm");
    }

    @Test
    void build_marksOnlyTheCurrentViewButton_asPrimary() {
        HorizontalLayout selector = DeployModeSelector.build(ImportComposeView.class);
        List<Button> buttons = _find(selector, Button.class);

        Button composeButton = buttons.get(1);
        assertThat(composeButton.getThemeNames()).contains("primary");

        for (Button other : List.of(buttons.get(0), buttons.get(2), buttons.get(3))) {
            assertThat(other.getThemeNames()).contains("tertiary").doesNotContain("primary");
        }
    }

    @Test
    void build_forDifferentCurrentView_movesThePrimaryHighlightAccordingly() {
        HorizontalLayout selector = DeployModeSelector.build(DeployFromDockerfileView.class);
        List<Button> buttons = _find(selector, Button.class);

        assertThat(buttons.get(0).getThemeNames()).contains("primary");
        assertThat(buttons.get(1).getThemeNames()).contains("tertiary");
    }
}
