package io.greencap.k8s.domain.helm;

import io.greencap.k8s.domain.cluster.Cluster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HelmRepositoryRepository extends JpaRepository<HelmRepository, Long> {

    List<HelmRepository> findByCluster(Cluster cluster);

    void deleteByClusterAndName(Cluster cluster, String name);

    boolean existsByClusterAndName(Cluster cluster, String name);
}
