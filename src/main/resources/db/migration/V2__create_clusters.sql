CREATE TABLE clusters (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    provider            VARCHAR(20)  NOT NULL CHECK (provider IN ('OKD', 'OPENSHIFT', 'KUBERNETES', 'RANCHER')),
    api_url             VARCHAR(500),
    kubeconfig_content  TEXT,
    connection_status   VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN'
                            CHECK (connection_status IN ('CONNECTED', 'DISCONNECTED', 'UNKNOWN', 'ERROR')),
    created_by          BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
