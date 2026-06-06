-- Grants WORKLOADS_JOBS_VIEW and WORKLOADS_CRONJOBS_VIEW to all users who already have
-- WORKLOADS_PODS_VIEW (all profiles: ADMIN, OPERATOR, and VIEWER).
INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'WORKLOADS_JOBS_VIEW'
FROM user_permissions
WHERE permissions = 'WORKLOADS_PODS_VIEW'
ON CONFLICT DO NOTHING;

INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'WORKLOADS_CRONJOBS_VIEW'
FROM user_permissions
WHERE permissions = 'WORKLOADS_PODS_VIEW'
ON CONFLICT DO NOTHING;
