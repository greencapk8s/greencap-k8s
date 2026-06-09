package io.greencap.k8s.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TopologyLayoutRepository extends JpaRepository<TopologyLayout, Long> {

    Optional<TopologyLayout> findByUserIdAndClusterIdAndNamespace(Long userId, Long clusterId, String namespace);
}
