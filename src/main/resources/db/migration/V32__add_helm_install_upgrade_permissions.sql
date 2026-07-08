-- Grants PROJECT_HELM_INSTALL and PROJECT_HELM_UPGRADE to users with GLOBAL_CLUSTERS_WRITE (ADMIN, OPERATOR).
INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'PROJECT_HELM_INSTALL'
FROM user_permissions
WHERE permissions = 'GLOBAL_CLUSTERS_WRITE'
ON CONFLICT DO NOTHING;

INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'PROJECT_HELM_UPGRADE'
FROM user_permissions
WHERE permissions = 'GLOBAL_CLUSTERS_WRITE'
ON CONFLICT DO NOTHING;
