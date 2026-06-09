package io.greencap.k8s.domain.user;

import io.greencap.k8s.domain.cluster.Cluster;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "topology_layouts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "cluster_id", "namespace"}))
@Getter
@Setter
public class TopologyLayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cluster_id", nullable = false)
    private Cluster cluster;

    @Column(nullable = false, length = 253)
    private String namespace;

    @Column(columnDefinition = "TEXT")
    private String nodePositions;

    @Column(nullable = false)
    private boolean groupingEnabled = true;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
