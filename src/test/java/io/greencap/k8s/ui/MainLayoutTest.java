package io.greencap.k8s.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.sidenav.SideNavItem;
import io.greencap.k8s.KaribuTest;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.cluster.ClusterService;
import io.greencap.k8s.domain.user.UserService;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.NamespaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.BuildProperties;

import java.util.List;
import java.util.Optional;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MainLayoutTest extends KaribuTest {

    // MainLayout builds a SideNavItem per menu entry, and SideNavItem(label, view, icon) eagerly
    // resolves the view's @Route — routes must be registered, unlike the plain MockVaadin.setup()
    // the other view tests rely on. Assertions here stay scoped to `view` (never a global _get),
    // so they're unaffected by MockVaadin's initial (and unavoidably failing, since none of this
    // app's Spring-injected views have a no-arg constructor) auto-navigation to "".
    private static final Routes ROUTES = new Routes().autoDiscoverViews("io.greencap.k8s");

    @Mock private ClusterContext clusterContext;
    @Mock private UserService userService;
    @Mock private NamespaceService namespaceService;
    @Mock private ClusterService clusterService;
    @Mock private BuildProperties buildProperties;

    private MainLayout view;

    @BeforeEach
    void setupView() {
        MockVaadin.tearDown();
        MockVaadin.setup(ROUTES);
        loginAs("USER");
        view = new MainLayout(clusterContext, userService, namespaceService, clusterService, buildProperties,
                new ObjectMapper());
    }

    @Test
    void header_showsLoggedInUsername() {
        List<Span> spans = _find(view, Span.class);
        assertThat(spans).extracting(Span::getText).contains("testuser");
    }

    @Test
    void header_keepsUsernameVisible_whenNoClusterIsActive() {
        view.updateClusterInfo();

        List<Span> spans = _find(view, Span.class);
        assertThat(spans).extracting(Span::getText).contains("testuser", "No active cluster");
    }

    @Test
    void drawer_hasDeveloperExperienceAsFirstSection() {
        List<String> sectionLabels = _find(view, Span.class).stream()
                .map(Span::getText)
                .filter(text -> List.of("DEVELOPER EXPERIENCE", "PROJECT", "GLOBAL", "SETTINGS").contains(text))
                .toList();

        assertThat(sectionLabels).containsExactly("DEVELOPER EXPERIENCE", "PROJECT", "GLOBAL", "SETTINGS");
    }

    @Test
    void everyTourTarget_isReachableInTheComponentTree() {
        List<String> missingTargets = TourTargets.ALL.stream()
                .filter(tourId -> _find(view, Component.class, spec -> spec.withId(tourId)).isEmpty())
                .toList();

        assertThat(missingTargets).isEmpty();
    }

    @Test
    void developerExperienceSection_hasNewApplicationBelowTemplatesCatalog() {
        List<String> labels = _find(view, SideNavItem.class).stream()
                .map(SideNavItem::getLabel)
                .filter(label -> "Templates Catalog".equals(label) || "New Application".equals(label))
                .toList();

        assertThat(labels).containsExactly("Templates Catalog", "New Application");
    }

    @Test
    void tourSteps_areSixForThePlatformAdmin() {
        assertThat(TourSteps.forUser(true)).hasSize(6);
    }

    @Test
    void tourSteps_mentionUsersOnlyForThePlatformAdmin() {
        assertThat(descriptionsOf(TourSteps.forUser(true))).anyMatch(text -> text.contains("Users"));
        assertThat(descriptionsOf(TourSteps.forUser(false))).noneMatch(text -> text.contains("Users"));
    }

    @Test
    void everyTourStep_pointsToADeclaredTarget() {
        assertThat(TourSteps.forUser(true))
                .extracting(TourStep::targetId)
                .isSubsetOf(TourTargets.ALL);
    }

    @Test
    void tour_startsForAFirstAccessUserWithAnActiveCluster() {
        when(userService.findTourSeen("testuser")).thenReturn(Optional.of(false));
        when(clusterContext.getCluster()).thenReturn(new Cluster());

        view.startTourIfFirstAccess();

        assertThat(deliveredSteps()).contains(TourTargets.ALL);
        assertThat(view.isDrawerOpened()).isTrue();
    }

    @Test
    void tour_doesNotStartWhenItWasAlreadySeen() {
        when(userService.findTourSeen("testuser")).thenReturn(Optional.of(true));

        view.startTourIfFirstAccess();

        assertThat(deliveredSteps()).isNull();
    }

    @Test
    void tour_doesNotStartWithoutAnActiveCluster() {
        when(userService.findTourSeen("testuser")).thenReturn(Optional.of(false));
        when(clusterContext.getCluster()).thenReturn(null);

        view.startTourIfFirstAccess();

        assertThat(deliveredSteps()).isNull();
    }

    private String deliveredSteps() {
        return _get(view, TourComponent.class).getElement().getProperty("steps");
    }

    private List<String> descriptionsOf(List<TourStep> steps) {
        return steps.stream().map(TourStep::description).toList();
    }
}
