package io.greencap.k8s.domain.cluster;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClusterRepository extends JpaRepository<Cluster, Long> {

    List<Cluster> findAllByOrderByCreatedAtDesc();

    List<Cluster> findByConnectionStatus(ConnectionStatus status);
}
