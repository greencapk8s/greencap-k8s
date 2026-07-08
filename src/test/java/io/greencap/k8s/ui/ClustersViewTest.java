package io.greencap.k8s.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import io.greencap.k8s.KaribuTest;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.cluster.ClusterService;
import io.greencap.k8s.domain.cluster.ConnectionStatus;
import io.greencap.k8s.domain.user.UserService;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubeconfigValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClustersViewTest extends KaribuTest {

    @Mock private ClusterService clusterService;
    @Mock private KubeconfigValidator kubeconfigValidator;
    @Mock private ClusterContext clusterContext;
    @Mock private UserService userService;
    @Mock private GridSelectionMemory gridSelectionMemory;

    private final Cluster cluster = buildCluster("test-cluster");
    private ClustersView view;

    @BeforeEach
    void setupView() {
        when(clusterService.findAll()).thenReturn(List.of(cluster));
        when(clusterContext.getCluster()).thenReturn(null);

        loginAs("GLOBAL_CLUSTERS_VIEW", "GLOBAL_CLUSTERS_WRITE");
        view = new ClustersView(clusterService, kubeconfigValidator, clusterContext, userService, gridSelectionMemory);
    }

    @Test
    void confirmDelete_callsDeleteService_withSelectedCluster() {
        selectCluster();
        clickToolbarRemove();
        clickDialogRemove();

        verify(clusterService).deleteCluster(cluster);
    }

    @SuppressWarnings("unchecked")
    private void selectCluster() {
        _get(view, Grid.class).select(cluster);
    }

    private void clickToolbarRemove() {
        _click(_get(view, Button.class, s -> s.withPredicate(b ->
                "Remove".equals(b.getElement().getAttribute("title")))));
    }

    private void clickDialogRemove() {
        _click(_get(Button.class, s -> s.withText("Remove")));
    }

    private static Cluster buildCluster(String name) {
        Cluster c = new Cluster();
        c.setName(name);
        c.setConnectionStatus(ConnectionStatus.UNKNOWN);
        return c;
    }
}
