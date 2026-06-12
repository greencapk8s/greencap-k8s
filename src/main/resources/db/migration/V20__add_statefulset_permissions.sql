-- Grants WORKLOADS_STATEFULSETS_VIEW to all users who already have
-- WORKLOADS_DEPLOYMENTS_VIEW (all profiles: ADMIN, OPERATOR, and VIEWER).
INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'WORKLOADS_STATEFULSETS_VIEW'
FROM user_permissions
WHERE permissions = 'WORKLOADS_DEPLOYMENTS_VIEW'
ON CONFLICT DO NOTHING;

-- Grants the StatefulSet write permissions to ADMIN and OPERATOR users
-- (identified by SETTINGS_CLUSTERS_WRITE, same pattern as V17/V18).
INSERT INTO user_permissions (user_id, permissions)
SELECT DISTINCT user_id, p.permission_name
FROM user_permissions up
CROSS JOIN (VALUES
    ('WORKLOADS_STATEFULSETS_SCALE'),
    ('WORKLOADS_STATEFULSETS_RESTART'),
    ('WORKLOADS_STATEFULSETS_ROLLBACK'),
    ('WORKLOADS_STATEFULSETS_DELETE')
) AS p(permission_name)
WHERE up.permissions = 'SETTINGS_CLUSTERS_WRITE'
ON CONFLICT DO NOTHING;
