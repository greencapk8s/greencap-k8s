-- Grants PROJECT_DEPLOY_APPLICATION to ADMIN and OPERATOR users
-- (identified by GLOBAL_CLUSTERS_WRITE, same pattern as V23/V24).
INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'PROJECT_DEPLOY_APPLICATION'
FROM user_permissions
WHERE permissions = 'GLOBAL_CLUSTERS_WRITE'
ON CONFLICT DO NOTHING;
