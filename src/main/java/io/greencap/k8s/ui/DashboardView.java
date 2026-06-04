package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.cluster.ClusterService;
import io.greencap.k8s.domain.cluster.ConnectionStatus;
import jakarta.annotation.security.PermitAll;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard — GreenCap K8s")
@PermitAll
public class DashboardView extends VerticalLayout {

    private final ClusterService clusterService;

    public DashboardView(ClusterService clusterService) {
        this.clusterService = clusterService;
        setPadding(true);
        setSpacing(true);
        add(new H2("Dashboard"), buildSummaryCards());
    }

    private HorizontalLayout buildSummaryCards() {
        List<Cluster> clusters = clusterService.findAll();
        Map<ConnectionStatus, Long> countByStatus = clusters.stream()
                .collect(Collectors.groupingBy(
                        Cluster::getConnectionStatus,
                        Collectors.counting()
                ));

        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(true);
        row.setDefaultVerticalComponentAlignment(Alignment.STRETCH);

        row.add(buildCard("Total",        clusters.size(), null));
        row.add(buildCard("Connected",    countByStatus.getOrDefault(ConnectionStatus.CONNECTED,    0L).intValue(), "success"));
        row.add(buildCard("Disconnected", countByStatus.getOrDefault(ConnectionStatus.DISCONNECTED, 0L).intValue(), "contrast"));
        row.add(buildCard("Error",        countByStatus.getOrDefault(ConnectionStatus.ERROR,         0L).intValue(), "error"));
        row.add(buildCard("Unknown",      countByStatus.getOrDefault(ConnectionStatus.UNKNOWN,       0L).intValue(), null));

        return row;
    }

    private Div buildCard(String label, int count, String badgeVariant) {
        Span countText = new Span(String.valueOf(count));
        countText.getStyle()
                .set("font-size", "2.5rem")
                .set("font-weight", "bold")
                .set("line-height", "1");

        Span badge = new Span(label);
        badge.getElement().getThemeList().add("badge");
        if (badgeVariant != null) {
            badge.getElement().getThemeList().add(badgeVariant);
        }

        VerticalLayout content = new VerticalLayout(countText, badge);
        content.setPadding(false);
        content.setSpacing(true);
        content.setDefaultHorizontalComponentAlignment(Alignment.START);

        Div card = new Div(content);
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-m)")
                .set("min-width", "160px")
                .set("cursor", "pointer")
                .set("box-shadow", "var(--lumo-box-shadow-xs)");

        card.addClickListener(e -> UI.getCurrent().navigate(ClustersView.class));

        return card;
    }
}
