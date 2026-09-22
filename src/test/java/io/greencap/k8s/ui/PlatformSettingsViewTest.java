package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.RouteConfiguration;
import io.greencap.k8s.KaribuTest;
import io.greencap.k8s.domain.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformSettingsViewTest extends KaribuTest {

    /**
     * Stands in for DashboardView so the router has a real target and the resulting Location can
     * be read. Registered dynamically rather than via @Route: MainLayoutTest auto-discovers
     * annotated routes across the whole classpath, and an annotation here would collide with the
     * real DashboardView.
     */
    public static class DashboardRouteStub extends Div {}

    @Mock private UserService userService;

    private PlatformSettingsView view;

    @BeforeEach
    void setupView() {
        RouteConfiguration.forApplicationScope().setRoute("", DashboardRouteStub.class);

        when(userService.findRefreshInterval("testuser")).thenReturn(Optional.empty());
        when(userService.findTheme("testuser")).thenReturn(Optional.empty());

        loginAs("USER");
        view = new PlatformSettingsView(userService);
        UI.getCurrent().add(view);
    }

    @Test
    void onboardingCard_sitsAlongsideTheOtherPreferenceCards() {
        List<String> cardTitles = _find(view, H3.class).stream().map(H3::getText).toList();

        assertThat(cardTitles).containsExactly("Refresh", "Appearance", "Onboarding");
    }

    @Test
    void startTourButton_landsOnTheDashboardRoute() {
        _click(_get(view, Button.class, spec -> spec.withText("Start tour")));

        // The stub only becomes the active view if the navigation really happened — asserting the
        // location alone would pass on a button that navigates nowhere, since "" is where the
        // test already starts.
        assertThat(_find(DashboardRouteStub.class)).hasSize(1);
        assertThat(UI.getCurrent().getInternals().getActiveViewLocation())
                .extracting(Location::getPath)
                .isEqualTo(DashboardView.ROUTE);
    }
}
