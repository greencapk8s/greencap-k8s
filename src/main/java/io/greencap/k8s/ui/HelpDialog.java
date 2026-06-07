package io.greencap.k8s.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

class HelpDialog {

    private HelpDialog() {}

    static void open(String title, String text) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(title);
        dialog.setWidth("520px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        for (String paragraph : text.split("\n\n")) {
            content.add(new Paragraph(paragraph));
        }

        Button closeBtn = new Button("Close", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.add(content);
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }
}
