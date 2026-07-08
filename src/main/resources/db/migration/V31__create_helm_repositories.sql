CREATE TABLE helm_repositories (
    id         BIGSERIAL PRIMARY KEY,
    cluster_id BIGINT       NOT NULL REFERENCES clusters(id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    url        VARCHAR(500) NOT NULL,
    UNIQUE (cluster_id, name)
);
