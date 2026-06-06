INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'NETWORKING_INGRESS_VIEW'
FROM user_permissions
WHERE permissions = 'NETWORKING_SERVICES_VIEW'
ON CONFLICT DO NOTHING;
