package io.greencap.k8s.config;

import io.greencap.k8s.domain.cluster.ClusterRepository;
import io.greencap.k8s.domain.cluster.ClusterService;
import io.greencap.k8s.domain.cluster.CreateClusterRequest;
import io.greencap.k8s.domain.user.Permission;
import io.greencap.k8s.domain.user.UserRepository;
import io.greencap.k8s.domain.user.UserService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final String SELF_CLUSTER_NAME = "greencap-platform";
    private static final String SELF_CLUSTER_KUBECONFIG_ENV = "GREENCAP_SELF_CLUSTER_KUBECONFIG";

    private final UserRepository userRepository;
    private final UserService userService;
    private final ClusterRepository clusterRepository;
    private final ClusterService clusterService;

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByUsername("admin")) {
            userService.createUser("admin", "admin@greencap.local", "admin", Permission.allPermissions());
            log.info("Admin user created — login: admin / admin");
        } else {
            userRepository.findByUsernameWithPermissions("admin").ifPresent(admin -> {
                Set<Permission> all = Permission.allPermissions();
                if (!admin.getPermissions().containsAll(all)) {
                    userService.updatePermissions(admin.getId(), all);
                    log.info("Admin permissions updated to include all current permissions");
                }
            });
        }

        String kubeconfig = System.getenv(SELF_CLUSTER_KUBECONFIG_ENV);
        if (kubeconfig != null && !kubeconfig.isBlank() && !clusterRepository.existsByName(SELF_CLUSTER_NAME)) {
            var cluster = clusterService.createCluster(new CreateClusterRequest(
                    SELF_CLUSTER_NAME,
                    kubeconfig
            ));
            userService.updateActiveCluster("admin", cluster);
            userService.updateActiveNamespace("admin", SELF_CLUSTER_NAME);
            log.info("Cluster '{}' auto-registered, set as active cluster and namespace for admin", SELF_CLUSTER_NAME);
        }
    }
}
