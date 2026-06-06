-- Grants WORKLOADS_DEPLOYMENTS_ROLLBACK to all users who already have WORKLOADS_DEPLOYMENTS_RESTART
-- (i.e. ADMIN and OPERATOR; VIEWER does not get this permission).
INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'WORKLOADS_DEPLOYMENTS_ROLLBACK'
FROM user_permissions
WHERE permissions = 'WORKLOADS_DEPLOYMENTS_RESTART'
ON CONFLICT DO NOTHING;
