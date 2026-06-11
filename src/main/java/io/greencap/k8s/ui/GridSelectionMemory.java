package io.greencap.k8s.ui;

import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@VaadinSessionScope
class GridSelectionMemory {

    private final Map<String, String> selectedItemNames = new ConcurrentHashMap<>();

    void remember(String viewKey, String itemName) {
        selectedItemNames.put(viewKey, itemName);
    }

    Optional<String> recall(String viewKey) {
        return Optional.ofNullable(selectedItemNames.get(viewKey));
    }
}
