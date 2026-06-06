package io.greencap.k8s.ui;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletResponse;

@Tag("div")
@PermitAll
public class AccessDeniedView extends Div implements HasErrorParameter<AccessDeniedException> {

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<AccessDeniedException> parameter) {
        event.forwardTo(DashboardView.class);
        return HttpServletResponse.SC_FORBIDDEN;
    }
}
