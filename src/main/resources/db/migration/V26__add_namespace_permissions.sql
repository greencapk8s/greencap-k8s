-- Grants GLOBAL_NAMESPACES_VIEW to all users with GLOBAL_CLUSTERS_VIEW (ADMIN, OPERATOR, VIEWER).
-- Grants GLOBAL_NAMESPACES_WRITE and GLOBAL_NAMESPACES_DELETE to users with GLOBAL_CLUSTERS_WRITE (ADMIN, OPERATOR).
INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'GLOBAL_NAMESPACES_VIEW'
FROM user_permissions
WHERE permissions = 'GLOBAL_CLUSTERS_VIEW'
ON CONFLICT DO NOTHING;

INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'GLOBAL_NAMESPACES_WRITE'
FROM user_permissions
WHERE permissions = 'GLOBAL_CLUSTERS_WRITE'
ON CONFLICT DO NOTHING;

INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'GLOBAL_NAMESPACES_DELETE'
FROM user_permissions
WHERE permissions = 'GLOBAL_CLUSTERS_WRITE'
ON CONFLICT DO NOTHING;
