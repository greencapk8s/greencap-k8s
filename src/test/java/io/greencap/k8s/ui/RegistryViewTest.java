package io.greencap.k8s.ui;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.greencap.k8s.KaribuTest;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.cluster.ConnectionStatus;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.RegistryMaintenanceService;
import io.greencap.k8s.kubernetes.RegistryService;
import io.greencap.k8s.kubernetes.dto.RepositoryInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistryViewTest extends KaribuTest {

    @Mock private RegistryService registryService;
    @Mock private RegistryMaintenanceService registryMaintenanceService;
    @Mock private ClusterContext clusterContext;
    @Mock private GridSelectionMemory gridSelectionMemory;

    private final Cluster cluster = buildCluster();
    private final RepositoryInfo repository      = new RepositoryInfo("mygroup/myapp",  2);
    private final RepositoryInfo otherRepository = new RepositoryInfo("other/service",  1);
    private RegistryView view;

    @BeforeEach
    void setupView() {
        lenient().when(clusterContext.getCluster()).thenReturn(cluster);
        lenient().when(registryService.listRepositories(cluster)).thenReturn(List.of(repository, otherRepository));
        lenient().when(gridSelectionMemory.recall(any())).thenReturn(Optional.empty());

        loginAs("GLOBAL_REGISTRY_VIEW", "GLOBAL_REGISTRY_DELETE");
        view = new RegistryView(registryService, registryMaintenanceService, clusterContext, gridSelectionMemory);
    }

    @Test
    void removeButton_isDisabled_whenNoItemSelected() {
        // No refresh — no items loaded, no auto-selection, button starts disabled
        assertThat(findRemoveButton().isEnabled()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void clickingViewTagsButton_selectsCorrespondingRow() {
        clickRefresh();
        Grid<RepositoryInfo> grid = _get(view, Grid.class);
        grid.select(repository);

        // Component columns aren't in the _find tree — invoke the renderer directly for the target item
        List<Grid.Column<RepositoryInfo>> columns = grid.getColumns();
        Grid.Column<RepositoryInfo> actionsColumn = columns.get(columns.size() - 1);
        ComponentRenderer<HorizontalLayout, RepositoryInfo> renderer =
                (ComponentRenderer<HorizontalLayout, RepositoryInfo>) actionsColumn.getRenderer();
        HorizontalLayout actionsCell = renderer.createComponent(otherRepository);
        // navigate() throws NotFoundException in the test environment (route not registered),
        // but grid.select() runs before it — absorb the navigation error and assert selection.
        try {
            _click((Button) actionsCell.getComponentAt(0));
        } catch (com.vaadin.flow.router.NotFoundException ignored) {}

        assertThat(grid.asSingleSelect().getValue()).isEqualTo(otherRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void confirmDelete_callsDeleteRepository_withSelectedRepository() {
        clickRefresh();
        _get(view, Grid.class).select(repository);
        clickRemoveButton();
        confirmDialog();

        verify(registryMaintenanceService).deleteRepository(cluster, "mygroup/myapp");
    }

    @Test
    @SuppressWarnings("unchecked")
    void confirmDelete_removesRepositoryFromGrid() {
        clickRefresh();
        Grid<RepositoryInfo> grid = _get(view, Grid.class);
        grid.select(repository);
        clickRemoveButton();
        confirmDialog();

        List<RepositoryInfo> visibleItems = grid.getListDataView().getItems().toList();
        assertThat(visibleItems).doesNotContain(repository);
    }

    private void clickRefresh() {
        _click(_get(view, Button.class, s -> s.withPredicate(b ->
                "Refresh".equals(b.getElement().getAttribute("title")))));
    }

    private void clickRemoveButton() {
        _click(findRemoveButton());
    }

    private Button findRemoveButton() {
        return _get(view, Button.class, s -> s.withPredicate(b ->
                "Remove Repository".equals(b.getElement().getAttribute("title"))));
    }

    private void confirmDialog() {
        // ConfirmDialog renders its confirm button as a web component slot — not accessible via _get(Button).
        // Fire the confirm event directly to trigger the addConfirmListener handler.
        ConfirmDialog dialog = _get(ConfirmDialog.class);
        ComponentUtil.fireEvent(dialog, new ConfirmDialog.ConfirmEvent(dialog, false));
    }

    private static Cluster buildCluster() {
        Cluster c = new Cluster();
        c.setName("test-cluster");
        c.setConnectionStatus(ConnectionStatus.UNKNOWN);
        return c;
    }
}
