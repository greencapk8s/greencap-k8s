-- Renames the SETTINGS_CLUSTERS_*/SETTINGS_INFRASTRUCTURE_* permissions to GLOBAL_*,
-- following the move of "Clusters" and "Infrastructure" from the Settings section to the new Global section.

UPDATE user_permissions SET permissions = 'GLOBAL_CLUSTERS_VIEW' WHERE permissions = 'SETTINGS_CLUSTERS_VIEW';
UPDATE user_permissions SET permissions = 'GLOBAL_CLUSTERS_WRITE' WHERE permissions = 'SETTINGS_CLUSTERS_WRITE';
UPDATE user_permissions SET permissions = 'GLOBAL_INFRASTRUCTURE_VIEW' WHERE permissions = 'SETTINGS_INFRASTRUCTURE_VIEW';
UPDATE user_permissions SET permissions = 'GLOBAL_INFRASTRUCTURE_CORDON' WHERE permissions = 'SETTINGS_INFRASTRUCTURE_CORDON';
