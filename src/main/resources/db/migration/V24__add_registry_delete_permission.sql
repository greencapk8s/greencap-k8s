-- Grants GLOBAL_REGISTRY_DELETE to ADMIN and OPERATOR users
-- (identified by GLOBAL_CLUSTERS_WRITE, same pattern as V23).
INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'GLOBAL_REGISTRY_DELETE'
FROM user_permissions
WHERE permissions = 'GLOBAL_CLUSTERS_WRITE'
ON CONFLICT DO NOTHING;
