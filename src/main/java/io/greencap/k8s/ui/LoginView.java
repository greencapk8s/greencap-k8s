package io.greencap.k8s.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Route("login")
@PageTitle("Login — GreenCap K8s")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        getElement().setAttribute("theme", Lumo.DARK);
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        loginForm.setAction("login");

        Image logo = new Image("greencap.png", "GreenCap K8s");
        logo.setHeight("140px");

        H1 title = new H1("GreenCap K8s");
        title.addClassNames(LumoUtility.Margin.NONE);

        Paragraph subtitle = new Paragraph("Kubernetes Cluster Manager");
        subtitle.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.NONE);

        add(logo, title, subtitle, loginForm);
        setSpacing(false);
        setPadding(false);
        getStyle().set("gap", "var(--lumo-space-m)");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            loginForm.setError(true);
        }
    }
}
