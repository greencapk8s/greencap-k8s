ALTER TABLE clusters DROP CONSTRAINT clusters_provider_check;

UPDATE clusters SET provider = 'MinikubeDocker' WHERE provider = 'Kubernetes';

ALTER TABLE clusters ADD CONSTRAINT clusters_provider_check
    CHECK (provider IN ('MinikubeDocker', 'OpenShift'));
