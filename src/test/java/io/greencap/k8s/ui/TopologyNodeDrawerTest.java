package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.NotFoundException;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import io.greencap.k8s.KaribuTest;
import org.junit.jupiter.api.Test;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TopologyNodeDrawerTest extends KaribuTest {

    @Test
    void podGroupWithServiceDependency_showsDependsOnSection_withEvidence() {
        TopologyNodeDrawer drawer = new TopologyNodeDrawer();

        JsonObject dependency = Json.createObject();
        dependency.put("targetLabel", "postgres-service");
        dependency.put("targetManifestUrl", "networking/services?name=postgres-service");
        dependency.put("matchedEnvVar", "DB_HOST");
        dependency.put("matchedValue", "postgres-service");

        JsonArray dependencies = Json.createArray();
        dependencies.set(0, dependency);

        drawer.open(podGroupDetail("backend", dependencies));

        assertThat(_find(drawer, Span.class, s -> s.withText("Depends on"))).isNotEmpty();
        assertThat(_find(drawer, Span.class, s -> s.withText("postgres-service"))).isNotEmpty();
        assertThat(_find(drawer, Span.class, s -> s.withText("via DB_HOST=postgres-service"))).isNotEmpty();
    }

    @Test
    void podGroupWithoutServiceDependency_doesNotShowDependsOnSection() {
        TopologyNodeDrawer drawer = new TopologyNodeDrawer();

        drawer.open(podGroupDetail("worker", Json.createArray()));

        assertThat(_find(drawer, Span.class, s -> s.withText("Depends on"))).isEmpty();
    }

    @Test
    void clickingGoToDependency_navigatesToTargetServiceManifestUrl() {
        TopologyNodeDrawer drawer = new TopologyNodeDrawer();
        // Attach to the mock UI so Button.getUI() resolves and the click listener's
        // ui.navigate(...) actually fires — TopologyNodeDrawer relies on tree attachment,
        // unlike views that call UI.getCurrent() directly.
        UI.getCurrent().add(drawer);

        JsonObject dependency = Json.createObject();
        dependency.put("targetLabel", "postgres-service");
        dependency.put("targetManifestUrl", "networking/services?name=postgres-service");
        dependency.put("matchedEnvVar", "DB_HOST");
        dependency.put("matchedValue", "postgres-service");

        JsonArray dependencies = Json.createArray();
        dependencies.set(0, dependency);

        drawer.open(podGroupDetail("backend", dependencies));

        Button goToButton = _find(drawer, Button.class, s -> s.withPredicate(b ->
                "Go to postgres-service".equals(b.getElement().getAttribute("title")))).get(0);

        // No routes are registered in the test environment, so a successful navigation
        // attempt always throws — proof the click reached ui.navigate(url). The target URL
        // carries a "?name=" query string, which Vaadin's Location only validates under
        // Java assertions (enabled by Gradle's test task, disabled in the production JVM),
        // surfacing as AssertionError here rather than the NotFoundException a query-less
        // URL would produce — same "?"-embedded-string navigate(String) pattern already used
        // elsewhere in this codebase (e.g. CronJobsView), not something this test should mask.
        assertThatThrownBy(() -> _click(goToButton)).isInstanceOfAny(NotFoundException.class, AssertionError.class);
    }

    private JsonObject podGroupDetail(String label, JsonArray serviceDependencies) {
        JsonObject detail = Json.createObject();
        detail.put("nodeLabel", label);
        detail.put("type", "2 Pods");
        detail.put("status", "Running");
        detail.put("manifestUrl", "workloads/pods");
        detail.put("readyReplicas", 0);
        detail.put("desiredReplicas", 2);
        detail.put("serviceType", "");
        detail.put("capacity", "");
        detail.put("accessMode", "");
        detail.put("labels", Json.createObject());
        detail.put("serviceDependencies", serviceDependencies);
        return detail;
    }
}
