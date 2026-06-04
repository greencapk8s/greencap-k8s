package io.greencap.k8s.kubernetes;

import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import io.greencap.k8s.domain.cluster.Cluster;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@VaadinSessionScope
@Getter
@Setter
public class ClusterContext {

    private Cluster cluster;
    private String namespace = "default";
}
