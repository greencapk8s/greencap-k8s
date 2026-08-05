package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouteConfiguration;
import io.greencap.k8s.KaribuTest;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.WorkloadService;
import io.greencap.k8s.kubernetes.dto.JobInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobsViewTest extends KaribuTest {

    /**
     * Stands in for PodsView so the router has a real target and the resulting Location can be read.
     * Registered dynamically rather than via @Route: MainLayoutTest auto-discovers annotated routes
     * across the whole classpath, and an annotation here would collide with the real PodsView.
     */
    public static class PodsRouteStub extends Div {}

    /** Owned by a CronJob — the link CronJobsView follows when it filters this listing. */
    private static final JobInfo SCHEDULED_RUN =
            new JobInfo("nightly-backup-28901234", "demo", "Complete", "1/1", "12s", "node-1", "2h", "nightly-backup");
    private static final JobInfo STANDALONE_RUN =
            new JobInfo("migrate-db", "demo", "Complete", "1/1", "45s", "node-2", "3d", "");

    @Mock private WorkloadService workloadService;
    @Mock private ClusterContext clusterContext;
    @Mock private GridSelectionMemory gridSelectionMemory;

    private JobsView view;

    @BeforeEach
    void setupView() {
        RouteConfiguration.forApplicationScope().setRoute("workloads/pods", PodsRouteStub.class);

        Cluster cluster = new Cluster();
        cluster.setName("test-cluster");

        when(clusterContext.getCluster()).thenReturn(cluster);
        when(workloadService.listJobs(any(), any())).thenReturn(List.of(SCHEDULED_RUN, STANDALONE_RUN));
        when(gridSelectionMemory.recall(any())).thenReturn(Optional.empty());

        loginAs("WORKLOADS_JOBS_VIEW");
        view = new JobsView(workloadService, clusterContext, gridSelectionMemory);
        enterWithQuery("");
    }

    @Test
    void viewPodsAction_landsOnThePodsRoute_withTheJobAsAQueryParameter() {
        _click(actionButton(SCHEDULED_RUN, "View Pods"));

        assertThat(activeLocation().getPath()).isEqualTo("workloads/pods");
        assertThat(activeLocation().getQueryParameters().getParameters())
                .containsEntry("job", List.of("nightly-backup-28901234"));
    }

    /** The receiving end of the CronJobsView link: the parameter has to reach the owner filter. */
    @Test
    void cronjobQueryParameter_narrowsTheListingToTheJobsThatCronJobOwns() {
        enterWithQuery("cronjob=nightly-backup");

        assertThat(visibleJobNames()).containsExactly("nightly-backup-28901234");
    }

    @Test
    void enteringWithoutACronJob_leavesTheListingUnfiltered() {
        assertThat(visibleJobNames()).containsExactly("nightly-backup-28901234", "migrate-db");
    }

    private Location activeLocation() {
        return UI.getCurrent().getInternals().getActiveViewLocation();
    }

    private void enterWithQuery(String queryString) {
        view.beforeEnter(new BeforeEnterEvent(
                UI.getCurrent().getInternals().getRouter(),
                NavigationTrigger.UI_NAVIGATE,
                new Location("workloads/jobs", QueryParameters.fromString(queryString)),
                JobsView.class,
                UI.getCurrent(),
                List.of()));
    }

    @SuppressWarnings("unchecked")
    private List<String> visibleJobNames() {
        Grid<JobInfo> grid = _get(view, Grid.class);
        return grid.getListDataView().getItems().map(JobInfo::name).toList();
    }

    /** The actions column renders components outside the _find tree — invoke its renderer directly. */
    @SuppressWarnings("unchecked")
    private Button actionButton(JobInfo job, String title) {
        Grid<JobInfo> grid = _get(view, Grid.class);
        Grid.Column<JobInfo> actionsColumn = grid.getColumns().get(grid.getColumns().size() - 1);
        ComponentRenderer<HorizontalLayout, JobInfo> renderer =
                (ComponentRenderer<HorizontalLayout, JobInfo>) actionsColumn.getRenderer();
        return renderer.createComponent(job).getChildren()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> title.equals(button.getElement().getAttribute("title")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No action button titled " + title));
    }
}
