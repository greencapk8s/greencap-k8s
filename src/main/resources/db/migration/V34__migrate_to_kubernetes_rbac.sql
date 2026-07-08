DROP TABLE IF EXISTS user_permissions;

ALTER TABLE users ADD COLUMN IF NOT EXISTS serviceaccount_name  VARCHAR(253);
ALTER TABLE users ADD COLUMN IF NOT EXISTS cluster_role_name    VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS serviceaccount_token TEXT;
