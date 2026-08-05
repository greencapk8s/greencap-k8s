package io.greencap.k8s.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.greencap.k8s.KaribuTest;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.ObservabilityService;
import io.greencap.k8s.kubernetes.WorkloadService;
import io.greencap.k8s.kubernetes.dto.PodInfo;
import io.greencap.k8s.kubernetes.dto.PodState;
import io.greencap.k8s.kubernetes.dto.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._setValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PodsViewTest extends KaribuTest {

    @Mock private WorkloadService workloadService;
    @Mock private ObservabilityService observabilityService;
    @Mock private ClusterContext clusterContext;
    @Mock private GridSelectionMemory gridSelectionMemory;

    /** The defect this sprint fixes: a Pod that never started, still reporting phase Running. */
    private static final PodInfo BROKEN = pod("backend", "Running",
            new PodState("ImagePullBackOff", Severity.PROBLEM, "Back-off pulling image \"registry/app:latest\""), "");
    private static final PodInfo HEALTHY = pod("web", "Running",
            new PodState("Running", Severity.HEALTHY, ""), "");
    private static final PodInfo STARTING = pod("cache", "Pending",
            new PodState("ContainerCreating", Severity.NEUTRAL, ""), "");
    private static final PodInfo UNRECOGNIZED = pod("worker", "Pending",
            new PodState("SomeReasonKubernetesAddedLater", Severity.PROBLEM, ""), "");
    private static final PodInfo COMPLETED_JOB = pod("backup-1", "Succeeded",
            new PodState("Succeeded", Severity.HEALTHY, ""), "backup");

    private PodsView view;

    @BeforeEach
    void setupView() {
        Cluster cluster = new Cluster();
        cluster.setName("test-cluster");

        when(clusterContext.getCluster()).thenReturn(cluster);
        when(workloadService.listPods(any(), any()))
                .thenReturn(List.of(BROKEN, HEALTHY, STARTING, UNRECOGNIZED, COMPLETED_JOB));
        when(gridSelectionMemory.recall(any())).thenReturn(Optional.empty());

        loginAs("WORKLOADS_PODS_VIEW");
        view = new PodsView(workloadService, observabilityService, clusterContext, gridSelectionMemory);
        clickRefresh();
    }

    @Test
    void statusBadge_showsThePodStateLabel_notThePhase() {
        assertThat(badgeFor(BROKEN).getText()).isEqualTo("ImagePullBackOff");
    }

    @Test
    void statusBadge_colorsBySeverity() {
        assertThat(themeOf(badgeFor(BROKEN))).contains("error");
        assertThat(themeOf(badgeFor(HEALTHY))).contains("success");
        assertThat(themeOf(badgeFor(STARTING))).contains("contrast");
    }

    @Test
    void statusBadge_treatsAnUnrecognizedReasonAsAProblem() {
        assertThat(themeOf(badgeFor(UNRECOGNIZED))).contains("error");
    }

    @Test
    void statusBadge_carriesTheReasonMessageAsTooltip() {
        assertThat(badgeFor(BROKEN).getElement().getAttribute("title"))
                .isEqualTo("Back-off pulling image \"registry/app:latest\"");
    }

    @Test
    void statusBadge_hasNoTooltip_whenThereIsNoMessage() {
        assertThat(badgeFor(HEALTHY).getElement().getAttribute("title")).isNull();
    }

    @Test
    void statusFilter_matchesTheDisplayedLabel() {
        _setValue(statusFilter(), "ImagePull");

        assertThat(visiblePodNames()).containsExactly("backend");
    }

    @Test
    void statusFilter_noLongerMatchesThePhaseBehindTheLabel() {
        _setValue(statusFilter(), "Running");

        assertThat(visiblePodNames()).doesNotContain("backend");
    }

    @Test
    void completedJobPods_stayHiddenByDefault() {
        assertThat(visiblePodNames()).doesNotContain("backup-1");
    }

    @Test
    void completedJobPods_appearWhenTheToggleIsCleared() {
        _setValue(_get(view, Checkbox.class), false);

        assertThat(visiblePodNames()).contains("backup-1");
    }

    private static PodInfo pod(String name, String phase, PodState state, String jobName) {
        return new PodInfo(name, "demo", phase, state, "node-1", 0, "5m", jobName);
    }

    /** Component columns aren't in the _find tree — invoke the renderer directly for the target item. */
    @SuppressWarnings("unchecked")
    private Span badgeFor(PodInfo pod) {
        Grid<PodInfo> grid = _get(view, Grid.class);
        Grid.Column<PodInfo> statusColumn = grid.getColumns().get(1);
        ComponentRenderer<Span, PodInfo> renderer = (ComponentRenderer<Span, PodInfo>) statusColumn.getRenderer();
        return renderer.createComponent(pod);
    }

    private String themeOf(Span badge) {
        return badge.getElement().getAttribute("theme");
    }

    /** Header-row filters share a placeholder; they follow column order — name, status, node. */
    private TextField statusFilter() {
        return _find(view, TextField.class).get(1);
    }

    @SuppressWarnings("unchecked")
    private List<String> visiblePodNames() {
        Grid<PodInfo> grid = _get(view, Grid.class);
        return grid.getListDataView().getItems().map(PodInfo::name).toList();
    }

    private void clickRefresh() {
        _click(_get(view, Button.class, s -> s.withPredicate(b ->
                "Refresh".equals(b.getElement().getAttribute("title")))));
    }
}
