package io.greencap.k8s.domain.helm;

import io.greencap.k8s.domain.cluster.Cluster;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HelmRepositoryService {

    private final HelmRepositoryRepository repository;

    public List<HelmRepository> listRepositories(Cluster cluster) {
        return repository.findByCluster(cluster);
    }

    @Transactional
    public HelmRepository addRepository(Cluster cluster, String name, String url) {
        if (repository.existsByClusterAndName(cluster, name)) {
            throw new IllegalArgumentException("Repository \"" + name + "\" already exists for this cluster");
        }
        HelmRepository repo = new HelmRepository();
        repo.setCluster(cluster);
        repo.setName(name);
        repo.setUrl(url);
        return repository.save(repo);
    }

    @Transactional
    public void removeRepository(Cluster cluster, String name) {
        repository.deleteByClusterAndName(cluster, name);
    }
}
