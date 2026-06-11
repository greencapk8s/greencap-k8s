-- Grants the new MANIFEST_EDIT permission to ADMIN and OPERATOR users.
-- Identified by possession of SETTINGS_CLUSTERS_WRITE, same signal used in V17/V18.

INSERT INTO user_permissions (user_id, permissions)
SELECT DISTINCT user_id, 'MANIFEST_EDIT'
FROM user_permissions up
WHERE up.permissions = 'SETTINGS_CLUSTERS_WRITE'
ON CONFLICT DO NOTHING;
