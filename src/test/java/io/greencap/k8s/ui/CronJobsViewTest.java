package io.greencap.k8s.ui;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.RouteConfiguration;
import io.greencap.k8s.KaribuTest;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.WorkloadService;
import io.greencap.k8s.kubernetes.dto.CronJobInfo;
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
class CronJobsViewTest extends KaribuTest {

    /**
     * Stands in for JobsView so the router has a real target and the resulting Location can be read.
     * Registered dynamically rather than via @Route: MainLayoutTest auto-discovers annotated routes
     * across the whole classpath, and an annotation here would collide with the real JobsView.
     */
    public static class JobsRouteStub extends Div {}

    private static final CronJobInfo NIGHTLY =
            new CronJobInfo("nightly-backup", "demo", "0 2 * * *", false, 0, "2h ago", "10d");

    @Mock private WorkloadService workloadService;
    @Mock private ClusterContext clusterContext;
    @Mock private GridSelectionMemory gridSelectionMemory;

    private CronJobsView view;

    @BeforeEach
    void setupView() {
        RouteConfiguration.forApplicationScope().setRoute("workloads/jobs", JobsRouteStub.class);

        Cluster cluster = new Cluster();
        cluster.setName("test-cluster");

        when(clusterContext.getCluster()).thenReturn(cluster);
        when(workloadService.listCronJobs(any(), any())).thenReturn(List.of(NIGHTLY));
        when(gridSelectionMemory.recall(any())).thenReturn(Optional.empty());

        loginAs("WORKLOADS_CRONJOBS_VIEW");
        view = new CronJobsView(workloadService, clusterContext, gridSelectionMemory);
        enterView();
    }

    @Test
    void viewJobsAction_landsOnTheJobsRoute_withTheCronJobAsAQueryParameter() {
        _click(actionButton(NIGHTLY, "View Jobs"));

        assertThat(activeLocation().getPath()).isEqualTo("workloads/jobs");
        assertThat(activeLocation().getQueryParameters().getParameters())
                .containsEntry("cronjob", List.of("nightly-backup"));
    }

    @Test
    void triggeringAJob_landsOnTheJobsRoute_withTheCronJobAsAQueryParameter() {
        when(workloadService.triggerCronJob(any(), any(), any())).thenReturn("nightly-backup-28901234");

        _click(actionButton(NIGHTLY, "Trigger Job"));
        confirmDialog();

        assertThat(activeLocation().getPath()).isEqualTo("workloads/jobs");
        assertThat(activeLocation().getQueryParameters().getParameters())
                .containsEntry("cronjob", List.of("nightly-backup"));
    }

    private Location activeLocation() {
        return UI.getCurrent().getInternals().getActiveViewLocation();
    }

    private void confirmDialog() {
        ConfirmDialog dialog = _get(ConfirmDialog.class);
        ComponentUtil.fireEvent(dialog, new ConfirmDialog.ConfirmEvent(dialog, false));
    }

    /** The actions column renders components outside the _find tree — invoke its renderer directly. */
    @SuppressWarnings("unchecked")
    private Button actionButton(CronJobInfo cronJob, String title) {
        Grid<CronJobInfo> grid = _get(view, Grid.class);
        Grid.Column<CronJobInfo> actionsColumn = grid.getColumns().get(grid.getColumns().size() - 1);
        ComponentRenderer<HorizontalLayout, CronJobInfo> renderer =
                (ComponentRenderer<HorizontalLayout, CronJobInfo>) actionsColumn.getRenderer();
        return renderer.createComponent(cronJob).getChildren()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> title.equals(button.getElement().getAttribute("title")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No action button titled " + title));
    }

    private void enterView() {
        view.beforeEnter(new BeforeEnterEvent(
                UI.getCurrent().getInternals().getRouter(),
                NavigationTrigger.UI_NAVIGATE,
                new Location("workloads/cronjobs"),
                CronJobsView.class,
                UI.getCurrent(),
                List.of()));
    }
}
