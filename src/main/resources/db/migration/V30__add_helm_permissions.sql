-- Grants PROJECT_HELM_VIEW to all users with GLOBAL_CLUSTERS_VIEW (ADMIN, OPERATOR, VIEWER).
-- Grants PROJECT_HELM_UNINSTALL to users with GLOBAL_CLUSTERS_WRITE (ADMIN, OPERATOR).
INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'PROJECT_HELM_VIEW'
FROM user_permissions
WHERE permissions = 'GLOBAL_CLUSTERS_VIEW'
ON CONFLICT DO NOTHING;

INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'PROJECT_HELM_UNINSTALL'
FROM user_permissions
WHERE permissions = 'GLOBAL_CLUSTERS_WRITE'
ON CONFLICT DO NOTHING;
