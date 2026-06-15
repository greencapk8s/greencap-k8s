package io.greencap.k8s.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSingleSelectionModel;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

final class UiConstants {

    static final int NOTIFICATION_DURATION_MS = 6000;
    static final String ICON_SIZE = "28px";
    static final Executor VIRTUAL_THREADS = Executors.newVirtualThreadPerTaskExecutor();

    private static final int ACTION_BUTTON_WIDTH_PX = 48;
    private static final int ACTIONS_COLUMN_RIGHT_PADDING_PX = 8;

    private static final String SELECTION_VIEW_KEY = "selectionViewKey";

    static VerticalLayout buildClusterUnreachableMessage() {
        Span text = new Span("Could not connect to the cluster. The cluster may be offline or the kubeconfig may be outdated.");
        text.addClassNames(LumoUtility.TextColor.ERROR, LumoUtility.FontSize.MEDIUM);

        Button goToClusters = new Button("Check Cluster Settings", VaadinIcon.COG.create(),
                e -> UI.getCurrent().navigate(ClustersView.class));
        goToClusters.addThemeVariants(ButtonVariant.LUMO_ERROR);

        VerticalLayout layout = new VerticalLayout(text, goToClusters);
        layout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        layout.setJustifyContentMode(JustifyContentMode.CENTER);
        layout.setSizeFull();
        layout.setVisible(false);
        return layout;
    }

    static VerticalLayout buildNoClusterMessage() {
        Span text = new Span("No active cluster. Select a cluster in Settings → Clusters.");
        text.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.MEDIUM);

