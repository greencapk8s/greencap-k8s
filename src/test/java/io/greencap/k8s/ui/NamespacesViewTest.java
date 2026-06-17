package io.greencap.k8s.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import io.greencap.k8s.KaribuTest;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.cluster.ClusterProvider;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.NamespaceService;
import io.greencap.k8s.kubernetes.dto.NamespaceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NamespacesViewTest extends KaribuTest {

    @Mock private NamespaceService namespaceService;
    @Mock private ClusterContext clusterContext;
    @Mock private GridSelectionMemory gridSelectionMemory;

    private static final NamespaceInfo KUBE_SYSTEM  = new NamespaceInfo("kube-system",  "Active", "100d", 10, 2, 5);
    private static final NamespaceInfo MY_NAMESPACE = new NamespaceInfo("my-namespace", "Active",   "5d",  2, 1, 1);

    private NamespacesView view;

    @BeforeEach
    void setupView() {
        Cluster cluster = new Cluster();
        cluster.setName("test-cluster");
        cluster.setProvider(ClusterProvider.MinikubeDocker);

        when(clusterContext.getCluster()).thenReturn(cluster);
        when(namespaceService.listNamespacesWithCounts(any())).thenReturn(List.of(KUBE_SYSTEM, MY_NAMESPACE));
        when(gridSelectionMemory.recall(any())).thenReturn(Optional.empty());

        loginAs("GLOBAL_NAMESPACES_VIEW", "GLOBAL_NAMESPACES_DELETE", "GLOBAL_NAMESPACES_WRITE");
        view = new NamespacesView(namespaceService, clusterContext, gridSelectionMemory);
        clickRefresh();
    }

    @Test
    void deletingSystemNamespace_showsErrorNotification_noDialogOpens() {
        selectNamespace(KUBE_SYSTEM);
        clickToolbarDelete();

        assertThat(_find(Dialog.class)).isEmpty();
        assertThat(_find(Notification.class)).isNotEmpty();
    }

    @Test
    void deleteDialog_opensForRegularNamespace_withDeleteButtonInitiallyDisabled() {
        selectNamespace(MY_NAMESPACE);
        clickToolbarDelete();

        Dialog dialog = _get(Dialog.class);
        assertThat(dialogDeleteButton(dialog).isEnabled()).isFalse();
    }

    @Test
    void typingWrongName_keepsDeleteButtonDisabled() {
        selectNamespace(MY_NAMESPACE);
        clickToolbarDelete();

        Dialog dialog = _get(Dialog.class);
        _setValue(_get(dialog, TextField.class), "wrong-name");

        assertThat(dialogDeleteButton(dialog).isEnabled()).isFalse();
    }

    @Test
    void typingExactNamespaceName_enablesDeleteButton() {
        selectNamespace(MY_NAMESPACE);
        clickToolbarDelete();

        Dialog dialog = _get(Dialog.class);
        _setValue(_get(dialog, TextField.class), MY_NAMESPACE.name());

        assertThat(dialogDeleteButton(dialog).isEnabled()).isTrue();
    }

    @SuppressWarnings("unchecked")
    private void selectNamespace(NamespaceInfo namespace) {
        _get(view, Grid.class).select(namespace);
    }

    private void clickRefresh() {
        _click(_get(view, Button.class, s -> s.withPredicate(b ->
                "Refresh".equals(b.getElement().getAttribute("title")))));
    }

    private void clickToolbarDelete() {
        _click(_get(view, Button.class, s -> s.withPredicate(b ->
                "Delete".equals(b.getElement().getAttribute("title")))));
    }

    private Button dialogDeleteButton(Dialog dialog) {
        return _get(dialog, Button.class, s -> s.withText("Delete"));
    }
}
