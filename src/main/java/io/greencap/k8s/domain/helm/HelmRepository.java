package io.greencap.k8s.domain.helm;

import io.greencap.k8s.domain.cluster.Cluster;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "helm_repositories",
       uniqueConstraints = @UniqueConstraint(columnNames = {"cluster_id", "name"}))
@Getter
@Setter
@EqualsAndHashCode(of = "id")
public class HelmRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cluster_id", nullable = false)
    private Cluster cluster;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String url;
}
