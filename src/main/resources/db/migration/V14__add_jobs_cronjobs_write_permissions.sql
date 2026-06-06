-- Grants WORKLOADS_CRONJOBS_RUN_NOW and WORKLOADS_CRONJOBS_SUSPEND to users who already have
-- WORKLOADS_CRONJOBS_VIEW (ADMIN and OPERATOR profiles).
INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'WORKLOADS_CRONJOBS_RUN_NOW'
FROM user_permissions
WHERE permissions = 'WORKLOADS_CRONJOBS_VIEW'
ON CONFLICT DO NOTHING;

INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'WORKLOADS_CRONJOBS_SUSPEND'
FROM user_permissions
WHERE permissions = 'WORKLOADS_CRONJOBS_VIEW'
ON CONFLICT DO NOTHING;

-- Grants WORKLOADS_JOBS_DELETE and WORKLOADS_CRONJOBS_DELETE only to ADMIN users,
-- identified by possession of SETTINGS_USERS_WRITE.
INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'WORKLOADS_JOBS_DELETE'
FROM user_permissions
WHERE permissions = 'SETTINGS_USERS_WRITE'
ON CONFLICT DO NOTHING;

INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'WORKLOADS_CRONJOBS_DELETE'
FROM user_permissions
WHERE permissions = 'SETTINGS_USERS_WRITE'
ON CONFLICT DO NOTHING;
