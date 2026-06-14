-- Grants GLOBAL_REGISTRY_VIEW to all users who already have
-- GLOBAL_INFRASTRUCTURE_VIEW (all profiles: ADMIN, OPERATOR, and VIEWER).
INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'GLOBAL_REGISTRY_VIEW'
FROM user_permissions
WHERE permissions = 'GLOBAL_INFRASTRUCTURE_VIEW'
ON CONFLICT DO NOTHING;
