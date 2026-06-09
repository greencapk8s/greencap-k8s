package io.greencap.k8s.domain.user;

import io.greencap.k8s.domain.cluster.ClusterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TopologyLayoutService {

    private final TopologyLayoutRepository topologyLayoutRepository;
    private final UserRepository userRepository;
    private final ClusterRepository clusterRepository;

    public Optional<TopologyLayout> findLayout(Long userId, Long clusterId, String namespace) {
        return topologyLayoutRepository.findByUserIdAndClusterIdAndNamespace(userId, clusterId, namespace);
    }

    @Transactional
    public void upsertLayout(Long userId, Long clusterId, String namespace, String nodePositions, boolean groupingEnabled) {
        TopologyLayout layout = topologyLayoutRepository
                .findByUserIdAndClusterIdAndNamespace(userId, clusterId, namespace)
                .orElseGet(() -> {
                    TopologyLayout newLayout = new TopologyLayout();
                    newLayout.setUser(userRepository.getReferenceById(userId));
                    newLayout.setCluster(clusterRepository.getReferenceById(clusterId));
                    newLayout.setNamespace(namespace);
                    return newLayout;
                });

        layout.setNodePositions(nodePositions);
        layout.setGroupingEnabled(groupingEnabled);
        topologyLayoutRepository.save(layout);
    }
}
