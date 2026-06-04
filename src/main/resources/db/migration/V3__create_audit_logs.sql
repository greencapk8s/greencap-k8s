CREATE TABLE audit_logs (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    action        VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50),
    resource_name VARCHAR(255),
    namespace     VARCHAR(255),
    cluster_id    BIGINT       REFERENCES clusters(id) ON DELETE SET NULL,
    payload       JSONB,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_user_id    ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_cluster_id ON audit_logs(cluster_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
