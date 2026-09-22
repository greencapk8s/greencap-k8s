package io.greencap.k8s.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import io.greencap.k8s.domain.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * Server-side wrapper for the tour LitElement Web Component. Hands the browser a step list that
 * is already decided — which steps, in what order, with what text — and takes back nothing but
 * the notice that the Tour is over.
 */
@Slf4j
@Tag("greencap-tour")
@NpmPackage(value = "driver.js", version = "1.8.0")
@JsModule("./tour.ts")
public class TourComponent extends Component {

    private final ObjectMapper objectMapper;
    private final UserService userService;

    public TourComponent(ObjectMapper objectMapper, UserService userService) {
        this.objectMapper = objectMapper;
        this.userService = userService;
    }

    public void start(List<TourStep> steps) {
        try {
            getElement().setProperty("steps", objectMapper.writeValueAsString(steps));
            // Driving the Tour is a command, not a state change. Replaying it from Platform
            // Settings hands over the very same list, and a property that did not change is a
            // property Flow never sends — the replay would silently do nothing.
            getElement().callJsFunction("startTour");
        } catch (JsonProcessingException e) {
            // A Tour that cannot be serialized is not worth taking the session down for.
            log.error("Could not serialize the tour steps — skipping the tour", e);
        }
    }

    /**
     * Every way out of the Tour lands here — finishing the last step, skipping, ESC or the close
     * button all count as seen, so it does not come back tomorrow over a mistyped key.
     */
    @ClientCallable
    public void markTourAsSeen() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.updateTourSeen(username, true);
    }
}
