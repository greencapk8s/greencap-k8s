-- Grants GLOBAL_REGISTRY_BUILD to ADMIN and OPERATOR users
-- (identified by GLOBAL_CLUSTERS_WRITE, same pattern as V20).
INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'GLOBAL_REGISTRY_BUILD'
FROM user_permissions
WHERE permissions = 'GLOBAL_CLUSTERS_WRITE'
ON CONFLICT DO NOTHING;