        Button goToClusters = new Button("Go to Clusters", VaadinIcon.SERVER.create(),
                e -> UI.getCurrent().navigate(ClustersView.class));
        goToClusters.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout layout = new VerticalLayout(text, goToClusters);
        layout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        layout.setJustifyContentMode(JustifyContentMode.CENTER);
        layout.setSizeFull();
        layout.setVisible(false);
        return layout;
    }

    static HorizontalLayout buildSectionHeader(String title, BooleanSupplier onRefresh,
                                                String helpTitle, String helpText) {
        return buildSectionHeader(title, onRefresh, helpTitle, helpText, List.of());
    }

    static <T> HorizontalLayout buildSectionHeader(String title, BooleanSupplier onRefresh,
                                                    String helpTitle, String helpText,
                                                    Grid<T> grid, List<SelectionAction<T>> selectionActions) {
        return buildSectionHeader(title, onRefresh, helpTitle, helpText, buildSelectionButtons(grid, selectionActions));
    }

    static HorizontalLayout buildSectionHeader(String title, BooleanSupplier onRefresh,
                                                String helpTitle, String helpText, List<Button> leadingButtons) {
        H3 heading = new H3(title);

        var refreshIcon = VaadinIcon.REFRESH.create();
        refreshIcon.setSize(ICON_SIZE);
        Button refreshBtn = new Button(refreshIcon, e -> {
            boolean success = onRefresh.getAsBoolean();
            if (success) {
                Notification notification = Notification.show(
                        "Data updated", NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
        });
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        refreshBtn.getElement().setAttribute("title", "Refresh");

        var helpIcon = VaadinIcon.QUESTION_CIRCLE.create();
        helpIcon.setSize(ICON_SIZE);
        Button helpBtn = new Button(helpIcon, e -> HelpDialog.open(helpTitle, helpText));
        helpBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        helpBtn.getElement().setAttribute("title", "Help");

        HorizontalLayout header = new HorizontalLayout(heading);
        header.add(leadingButtons.toArray(Component[]::new));
        header.add(refreshBtn, helpBtn);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.setWidthFull();
        header.expand(heading);
        return header;
    }

    private static <T> List<Button> buildSelectionButtons(Grid<T> grid, List<SelectionAction<T>> selectionActions) {
        List<Button> buttons = new ArrayList<>();
        for (SelectionAction<T> action : selectionActions) {
            var icon = action.icon().create();
            icon.setSize(ICON_SIZE);
            Button btn = new Button(icon, e -> grid.asSingleSelect().getOptionalValue().ifPresent(action.handler()));
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            if (action.destructive()) {
                btn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            }
            btn.getElement().setAttribute("title", action.title());
            btn.setEnabled(action.enabled() && grid.asSingleSelect().getValue() != null);
            buttons.add(btn);
        }

        grid.asSingleSelect().addValueChangeListener(event -> {
            boolean hasSelection = event.getValue() != null;
            for (int i = 0; i < buttons.size(); i++) {
                buttons.get(i).setEnabled(selectionActions.get(i).enabled() && hasSelection);
            }
        });

        return buttons;
    }

    static <T> void configureSingleSelection(Grid<T> grid) {
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        ((GridSingleSelectionModel<T>) grid.getSelectionModel()).setDeselectAllowed(false);
    }

    static <T> void configureSingleSelection(Grid<T> grid, GridSelectionMemory selectionMemory,
                                              String viewKey, Function<T, String> nameExtractor) {
        configureSingleSelection(grid);
        ComponentUtil.setData(grid, GridSelectionMemory.class, selectionMemory);
        ComponentUtil.setData(grid, SELECTION_VIEW_KEY, viewKey);
        grid.asSingleSelect().addValueChangeListener(event -> {
            T selected = event.getValue();
            if (selected != null) {
                selectionMemory.remember(viewKey, nameExtractor.apply(selected));
            }
        });
    }

    static <T> void selectFirstOrPreserve(Grid<T> grid, ListDataProvider<T> dataProvider, Function<T, String> nameExtractor) {
        T current = grid.asSingleSelect().getValue();
        String currentName = current != null ? nameExtractor.apply(current) : recallSelectedName(grid);

        List<T> visibleItems = dataProvider.fetch(new Query<>()).collect(Collectors.toList());

        T toSelect = currentName != null
                ? visibleItems.stream().filter(item -> nameExtractor.apply(item).equals(currentName)).findFirst().orElse(null)
                : null;
        if (toSelect == null && !visibleItems.isEmpty()) {
            toSelect = visibleItems.get(0);
        }

        if (toSelect != null) {
            grid.select(toSelect);
        } else {
            grid.deselectAll();
        }
    }

    private static <T> String recallSelectedName(Grid<T> grid) {
        GridSelectionMemory selectionMemory = ComponentUtil.getData(grid, GridSelectionMemory.class);
        String viewKey = (String) ComponentUtil.getData(grid, SELECTION_VIEW_KEY);
        if (selectionMemory == null || viewKey == null) {
            return null;
        }
        return selectionMemory.recall(viewKey).orElse(null);
    }

    static Span replicasBadge(int ready, int desired) {
        Span badge = new Span(ready + "/" + desired);
        badge.getElement().getThemeList().add("badge");
        if (desired > 0 && ready >= desired) {
            badge.getElement().getThemeList().add("success");
        } else if (ready == 0) {
            badge.getElement().getThemeList().add("error");
        } else {
            badge.getElement().getThemeList().add("contrast");
        }
        return badge;
    }

    static String actionsColumnWidth(int buttonCount) {
        return (buttonCount * ACTION_BUTTON_WIDTH_PX + ACTIONS_COLUMN_RIGHT_PADDING_PX) + "px";
    }

    static <T> void addActionsColumn(Grid<T> grid, int buttonCount, Function<T, ? extends List<? extends Component>> buttonsProvider) {
        grid.addComponentColumn(item -> buildActionsLayout(buttonsProvider.apply(item)))
                .setHeader("").setWidth(actionsColumnWidth(buttonCount)).setFlexGrow(0);
    }

    private static HorizontalLayout buildActionsLayout(List<? extends Component> buttons) {
        HorizontalLayout actions = new HorizontalLayout(buttons.toArray(Component[]::new));
        actions.setSpacing(false);
        actions.setPadding(false);
        actions.getStyle().set("padding-right", ACTIONS_COLUMN_RIGHT_PADDING_PX + "px");
        return actions;
    }

    record SelectionAction<T>(VaadinIcon icon, String title, boolean enabled, boolean destructive, Consumer<T> handler) {
        static <T> SelectionAction<T> of(VaadinIcon icon, String title, Consumer<T> handler) {
            return new SelectionAction<>(icon, title, true, false, handler);
        }

        static <T> SelectionAction<T> of(VaadinIcon icon, String title, boolean enabled, Consumer<T> handler) {
            return new SelectionAction<>(icon, title, enabled, false, handler);
        }

        static <T> SelectionAction<T> destructive(VaadinIcon icon, String title, boolean enabled, Consumer<T> handler) {
            return new SelectionAction<>(icon, title, enabled, true, handler);
        }
    }

    private UiConstants() {}
}
