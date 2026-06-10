-- Grants the new SETTINGS_INFRASTRUCTURE_CORDON permission to ADMIN and OPERATOR users.
-- SETTINGS_CLUSTERS_WRITE is present in both presets and absent from viewer,
-- so it is used as the ADMIN/OPERATOR signal (same approach as V17).

INSERT INTO user_permissions (user_id, permissions)
SELECT DISTINCT user_id, 'SETTINGS_INFRASTRUCTURE_CORDON'
FROM user_permissions
WHERE permissions = 'SETTINGS_CLUSTERS_WRITE'
ON CONFLICT DO NOTHING;
