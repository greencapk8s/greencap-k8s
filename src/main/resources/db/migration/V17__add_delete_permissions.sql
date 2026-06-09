-- Grants all new _DELETE permissions to ADMIN and OPERATOR users.
-- ADMIN is identified by possession of SETTINGS_USERS_WRITE.
-- OPERATOR is identified by possession of WORKLOADS_JOBS_DELETE (granted in V14 only to admins at the time).
-- Since V14 granted WORKLOADS_JOBS_DELETE only to admins, we use SETTINGS_CLUSTERS_WRITE
-- as the operator signal (present in operator preset, absent from viewer).

INSERT INTO user_permissions (user_id, permissions)
SELECT DISTINCT user_id, p.permission_name
FROM user_permissions up
CROSS JOIN (VALUES
    ('WORKLOADS_DEPLOYMENTS_DELETE'),
    ('WORKLOADS_REPLICASETS_DELETE'),
    ('WORKLOADS_PODS_DELETE'),
    ('NETWORKING_SERVICES_DELETE'),
    ('NETWORKING_INGRESS_DELETE'),
    ('PARAMETERS_CONFIGMAPS_DELETE'),
    ('PARAMETERS_SECRETS_DELETE'),
    ('AUTOSCALING_HORIZONTALSCALER_DELETE'),
    ('STORAGE_PVC_DELETE')
) AS p(permission_name)
WHERE up.permissions = 'SETTINGS_CLUSTERS_WRITE'
ON CONFLICT DO NOTHING;
