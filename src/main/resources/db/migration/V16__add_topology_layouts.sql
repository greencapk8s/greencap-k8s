CREATE TABLE topology_layouts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    cluster_id BIGINT NOT NULL REFERENCES clusters(id) ON DELETE CASCADE,
    namespace VARCHAR(253) NOT NULL,
    node_positions TEXT,
    grouping_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_topology_layout UNIQUE (user_id, cluster_id, namespace)
);
